package info.jab.churrera.cli.service;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.agent.AgentState;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.SequenceInfo;
import info.jab.churrera.workflow.WorkflowParseException;
import info.jab.churrera.workflow.BindResultTypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import info.jab.churrera.util.ConversationJsonDeserializer;
import info.jab.churrera.util.PropertyResolver;
import info.jab.churrera.util.PmlConverter;
import info.jab.cursor.CursorAgentManagementImpl;
import info.jab.cursor.CursorAgentInformationImpl;

/**
 * Background job processor that automatically processes unfinished jobs,
 * launching and tracking Cursor agents using traditional threading.
 */
public class JobProcessor {

    private static final Logger logger = LoggerFactory.getLogger(JobProcessor.class);

    private final JobRepository jobRepository;
    private final CLIAgent cliAgent;
    private final WorkflowParser workflowParser;
    private final int pollingIntervalSeconds;

    // Public constructor for dependency injection
    public JobProcessor(JobRepository jobRepository, CLIAgent cliAgent, WorkflowParser workflowParser, int pollingIntervalSeconds) {
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
        this.workflowParser = workflowParser;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    /**
     * Main processing loop that finds and processes unfinished jobs.
     * This method can be called periodically by a ScheduledExecutorService.
     */
    public void processJobs() {
        try {
            List<Job> unfinishedJobs = jobRepository.findUnfinishedJobs();

            if (unfinishedJobs.isEmpty()) {
                logger.debug("No unfinished jobs found");
                return; // No jobs to process
            }

            logger.info("Found {} unfinished job(s): {}", unfinishedJobs.size(),
                unfinishedJobs.stream().map(Job::jobId).toList());

            for (Job job : unfinishedJobs) {
                try {
                    logger.info("Processing job: {} (cursorAgentId: {}, status: {})",
                        job.jobId(), job.cursorAgentId(), job.status());
                    processJob(job);
                } catch (Exception e) {
                    logger.error("Error processing job {}: {}", job.jobId(), e.getMessage());
                    // Continue with other jobs
                }
            }
        } catch (Exception e) {
            logger.error("Error finding unfinished jobs: {}", e.getMessage());
        }
    }

    /**
     * Process a single job by launching agents and executing prompts.
     */
    private void processJob(Job job) {
        try {
            logger.info("Starting to process job: {} (current status: {})", job.jobId(), job.status());

            // Get job details with prompts
            var jobDetailsOpt = jobRepository.findJobWithDetails(job.jobId());
            if (jobDetailsOpt.isEmpty()) {
                logger.error("Job details not found for job: {}", job.jobId());
                return;
            }

            var jobDetails = jobDetailsOpt.get();
            List<Prompt> prompts = jobDetails.getPrompts();

            if (prompts.isEmpty()) {
                logger.error("No prompts found for job: {}", job.jobId());
                return;
            }

            logger.info("Found {} prompts for job: {} (status before workflow: {})", prompts.size(), job.jobId(), job.status());

            // Process the job workflow
            processJobWorkflow(job, prompts);

            logger.info("Finished processing job: {}", job.jobId());

        } catch (Exception e) {
            logger.error("Error processing job {}: {}", job.jobId(), e.getMessage(), e);
        }
    }

    /**
     * Process a job workflow by launching it if needed and executing prompts.
     */
    private void processJobWorkflow(Job job, List<Prompt> prompts) {
        try {
            logger.info("Starting to parse workflow for job: {} (status: {})", job.jobId(), job.status());

            // Parse workflow to get PML files
            WorkflowData workflowData = parseWorkflow(job.path());
            logger.info("Workflow parsed successfully for job: {}", job.jobId());

            // Check if this is a child job (from parallel workflow)
            if (job.parentJobId() != null) {
                logger.info("Detected child job: {} (parent: {}, status: {})", job.jobId(), job.parentJobId(), job.status());
                // Child jobs are processed as sequence workflows
                // They inherit the sequence from parent's parallel workflow
                // The workflow data is already parsed from parent's path, but we treat it as a sequence
                processChildJobWorkflow(job, workflowData, prompts);
                return;
            }

            // Check if this is a parallel workflow
            if (workflowData.isParallelWorkflow()) {
                logger.info("Detected parallel workflow for job: {} (status: {})", job.jobId(), job.status());
                processParallelWorkflow(job, workflowData);
                return;
            }

            // Standard sequence workflow processing
            // If job has no cursorAgentId, launch it with the first prompt
            boolean justLaunched = false;
            if (job.cursorAgentId() == null) {
                logger.info("Launching agent for job: {} (previous status: {})", job.jobId(), job.status());
                launchJobAgent(job, workflowData);

                // Fetch the updated job from database after launching
                job = jobRepository.findById(job.jobId()).orElse(job);
                logger.info("Job {} updated with cursorAgentId: {} (status: {})", job.jobId(), job.cursorAgentId(), job.status());
                justLaunched = true;
            }

            // Check current status and continue processing if needed
            // Don't monitor immediately after launching to allow parallel job processing
            if (job.cursorAgentId() != null && !justLaunched) {
                try {
                    logger.info("Getting agent status for job: {}", job.jobId());
                    AgentState currentStatus = cliAgent.getAgentStatus(job.cursorAgentId());

                    // Update job status in database
                    logger.info("Job {} status polled: {} -> updating database", job.jobId(), currentStatus);
                    cliAgent.updateJobStatusInDatabase(job, currentStatus);

                    logger.info("Job {} status updated to: {}", job.jobId(), currentStatus);

                    // Don't block on monitoring - just check status and process if completed
                    // This allows multiple jobs to be processed in parallel
                    if (currentStatus.isActive()) {
                        logger.info("Job {} is still active, will check again on next polling cycle", job.jobId());
                    } else if (currentStatus.isSuccessful()) {
                        // Agent completed successfully, process remaining prompts
                        logger.info("Job {} completed successfully, processing remaining prompts", job.jobId());
                        processRemainingPrompts(job, prompts, workflowData);
                    } else if (currentStatus.isTerminal()) {
                        logger.info("Job {} reached terminal state: {}", job.jobId(), currentStatus);
                    }
                } catch (Exception statusError) {
                    logger.error("Error getting agent status for job {}: {}", job.jobId(), statusError.getMessage());
                    // Mark job as failed if we can't get status
                    cliAgent.updateJobStatusInDatabase(job, AgentState.FAILED);
                }
            } else if (justLaunched) {
                logger.info("Job {} just launched, will check status on next polling cycle", job.jobId());
            }

        } catch (Exception e) {
            logger.error("Error processing job workflow {}: {}", job.jobId(), e.getMessage(), e);
        }
    }

    /**
     * Process a child job workflow extracted from parent's parallel workflow.
     * Child jobs use the sequence definition from the parent's parallel workflow.
     */
    private void processChildJobWorkflow(Job job, WorkflowData parentWorkflowData, List<Prompt> prompts) {
        try {
            logger.info("Processing child job workflow for: {}", job.jobId());

            // Extract sequence info from parent's parallel workflow
            if (!parentWorkflowData.isParallelWorkflow()) {
                logger.error("Parent workflow is not a parallel workflow for child job: {}", job.jobId());
                return;
            }

            ParallelWorkflowData parallelData = parentWorkflowData.getParallelWorkflowData();
            if (parallelData.getSequences().isEmpty()) {
                logger.error("No sequences found in parent parallel workflow for child job: {}", job.jobId());
                return;
            }

            // Get the sequence info (first sequence, as currently only one is supported)
            SequenceInfo sequenceInfo = parallelData.getSequences().get(0);

            // Create a WorkflowData for the child job using the sequence prompts
            List<PromptInfo> sequencePrompts = sequenceInfo.getPrompts();
            if (sequencePrompts.isEmpty()) {
                logger.error("No prompts found in sequence for child job: {}", job.jobId());
                return;
            }

            // First prompt is launch, rest are updates
            PromptInfo launchPrompt = sequencePrompts.get(0);
            List<PromptInfo> updatePrompts = sequencePrompts.size() > 1
                ? sequencePrompts.subList(1, sequencePrompts.size())
                : new ArrayList<>();

            WorkflowData childWorkflowData = new WorkflowData(
                launchPrompt,
                sequenceInfo.getModel(),
                sequenceInfo.getRepository(),
                updatePrompts
            );

            logger.info("Child job {} will use launch prompt: {} with {} update prompts",
                job.jobId(), launchPrompt.getSrcFile(), updatePrompts.size());

            // Process as standard sequence workflow
            boolean justLaunched = false;
            if (job.cursorAgentId() == null) {
                logger.info("Launching child job agent: {}", job.jobId());
                launchJobAgent(job, childWorkflowData);

                // Fetch the updated job from database after launching
                job = jobRepository.findById(job.jobId()).orElse(job);
                logger.info("Child job {} updated with cursorAgentId: {}", job.jobId(), job.cursorAgentId());
                justLaunched = true;
            }

            // Non-blocking poll and process prompts per cycle for child jobs
            if (job.cursorAgentId() != null && !justLaunched) {
                try {
                    AgentState currentStatus = cliAgent.getAgentStatus(job.cursorAgentId());
                    logger.info("Child job {} status polled: {} -> updating database", job.jobId(), currentStatus);
                    cliAgent.updateJobStatusInDatabase(job, currentStatus);

                    // Refresh job from database to get the updated status
                    job = jobRepository.findById(job.jobId()).orElse(job);

                    if (currentStatus.isActive()) {
                        logger.info("Child job {} is still active, will check again on next polling cycle", job.jobId());
                    } else if (currentStatus.isSuccessful()) {
                        logger.info("Child job {} completed successfully, processing remaining prompts", job.jobId());
                        processRemainingPrompts(job, prompts, childWorkflowData);
                    } else if (currentStatus.isTerminal()) {
                        logger.info("Child job {} reached terminal state: {}", job.jobId(), currentStatus);
                    }
                } catch (Exception statusError) {
                    logger.error("Error processing child job {}: {}", job.jobId(), statusError.getMessage());
                    cliAgent.updateJobStatusInDatabase(job, AgentState.FAILED);
                }
            } else if (justLaunched) {
                logger.info("Child job {} just launched, will check status on next polling cycle", job.jobId());
            }

        } catch (Exception e) {
            logger.error("Error processing child job workflow {}: {}", job.jobId(), e.getMessage(), e);
        }
    }

    /**
     * Process a parallel workflow by launching the parent job, waiting for results,
     * and creating child jobs for each element in the result.
     */
    private void processParallelWorkflow(Job job, WorkflowData workflowData) {
        try {
            logger.info("Processing parallel workflow for job: {}", job.jobId());

            ParallelWorkflowData parallelData = workflowData.getParallelWorkflowData();

            // Step 1: Launch the parent job if not already launched
            if (job.cursorAgentId() == null) {
                logger.info("Launching parent parallel job: {}", job.jobId());
                launchJobAgent(job, workflowData);

                // Fetch updated job
                job = jobRepository.findById(job.jobId()).orElse(job);
                logger.info("Parent job {} launched with agent: {}", job.jobId(), job.cursorAgentId());
            }

            // Step 2: Poll the parent job status on each cycle (non-blocking)
            // Only poll if the job is not already in a terminal state
            if (job.cursorAgentId() != null && !job.status().isTerminal()) {
                try {
                    AgentState currentStatus = cliAgent.getAgentStatus(job.cursorAgentId());
                    logger.info("Parent parallel job {} status polled: {} -> updating database", job.jobId(), currentStatus);
                    cliAgent.updateJobStatusInDatabase(job, currentStatus);

                    // Refresh job from database to get the updated status
                    job = jobRepository.findById(job.jobId()).orElse(job);

                    // Step 3: If successful, extract and process results
                    if (currentStatus.isSuccessful()) {
                        logger.info("Parent job {} completed successfully, proceeding to result extraction", job.jobId());
                    } else if (currentStatus.isActive()) {
                        logger.info("Parent parallel job {} is still active, will check again on next polling cycle", job.jobId());
                        return; // Defer extraction until completion
                    } else if (currentStatus.isTerminal()) {
                        logger.error("Parent job {} reached terminal state: {}", job.jobId(), currentStatus);
                        return;
                    }
                } catch (Exception statusError) {
                    logger.error("Error getting agent status for parent job {}: {}", job.jobId(), statusError.getMessage());
                    try {
                        cliAgent.updateJobStatusInDatabase(job, AgentState.FAILED);
                    } catch (Exception updateError) {
                        logger.error("Error updating job status to FAILED: {}", updateError.getMessage());
                    }
                    return;
                }

                // If we reach here, status was successful → extract results and create child jobs
                logger.info("Parent job {} completed successfully, attempting to extract results from conversation", job.jobId());

                // Get conversation to extract result
                String conversationContent = cliAgent.getConversationContent(job.cursorAgentId());
                logger.info("Conversation content length: {} characters", conversationContent.length());

                // Step 4: Deserialize results based on bindResultType
                String jsonResult = null;
                List<Object> deserializedList = null;

                if (parallelData.hasBindResultType()) {
                    String bindResultType = parallelData.getBindResultType();
                    logger.info("Attempting to deserialize result with type: {}", bindResultType);

                    if (BindResultTypeMapper.isListType(bindResultType)) {
                        // Deserialize to list
                        Class<?> elementType = BindResultTypeMapper.mapToElementType(bindResultType);

                        // Use raw type to handle generics, passing bindResultType as preferred key
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        Optional<List<Object>> resultList =
                            (Optional<List<Object>>) (Optional)
                            ConversationJsonDeserializer.deserializeList(conversationContent, (Class) elementType, bindResultType);

                        if (resultList.isPresent()) {
                            deserializedList = resultList.get();
                            logger.info("Successfully deserialized {} elements from conversation", deserializedList.size());
                            // Store as proper JSON array using Jackson ObjectMapper
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                jsonResult = mapper.writeValueAsString(deserializedList);
                                logger.info("Successfully serialized result as JSON: {}", jsonResult);
                            } catch (JsonProcessingException e) {
                                // Fallback to toString if JSON serialization fails
                                jsonResult = deserializedList.toString();
                                logger.error("Failed to serialize result as JSON, using toString: {}", e.getMessage());
                            }
                        } else {
                            logger.error("Failed to deserialize result from conversation for job: {}", job.jobId());
                            logger.error("Full conversation content for failed deserialization (length: {} chars): {}",
                                conversationContent.length(), conversationContent);
                        }
                    }
                }

                // Store extracted JSON result in job (not the full conversation)
                if (jsonResult != null) {
                    job = job.withResult(jsonResult);
                    jobRepository.save(job);
                    logger.info("Successfully stored result for parent job: {}", job.jobId());
                } else {
                    logger.error("No result to store for parent job: {}", job.jobId());
                    // If deserialization failed after a successful agent run, mark job as FAILED
                    try {
                        cliAgent.updateJobStatusInDatabase(job, AgentState.FAILED);
                        logger.error("Parent job {} marked as FAILED due to result deserialization issues", job.jobId());
                    } catch (Exception updateError) {
                        logger.error("Error updating job status to FAILED: {}", updateError.getMessage());
                    }
                }

                // Step 5: Create child jobs for each element
                if (deserializedList != null) {
                    createChildJobs(job, deserializedList, parallelData);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing parallel workflow for job {}: {}", job.jobId(), e.getMessage(), e);
            try {
                cliAgent.updateJobStatusInDatabase(job, AgentState.FAILED);
            } catch (Exception updateError) {
                logger.error("Error updating job status: {}", updateError.getMessage());
            }
        }
    }

    /**
     * Create child jobs for each element in the result list.
     */
    private void createChildJobs(Job parentJob, List<Object> resultList, ParallelWorkflowData parallelData) {
        try {
            logger.info("Creating {} child jobs for parent job: {}", resultList.size(), parentJob.jobId());

            // Get the first sequence (currently only one sequence is supported per parallel)
            if (parallelData.getSequences().isEmpty()) {
                logger.error("No sequences found in parallel workflow data");
                return;
            }

            SequenceInfo sequenceInfo = parallelData.getSequences().get(0);

            // For each element in the result list, create a child job
            for (int i = 0; i < resultList.size(); i++) {
                Object element = resultList.get(i);
                logger.info("Creating child job {} of {} with value: {}", i + 1, resultList.size(), element);

                try {
                    // Generate new job ID
                    String childJobId = UUID.randomUUID().toString();
                    LocalDateTime now = LocalDateTime.now();

                    // Store the bound value in the job's result field so it can be used during prompt processing
                    String boundValue = String.valueOf(element);

                    // Use parent workflow path - no need to create physical child workflow files
                    // Child jobs will be identified by parentJobId and will extract sequence info from parent workflow
                    String childWorkflowPath = parentJob.path();

                    // Create child job with parent reference and bound value
                    Job childJob = new Job(
                        childJobId,
                        childWorkflowPath, // Reuse parent workflow path
                        null, // cursorAgentId starts as null
                        sequenceInfo.getModel() != null ? sequenceInfo.getModel() : parentJob.model(),
                        sequenceInfo.getRepository() != null ? sequenceInfo.getRepository() : parentJob.repository(),
                        info.jab.churrera.agent.AgentState.UNKNOWN,
                        now,
                        now,
                        parentJob.jobId(), // Set parent job ID
                        boundValue, // Store the bound value from parent result
                        info.jab.churrera.workflow.WorkflowType.SEQUENCE // Child jobs are always SEQUENCE type
                    );

                    // Save child job
                    jobRepository.save(childJob);
                    logger.info("Created child job: {} for parent: {} with bound value: {}", childJobId, parentJob.jobId(), boundValue);

                    // Create prompts for child job
                    createChildJobPrompts(childJob, sequenceInfo);

                } catch (Exception e) {
                    logger.error("Error creating child job {} for parent {}: {}", i, parentJob.jobId(), e.getMessage(), e);
                }
            }

            logger.info("Successfully created {} child jobs for parent: {}", resultList.size(), parentJob.jobId());

        } catch (Exception e) {
            logger.error("Error creating child jobs for parent {}: {}", parentJob.jobId(), e.getMessage(), e);
        }
    }


    /**
     * Create prompt records in database for a child job.
     */
    private void createChildJobPrompts(Job childJob, SequenceInfo sequenceInfo) throws Exception {
        for (PromptInfo promptInfo : sequenceInfo.getPrompts()) {
            // Create prompt record with original filename
            String promptId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            Prompt prompt = new Prompt(
                promptId,
                childJob.jobId(),
                promptInfo.getSrcFile(), // Use original filename
                "UNKNOWN",
                now,
                now
            );

            jobRepository.savePrompt(prompt);
            logger.info("Created prompt {} for child job: {}", promptId, childJob.jobId());
        }
    }

    /**
     * Launch a new agent with the first prompt.
     */
    private void launchJobAgent(Job job, WorkflowData workflowData) {
        try {
            // Get launch prompt info
            PromptInfo launchPrompt = workflowData.getLaunchPrompt();

            // Read the launch prompt file
            String promptContent = readPromptFile(job.path(), launchPrompt.getSrcFile());

            // Get bind value from job result if present (for child jobs from parallel workflows)
            // Only apply if the prompt has bindResultExp attribute
            String bindValue = null;
            if (launchPrompt.hasBindResultExp() && job.result() != null) {
                bindValue = job.result();
                logger.info("Prompt has bindResultExp, will apply value: {}", bindValue);
            }

            // Determine PR flag: parallel workflow parent jobs should not create PR (only generate list),
            // sequence workflows (standard and child jobs) should create PR)
            boolean createPr = !workflowData.isParallelWorkflow();
            logger.info("Launching agent with PR flag: {} (workflow type: {})", createPr,
                workflowData.isParallelWorkflow() ? "parallel" : "sequence");

            // Launch the agent with type information, optional bind value, and PR flag
            String cursorAgentId = cliAgent.launchAgentForJob(job, promptContent, launchPrompt.getType(), bindValue, createPr);

            // Update job in database with cursorAgentId and CREATING status
            cliAgent.updateJobCursorIdInDatabase(job, cursorAgentId, AgentState.CREATING);

            logger.info("Launched job {} with Cursor ID: {} (type: {})", job.jobId(), cursorAgentId, launchPrompt.getType());

        } catch (Exception e) {
            logger.error("Error launching job {}: {}", job.jobId(), e.getMessage());
            // Mark job as failed if launch fails
            try {
                cliAgent.updateJobStatusInDatabase(job, AgentState.FAILED);
            } catch (Exception updateError) {
                logger.error("Error updating job status to FAILED: {}", updateError.getMessage());
            }
        }
    }

    /**
     * Monitor agent status and process prompts as they become available.
     */
    private void monitorAndProcessPrompts(Job job, List<Prompt> prompts, WorkflowData workflowData) {
        try {
            // Monitor agent until it reaches a terminal state
            AgentState finalStatus = cliAgent.monitorAgent(job.cursorAgentId(), pollingIntervalSeconds);

            // Update final status
            cliAgent.updateJobStatusInDatabase(job, finalStatus);
            cliAgent.updateJobInDatabase(job);

            logger.info("Job {} finished with status: {}", job.jobId(), finalStatus);

            // If successful, process remaining prompts
            if (finalStatus.isSuccessful()) {
                processRemainingPrompts(job, prompts, workflowData);
            }

        } catch (Exception e) {
            logger.error("Error monitoring job {}: {}", job.jobId(), e.getMessage());
        }
    }

    /**
     * Process remaining prompts that haven't been executed yet.
     */
    private void processRemainingPrompts(Job job, List<Prompt> prompts, WorkflowData workflowData) {
        try {
            List<PromptInfo> updatePrompts = workflowData.getUpdatePrompts();

            for (int i = 0; i < updatePrompts.size() && i < prompts.size(); i++) {
                Prompt prompt = prompts.get(i);
                PromptInfo promptInfo = updatePrompts.get(i);

                // Check if prompt has been processed (status not UNKNOWN)
                if ("UNKNOWN".equals(prompt.status())) {
                    processPrompt(job, prompt, promptInfo);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing remaining prompts for job {}: {}", job.jobId(), e.getMessage());
        }
    }

    /**
     * Process a single prompt by sending it as a follow-up to the agent.
     */
    private void processPrompt(Job job, Prompt prompt, PromptInfo promptInfo) {
        try {
            // Only send the prompt if it hasn't been sent yet
            if ("UNKNOWN".equals(prompt.status())) {
                // Read the prompt file
                String promptContent = readPromptFile(job.path(), promptInfo.getSrcFile());

                // Get bind value from job result if present (for child jobs from parallel workflows)
                // Only apply if the prompt has bindResultExp attribute
                String bindValue = null;
                if (promptInfo.hasBindResultExp() && job.result() != null) {
                    bindValue = job.result();
                    logger.info("Prompt has bindResultExp, will apply value: {}", bindValue);
                }

                // Send follow-up prompt with type information and optional bind value
                String followUpId = cliAgent.followUpForPrompt(job.cursorAgentId(), promptContent, promptInfo.getType(), bindValue);

                // Update prompt status to SENT
                cliAgent.updatePromptInDatabase(prompt, "SENT");
                cliAgent.updateJobInDatabase(job);

                logger.info("Sent follow-up prompt {} for job {} (type: {})", prompt.promptId(), job.jobId(), promptInfo.getType());

                // Don't block on monitoring - the job status will be checked on next polling cycle
                logger.info("Prompt {} sent, job {} status will be checked on next polling cycle", prompt.promptId(), job.jobId());
            } else if ("SENT".equals(prompt.status())) {
                // Prompt was sent, check if the job has completed
                AgentState currentStatus = cliAgent.getAgentStatus(job.cursorAgentId());

                if (currentStatus.isTerminal()) {
                    // Update prompt status based on job completion
                    cliAgent.updatePromptInDatabase(prompt, currentStatus.isSuccessful() ? "COMPLETED" : "FAILED");
                    cliAgent.updateJobInDatabase(job);
                    logger.info("Prompt {} finished with status: {}", prompt.promptId(), currentStatus);
                } else {
                    logger.info("Prompt {} still processing, job {} is in state: {}", prompt.promptId(), job.jobId(), currentStatus);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing prompt {}: {}", prompt.promptId(), e.getMessage());
            try {
                cliAgent.updatePromptInDatabase(prompt, "ERROR");
                cliAgent.updateJobInDatabase(job);
            } catch (Exception updateError) {
                logger.error("Error updating prompt status: {}", updateError.getMessage());
            }
        }
    }

    /**
     * Parse the workflow XML file to extract PML file information.
     */
    private WorkflowData parseWorkflow(String workflowPath) {
        try {
            logger.info("Parsing workflow file: {}", workflowPath);
            Path path = Paths.get(workflowPath);
            logger.info("Workflow file path resolved to: {}", path.toAbsolutePath());
            logger.info("Workflow file exists: {}", Files.exists(path));
            WorkflowData result = workflowParser.parse(path);
            logger.info("Workflow parsed successfully: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error parsing workflow file {}: {}", workflowPath, e.getMessage(), e);
            throw new RuntimeException("Failed to parse workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Read a prompt file from the same directory as the workflow file.
     */
    private String readPromptFile(String workflowPath, String promptFileName) {
        try {
            Path workflowDir = Paths.get(workflowPath).getParent();
            Path promptPath = workflowDir.resolve(promptFileName);

            if (!Files.exists(promptPath)) {
                throw new RuntimeException("Prompt file not found: " + promptPath);
            }

            return Files.readString(promptPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read prompt file: " + e.getMessage(), e);
        }
    }
}

