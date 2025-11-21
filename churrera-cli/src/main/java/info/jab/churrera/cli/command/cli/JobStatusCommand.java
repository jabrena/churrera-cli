package info.jab.churrera.cli.command.cli;

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
import info.jab.churrera.cli.model.AgentState;
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
                displayJobStatus(jobDetailsOpt.get());
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
            return resolveByPrefix(provided);
        }

        System.out.println("Job not found: " + provided);
        return null;
    }

    private String resolveByPrefix(String provided) throws BaseXException, QueryException {
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
            handleAmbiguousPrefix(provided, matches);
            return null;
        }

        return matches.get(0).jobId();
    }

    private void handleAmbiguousPrefix(String provided, List<Job> matches) {
        System.out.println("Ambiguous job prefix '" + provided + "' matches multiple jobs:");
        for (Job m : matches) {
            System.out.println("  - " + m.jobId());
        }
        System.out.println("Please specify a full UUID or a unique 8-char prefix.");
    }

    private void displayJobStatus(JobWithDetails jobDetails) {
        Job job = jobDetails.getJob();
        List<Prompt> prompts = jobDetails.getPrompts();

        displayBasicJobInfo(job);
        displayParallelWorkflowInfo(job);
        displayPrompts(prompts);
    }

    private void displayBasicJobInfo(Job job) {
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

        String typeDisplay = determineTypeDisplay(job);
        System.out.println("  Type: " + typeDisplay);
        System.out.println("  Parent Job ID: " + (job.parentJobId() != null ? job.parentJobId() : "None"));
    }

    private String determineTypeDisplay(Job job) {
        if (job.type() != null) {
            return job.type().toString();
        }
        try {
            WorkflowType parsedType = WorkflowParser.determineWorkflowType(new File(job.path()));
            return parsedType != null ? parsedType.toString() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void displayParallelWorkflowInfo(Job job) {
        if (job.type() != WorkflowType.PARALLEL) {
            return;
        }

        try {
            WorkflowParser parser = new WorkflowParser();
            WorkflowData workflowData = parser.parse(new File(job.path()));
            if (workflowData.isParallelWorkflow()) {
                ParallelWorkflowData parallelData = workflowData.getParallelWorkflowData();
                if (parallelData.hasBindResultType()) {
                    displayBindResultType(parallelData, job);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not parse workflow file: {}", e.getMessage());
        }
    }

    private void displayBindResultType(ParallelWorkflowData parallelData, Job job) {
        System.out.println("  Bind Result Type: " + parallelData.getBindResultType());
        String resultToDisplay = extractAndStoreResult(parallelData, job);
        if (resultToDisplay != null && !resultToDisplay.isEmpty()) {
            System.out.println("  Result: " + resultToDisplay);
        }
    }

    private String extractAndStoreResult(ParallelWorkflowData parallelData, Job job) {
        String resultToDisplay = job.result();
        if (shouldExtractResult(resultToDisplay, job)) {
            resultToDisplay = extractResultFromConversation(parallelData, job);
        }
        return resultToDisplay;
    }

    private boolean shouldExtractResult(String resultToDisplay, Job job) {
        return (resultToDisplay == null || resultToDisplay.isEmpty())
                && job.status().isSuccessful()
                && job.cursorAgentId() != null;
    }

    private String extractResultFromConversation(ParallelWorkflowData parallelData, Job job) {
        logger.info("Attempting to extract result from conversation for job: {}", job.jobId());
        try {
            String conversationContent = cliAgent.getConversationContent(job.cursorAgentId());
            String bindResultType = parallelData.getBindResultType();

            if (BindResultTypeMapper.isListType(bindResultType)) {
                return extractListResult(conversationContent, bindResultType, job);
            }
        } catch (Exception e) {
            logger.error("Error extracting result from conversation: {}", e.getMessage(), e);
        }
        return job.result();
    }

    private String extractListResult(String conversationContent, String bindResultType, Job job) {
        Class<?> elementType = BindResultTypeMapper.mapToElementType(bindResultType);
        @SuppressWarnings("unchecked")
        Optional<List<Object>> resultList = (Optional<List<Object>>) (Optional<?>) ConversationJsonDeserializer
                .deserializeList(conversationContent, elementType, bindResultType);

        if (resultList.isPresent()) {
            String resultToDisplay = serializeResult(resultList.get());
            storeResult(job, resultToDisplay);
            return resultToDisplay;
        } else {
            logger.error("Failed to deserialize result from conversation for job: {}", job.jobId());
        }
        return job.result();
    }

    private String serializeResult(List<Object> resultList) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String result = mapper.writeValueAsString(resultList);
            logger.info("Successfully serialized result as JSON: {}", result);
            return result;
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize result as JSON, using toString: {}", e.getMessage());
            return resultList.toString();
        }
    }

    private void storeResult(Job job, String resultToDisplay) {
        try {
            Job updatedJob = job.withResult(resultToDisplay);
            jobRepository.save(updatedJob);
            logger.info("Successfully extracted and stored result for job: {}", job.jobId());
        } catch (BaseXException | QueryException e) {
            logger.error("Error storing result for job {}: {}", job.jobId(), e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error storing result for job {}: {}", job.jobId(), e.getMessage(), e);
        }
    }

    private void displayPrompts(List<Prompt> prompts) {
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
}
