package info.jab.churrera.cli.service.handler;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.service.*;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.SequenceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handler for processing parallel workflows.
 */
public class ParallelWorkflowHandler {

    private static final Logger logger = LoggerFactory.getLogger(ParallelWorkflowHandler.class);

    private final JobRepository jobRepository;
    private final CLIAgent cliAgent;
    private final AgentLauncher agentLauncher;
    private final TimeoutManager timeoutManager;
    private final FallbackExecutor fallbackExecutor;
    private final ResultExtractor resultExtractor;

    public ParallelWorkflowHandler(JobRepository jobRepository, CLIAgent cliAgent,
                                  AgentLauncher agentLauncher, TimeoutManager timeoutManager,
                                  FallbackExecutor fallbackExecutor, ResultExtractor resultExtractor) {
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
        this.agentLauncher = agentLauncher;
        this.timeoutManager = timeoutManager;
        this.fallbackExecutor = fallbackExecutor;
        this.resultExtractor = resultExtractor;
    }

    /**
     * Process a parallel workflow by launching the parent job, waiting for results,
     * and creating child jobs for each element in the result.
     *
     * @param job the parent job
     * @param workflowData the workflow data
     */
    public void processWorkflow(Job job, WorkflowData workflowData) {
        try {
            logger.info("Processing parallel workflow for job: {}", job.jobId());

            ParallelWorkflowData parallelData = workflowData.getParallelWorkflowData();

            // Step 1: Launch the parent job if not already launched
            if (job.cursorAgentId() == null) {
                logger.info("Launching parent parallel job: {}", job.jobId());
                agentLauncher.launchJobAgent(job, workflowData);

                // Fetch updated job
                job = jobRepository.findById(job.jobId()).orElse(job);
                logger.info("Parent job {} launched with agent: {}", job.jobId(), job.cursorAgentId());
            }

            // Check timeout for parallel workflow (only if timeout is set)
            Long timeoutMillis = parallelData.getTimeoutMillis();
            if (timeoutMillis != null && job.workflowStartTime() != null) {
                long elapsedMillis = timeoutManager.getElapsedMillis(job);
                if (elapsedMillis >= timeoutMillis) {
                    // Only execute fallback if it hasn't been executed yet
                    // Note: For parallel workflows, we check child statuses in executeFallbackForParallelChildren
                    if (job.fallbackExecuted() == null || !job.fallbackExecuted()) {
                        logger.warn("Parallel workflow {} has reached timeout ({}ms elapsed, {}ms limit). Executing fallback for all unfinished children.",
                            job.jobId(), elapsedMillis, timeoutMillis);
                        fallbackExecutor.executeFallbackForParallelChildren(job, parallelData);
                    } else {
                        logger.debug("Parallel workflow {} has reached timeout but fallback already executed, skipping.", job.jobId());
                    }
                    return;
                }
            }

            // Step 2: Poll the parent job status on each cycle (non-blocking)
            // Only poll if the job is not already in a terminal state
            if (job.cursorAgentId() != null && !job.status().isTerminal()) {
                // Check timeout again before processing
                if (timeoutMillis != null && job.workflowStartTime() != null) {
                    long elapsedMillis = timeoutManager.getElapsedMillis(job);
                    if (elapsedMillis >= timeoutMillis) {
                        // Only execute fallback if it hasn't been executed yet
                        // Note: For parallel workflows, we check child statuses in executeFallbackForParallelChildren
                        if (job.fallbackExecuted() == null || !job.fallbackExecuted()) {
                            logger.warn("Parallel workflow {} has reached timeout ({}ms elapsed, {}ms limit). Executing fallback for all unfinished children.",
                                job.jobId(), elapsedMillis, timeoutMillis);
                            fallbackExecutor.executeFallbackForParallelChildren(job, parallelData);
                        } else {
                            logger.debug("Parallel workflow {} has reached timeout but fallback already executed, skipping.", job.jobId());
                        }
                        return;
                    }
                }

                JobStatusCheckResult result = checkParentJobStatus(job);
                if (!result.shouldContinue()) {
                    return;
                }
                job = result.getUpdatedJob();

                // If we reach here, status was successful → extract results and create child jobs
                logger.info("Parent job {} completed successfully, attempting to extract results from conversation", job.jobId());

                // Step 4: Extract and deserialize results
                List<Object> deserializedList = resultExtractor.extractResults(job, parallelData);

                if (deserializedList == null) {
                    logger.error("No result to store for parent job: {}", job.jobId());
                    // If deserialization failed after a successful agent run, mark job as FAILED
                    updateJobStatusToError(job, "Parent job " + job.jobId() + " marked as FAILED due to result deserialization issues");
                    return;
                }

                // Step 5: Create child jobs for each element
                createChildJobs(job, deserializedList, parallelData);
            }

        } catch (Exception e) {
            logger.error("Error processing parallel workflow for job {}: {}", job.jobId(), e.getMessage(), e);
            updateJobStatusToError(job, "Error updating job status");
        }
    }

