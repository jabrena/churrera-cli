package info.jab.churrera.cli.command;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.JobWithDetails;
import info.jab.churrera.workflow.BindResultTypeMapper;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.util.ConversationJsonDeserializer;
import info.jab.churrera.agent.AgentState;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Command to show the status of a specific job.
 */
@CommandLine.Command(name = "jobs/{id}/status", description = "Show status for a specific job")
public class JobStatusCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(JobStatusCommand.class);

    private static final int JOB_ID_PREFIX_LENGTH = 8;

    private final JobRepository jobRepository;
    private final String jobId;
    private final CLIAgent cliAgent;

    public JobStatusCommand(JobRepository jobRepository, CLIAgent cliAgent, String jobId) {
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
        this.jobId = jobId;
    }

    @Override
    public void run() {
        try {
            String resolvedJobId = resolveJobId(jobId);
            if (resolvedJobId == null) {
                return;
            }

            Optional<JobWithDetails> jobDetailsOpt = jobRepository.findJobWithDetails(resolvedJobId);

            if (jobDetailsOpt.isEmpty()) {
                System.out.println("Job not found: " + resolvedJobId);
            } else {
                JobWithDetails jobDetails = jobDetailsOpt.get();
                Job job = jobDetails.getJob();
                List<Prompt> prompts = jobDetails.getPrompts();

                System.out.println("Job Details:");
                System.out.println("  Job ID: " + job.jobId());
                System.out.println("  Path: " + job.path());
                System.out.println("  Cursor Agent ID: "
                        + (job.cursorAgentId() != null ? job.cursorAgentId() : "Not launched yet"));
                System.out.println("  Model: " + job.model());
                System.out.println("  Repository: " + job.repository());
                System.out.println("  Status: " + job.status());
                System.out.println("  Created: " + job.createdAt());
                System.out.println("  Last Update: " + job.lastUpdate());

                // Display workflow type
                String typeDisplay = "Unknown";
                if (job.type() != null) {
                    typeDisplay = job.type().toString();
                } else {
                    // Parse workflow file to determine type for legacy jobs
                    try {
                        WorkflowType parsedType = WorkflowParser.determineWorkflowType(new File(job.path()));
                        typeDisplay = parsedType != null ? parsedType.toString() : "Unknown";
                    } catch (Exception e) {
                        typeDisplay = "Unknown";
                    }
                }
                System.out.println("  Type: " + typeDisplay);

                // Display parent job ID
                System.out.println("  Parent Job ID: " + (job.parentJobId() != null ? job.parentJobId() : "None"));

                // Display bindResultType and result for parallel workflows
                if (job.type() == WorkflowType.PARALLEL || "PARALLEL".equals(typeDisplay)) {
                    try {
                        WorkflowParser parser = new WorkflowParser();
                        WorkflowData workflowData = parser.parse(new File(job.path()));
                        if (workflowData.isParallelWorkflow()) {
                            ParallelWorkflowData parallelData = workflowData.getParallelWorkflowData();
                            if (parallelData.hasBindResultType()) {
                                System.out.println("  Bind Result Type: " + parallelData.getBindResultType());

                                String resultToDisplay = job.result();

                                // If result is empty but job is finished and has cursorAgentId, extract and
                                // store result
                                if ((resultToDisplay == null || resultToDisplay.isEmpty()) &&
                                        job.status().isSuccessful() &&
                                        job.cursorAgentId() != null) {

                                    logger.info("Attempting to extract result from conversation for job: {}",
                                            job.jobId());
                                    try {
                                        String conversationContent = cliAgent
                                                .getConversationContent(job.cursorAgentId());
                                        String bindResultType = parallelData.getBindResultType();

                                        if (BindResultTypeMapper.isListType(bindResultType)) {
                                            Class<?> elementType = BindResultTypeMapper
                                                    .mapToElementType(bindResultType);
                                            @SuppressWarnings("unchecked")
                                            Optional<List<Object>> resultList = (Optional<List<Object>>) (Optional<?>) ConversationJsonDeserializer
                                                    .deserializeList(conversationContent, elementType, bindResultType);

                                            if (resultList.isPresent()) {
                                                // Store as proper JSON array using Jackson ObjectMapper
                                                try {
                                                    ObjectMapper mapper = new ObjectMapper();
                                                    resultToDisplay = mapper.writeValueAsString(resultList.get());
                                                    logger.info("Successfully serialized result as JSON: {}",
                                                            resultToDisplay);
                                                } catch (JsonProcessingException e) {
                                                    // Fallback to toString if JSON serialization fails
                                                    resultToDisplay = resultList.get().toString();
                                                    logger.warn(
                                                            "Failed to serialize result as JSON, using toString: {}",
                                                            e.getMessage());
                                                }
                                                // Store the result for future use
                                                Job updatedJob = job.withResult(resultToDisplay);
                                                jobRepository.save(updatedJob);
                                                logger.info("Successfully extracted and stored result for job: {}",
                                                        job.jobId());
                                            } else {
                                                logger.error(
                                                        "Failed to deserialize result from conversation for job: {}",
                                                        job.jobId());
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error("Error extracting result from conversation: {}", e.getMessage(),
                                                e);
                                    }
                                }

                                // Display result if available
                                if (resultToDisplay != null && !resultToDisplay.isEmpty()) {
                                    System.out.println("  Result: " + resultToDisplay);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Could not parse workflow file: {}", e.getMessage());
                    }
                }

                System.out.println();

                if (prompts.isEmpty()) {
                    System.out.println("  No prompts found for this job.");
                } else {
                    System.out.println("  Prompts:");
                    for (int i = 0; i < prompts.size(); i++) {
                        Prompt prompt = prompts.get(i);
                        System.out.printf("    %d. Prompt ID: %s%n", i + 1, prompt.promptId());
                        System.out.printf("       Job ID: %s%n", prompt.jobId());
                        System.out.printf("       PML File: %s%n", prompt.pmlFile());
                        System.out.printf("       Status: %s%n", prompt.status());
                        System.out.printf("       Created: %s%n", prompt.createdAt());
                        System.out.printf("       Last Update: %s%n", prompt.lastUpdate());
                        System.out.println();
                    }
                }
            }
        } catch (BaseXException | QueryException e) {
            logger.error("Error retrieving job status: {}", e.getMessage());
            System.err.println("Error retrieving job status: " + e.getMessage());
        }
    }

    private String resolveJobId(String provided) throws BaseXException, QueryException {
        var exact = jobRepository.findById(provided);
        if (exact.isPresent()) {
            return provided;
        }

        if (provided != null && provided.length() == JOB_ID_PREFIX_LENGTH) {
            List<Job> all = jobRepository.findAll();
            List<Job> matches = new ArrayList<>();
            for (Job j : all) {
                if (j.jobId() != null && j.jobId().startsWith(provided)) {
                    matches.add(j);
                }
            }

            if (matches.isEmpty()) {
                System.out.println("No job found starting with: " + provided);
                return null;
            }
            if (matches.size() > 1) {
                System.out.println("Ambiguous job prefix '" + provided + "' matches multiple jobs:");
                for (Job m : matches) {
                    System.out.println("  - " + m.jobId());
                }
                System.out.println("Please specify a full UUID or a unique 8-char prefix.");
                return null;
            }

            return matches.get(0).jobId();
        }

        System.out.println("Job not found: " + provided);
        return null;
    }
}