    /**
     * Create child jobs for each element in the result list.
     *
     * @param parentJob the parent job
     * @param resultList the list of results to create child jobs for
     * @param parallelData the parallel workflow data
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

                createSingleChildJob(parentJob, element, i, sequenceInfo, parallelData);
            }

            logger.info("Successfully created {} child jobs for parent: {}", resultList.size(), parentJob.jobId());

        } catch (Exception e) {
            logger.error("Error creating child jobs for parent {}: {}", parentJob.jobId(), e.getMessage(), e);
        }
    }

    /**
     * Create prompt records in database for a child job.
     *
     * @param childJob the child job
     * @param sequenceInfo the sequence info containing prompts
     */
    private void createChildJobPrompts(Job childJob, SequenceInfo sequenceInfo) throws Exception {
        for (info.jab.churrera.workflow.PromptInfo promptInfo : sequenceInfo.getPrompts()) {
            // Create prompt record with original filename
            String promptId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            info.jab.churrera.cli.model.Prompt prompt = new info.jab.churrera.cli.model.Prompt(
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
     * Result of checking parent job status.
     */
    private static class JobStatusCheckResult {
        private final boolean shouldContinue;
        private final Job updatedJob;

        JobStatusCheckResult(boolean shouldContinue, Job updatedJob) {
            this.shouldContinue = shouldContinue;
            this.updatedJob = updatedJob;
        }

        boolean shouldContinue() {
            return shouldContinue;
        }

        Job getUpdatedJob() {
            return updatedJob;
        }
    }

    /**
     * Checks and updates the parent job status.
     *
     * @param job the parent job
     * @return result indicating whether to continue and the updated job
     */
    private JobStatusCheckResult checkParentJobStatus(Job job) {
        try {
            AgentState currentStatus = cliAgent.getAgentStatus(job.cursorAgentId());
            logger.info("Parent parallel job {} status polled: {} -> updating database", job.jobId(), currentStatus);
            cliAgent.updateJobStatusInDatabase(job, currentStatus);

            // Refresh job from database to get the updated status
            Job updatedJob = jobRepository.findById(job.jobId()).orElse(job);

            // Step 3: If successful, extract and process results
            if (currentStatus.isSuccessful()) {
                logger.info("Parent job {} completed successfully, proceeding to result extraction", updatedJob.jobId());
                return new JobStatusCheckResult(true, updatedJob);
            } else if (currentStatus.isActive()) {
                logger.info("Parent parallel job {} is still active, will check again on next polling cycle", updatedJob.jobId());
                return new JobStatusCheckResult(false, updatedJob); // Defer extraction until completion
            } else if (currentStatus.isTerminal()) {
                logger.error("Parent job {} reached terminal state: {}", updatedJob.jobId(), currentStatus);
                return new JobStatusCheckResult(false, updatedJob);
            }
            return new JobStatusCheckResult(false, updatedJob);
        } catch (Exception statusError) {
            logger.error("Error getting agent status for parent job {}: {}", job.jobId(), statusError.getMessage());
            updateJobStatusToError(job, "Error updating job status to FAILED");
            return new JobStatusCheckResult(false, job);
        }
    }

    /**
     * Updates job status to ERROR, handling any exceptions.
     *
     * @param job the job to update
     * @param errorMessage the error message to log
     */
    private void updateJobStatusToError(Job job, String errorMessage) {
        try {
            cliAgent.updateJobStatusInDatabase(job, AgentState.error());
            if (errorMessage != null && !errorMessage.isEmpty()) {
                logger.error(errorMessage);
            }
        } catch (Exception updateError) {
            logger.error("Error updating job status to FAILED: {}", updateError.getMessage());
        }
    }

    /**
     * Creates a single child job for an element in the result list.
     *
     * @param parentJob the parent job
     * @param element the element to create a child job for
     * @param index the index of the element
     * @param sequenceInfo the sequence info
     * @param parallelData the parallel workflow data
     */
    private void createSingleChildJob(Job parentJob, Object element, int index, SequenceInfo sequenceInfo, ParallelWorkflowData parallelData) {
        try {
            // Generate new job ID
            String childJobId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            // Store the bound value in the job's result field so it can be used during prompt processing
            String boundValue = String.valueOf(element);

            // Use parent workflow path - no need to create physical child workflow files
            // Child jobs will be identified by parentJobId and will extract sequence info from parent workflow
            String childWorkflowPath = parentJob.path();

            // Inherit timeout and fallback from parent parallel workflow
            Long childTimeoutMillis = sequenceInfo.getTimeoutMillis();
            if (childTimeoutMillis == null) {
                childTimeoutMillis = parallelData.getTimeoutMillis();
            }
            String childFallbackSrc = sequenceInfo.getFallbackSrc();
            if (childFallbackSrc == null) {
                childFallbackSrc = parallelData.getFallbackSrc();
            }

            // Create child job with parent reference and bound value
            Job childJob = new Job(
                childJobId,
                childWorkflowPath, // Reuse parent workflow path
                null, // cursorAgentId starts as null
                sequenceInfo.getModel() != null ? sequenceInfo.getModel() : parentJob.model(),
                sequenceInfo.getRepository() != null ? sequenceInfo.getRepository() : parentJob.repository(),
                AgentState.creating(),
                now,
                now,
                parentJob.jobId(), // Set parent job ID
                boundValue, // Store the bound value from parent result
                info.jab.churrera.workflow.WorkflowType.SEQUENCE, // Child jobs are always SEQUENCE type
                childTimeoutMillis, // Inherit timeout from parent or sequence
                null, // workflowStartTime is null initially, set when launched if timeout is set
                childFallbackSrc, // Inherit fallback from parent or sequence
                null // fallbackExecuted is null initially (false when not executed)
            );

            // Save child job
            jobRepository.save(childJob);
            logger.info("Created child job: {} for parent: {} with bound value: {}", childJobId, parentJob.jobId(), boundValue);

            // Create prompts for child job
            createChildJobPrompts(childJob, sequenceInfo);

        } catch (Exception e) {
            logger.error("Error creating child job {} for parent {}: {}", index, parentJob.jobId(), e.getMessage(), e);
        }
    }
}

