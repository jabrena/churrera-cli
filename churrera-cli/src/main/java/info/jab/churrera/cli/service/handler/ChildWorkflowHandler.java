package info.jab.churrera.cli.service.handler;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.service.*;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.SequenceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for processing child job workflows from parallel executions.
 */
public class ChildWorkflowHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChildWorkflowHandler.class);

    private final JobRepository jobRepository;
    private final CLIAgent cliAgent;
    private final AgentLauncher agentLauncher;
    private final PromptProcessor promptProcessor;
    private final TimeoutManager timeoutManager;
    private final FallbackExecutor fallbackExecutor;

    @Inject
    public ChildWorkflowHandler(JobRepository jobRepository, CLIAgent cliAgent,
                               AgentLauncher agentLauncher, PromptProcessor promptProcessor,
                               TimeoutManager timeoutManager, FallbackExecutor fallbackExecutor) {
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
        this.agentLauncher = agentLauncher;
        this.promptProcessor = promptProcessor;
        this.timeoutManager = timeoutManager;
        this.fallbackExecutor = fallbackExecutor;
    }

    /**
     * Process a child job workflow extracted from parent's parallel workflow.
     * Child jobs use the sequence definition from the parent's parallel workflow.
     *
     * @param job the child job to process
     * @param parentWorkflowData the parent workflow data
     * @param prompts the list of prompts
     */
    public void processWorkflow(Job job, WorkflowData parentWorkflowData, List<Prompt> prompts) {
        try {
            logger.info("Processing child job workflow for: {}", job.jobId());

            WorkflowData childWorkflowData = createChildWorkflowData(job, parentWorkflowData);
            if (childWorkflowData == null) {
                return;
            }

            Long timeoutMillis = getTimeoutMillis(job, parentWorkflowData);
            boolean justLaunched = launchJobIfNeeded(job, childWorkflowData);
            if (justLaunched) {
                job = jobRepository.findById(job.jobId()).orElse(job);
            }

            if (job.cursorAgentId() != null && !justLaunched) {
                job = handleTimeoutAndFallback(job, parentWorkflowData, childWorkflowData, timeoutMillis);
                checkAndUpdateChildJobStatus(job, prompts, childWorkflowData);
            } else if (justLaunched) {
                logger.info("Child job {} just launched, will check status on next polling cycle", job.jobId());
            }

        } catch (Exception e) {
            logger.error("Error processing child job workflow {}: {}", job.jobId(), e.getMessage(), e);
        }
    }

    private WorkflowData createChildWorkflowData(Job job, WorkflowData parentWorkflowData) {
        if (!parentWorkflowData.isParallelWorkflow()) {
            logger.error("Parent workflow is not a parallel workflow for child job: {}", job.jobId());
            return null;
        }

        ParallelWorkflowData parallelData = parentWorkflowData.getParallelWorkflowData();
        if (parallelData.getSequences().isEmpty()) {
            logger.error("No sequences found in parent parallel workflow for child job: {}", job.jobId());
            return null;
        }

        SequenceInfo sequenceInfo = parallelData.getSequences().get(0);
        List<info.jab.churrera.workflow.PromptInfo> sequencePrompts = sequenceInfo.getPrompts();
        if (sequencePrompts.isEmpty()) {
            logger.error("No prompts found in sequence for child job: {}", job.jobId());
            return null;
        }

        info.jab.churrera.workflow.PromptInfo launchPrompt = sequencePrompts.get(0);
        List<info.jab.churrera.workflow.PromptInfo> updatePrompts = sequencePrompts.size() > 1
            ? sequencePrompts.subList(1, sequencePrompts.size())
            : new ArrayList<>();

        WorkflowData childWorkflowData = new WorkflowData(
            launchPrompt,
            sequenceInfo.getModel(),
            sequenceInfo.getRepository(),
            updatePrompts,
            null, null, null
        );

        logger.info("Child job {} will use launch prompt: {} with {} update prompts",
            job.jobId(), launchPrompt.getSrcFile(), updatePrompts.size());
        return childWorkflowData;
    }

    private Long getTimeoutMillis(Job job, WorkflowData parentWorkflowData) {
        Long timeoutMillis = job.timeoutMillis();
        if (timeoutMillis == null && parentWorkflowData.isParallelWorkflow()) {
            timeoutMillis = parentWorkflowData.getParallelWorkflowData().getTimeoutMillis();
        }
        return timeoutMillis;
    }

    private boolean launchJobIfNeeded(Job job, WorkflowData childWorkflowData) {
        if (job.cursorAgentId() == null) {
            logger.info("Launching child job agent: {}", job.jobId());
            agentLauncher.launchJobAgent(job, childWorkflowData);
            return true;
        }
        return false;
    }

    private Job handleTimeoutAndFallback(Job job, WorkflowData parentWorkflowData, WorkflowData childWorkflowData, Long timeoutMillis) {
        if (timeoutMillis == null || job.workflowStartTime() == null) {
            return job;
        }

        long elapsedMillis = timeoutManager.getElapsedMillis(job);
        logger.debug("Child job {} timeout check: elapsed={}ms, limit={}ms, workflowStartTime={}",
            job.jobId(), elapsedMillis, timeoutMillis, job.workflowStartTime());

        if (elapsedMillis >= timeoutMillis) {
            return executeFallbackIfNeeded(job, parentWorkflowData, childWorkflowData, elapsedMillis, timeoutMillis);
        }
        return job;
    }

    private Job executeFallbackIfNeeded(Job job, WorkflowData parentWorkflowData, WorkflowData childWorkflowData, 
                                       long elapsedMillis, long timeoutMillis) {
        if (job.status().isTerminal()) {
            logger.info("Child job {} has reached timeout but is already in terminal state ({}), skipping fallback.",
                job.jobId(), job.status());
            return job;
        }

        if (job.fallbackExecuted() != null && job.fallbackExecuted()) {
            logger.debug("Child job {} has reached timeout but fallback already executed, skipping.", job.jobId());
            return job;
        }

        logger.warn("Child job {} has reached timeout ({}ms elapsed, {}ms limit). Executing fallback.",
            job.jobId(), elapsedMillis, timeoutMillis);

        String fallbackSrc = job.fallbackSrc();
        if (fallbackSrc == null && parentWorkflowData.isParallelWorkflow()) {
            fallbackSrc = parentWorkflowData.getParallelWorkflowData().getFallbackSrc();
        }

        if (fallbackSrc != null) {
            fallbackExecutor.executeFallback(job.withFallbackSrc(fallbackSrc), childWorkflowData, elapsedMillis, timeoutMillis);
            Job updatedJob = jobRepository.findById(job.jobId()).orElse(job);
            logger.info("Child job {} updated after fallback execution, cursorAgentId: {}, status: {}",
                updatedJob.jobId(), updatedJob.cursorAgentId(), updatedJob.status());
            return updatedJob;
        }
        return job;
    }

    /**
     * Checks and updates the status of a child job, ensuring database synchronization.
     *
     * @param job the child job to check
     * @param prompts the list of prompts
     * @param childWorkflowData the child workflow data
     * @return the updated job
     */
    private Job checkAndUpdateChildJobStatus(Job job, List<Prompt> prompts, WorkflowData childWorkflowData) {
        try {
            AgentState currentStatus = cliAgent.getAgentStatus(job.cursorAgentId());
            logger.info("Child job {} status polled: {} -> updating database (current DB status: {})",
                job.jobId(), currentStatus, job.status());
            cliAgent.updateJobStatusInDatabase(job, currentStatus);

            // Refresh job from database to get the updated status
            Job updatedJob = jobRepository.findById(job.jobId()).orElse(job);

            if (currentStatus.isActive()) {
                logger.info("Child job {} is still active, will check again on next polling cycle", updatedJob.jobId());
            } else if (currentStatus.isSuccessful()) {
                logger.info("Child job {} completed successfully, processing remaining prompts", updatedJob.jobId());
                promptProcessor.processRemainingPrompts(updatedJob, prompts, childWorkflowData);
            } else if (currentStatus.isTerminal()) {
                logger.info("Child job {} reached terminal state: {}", updatedJob.jobId(), currentStatus);
            }
            return updatedJob;
        } catch (Exception statusError) {
            logger.error("Error processing child job {}: {}", job.jobId(), statusError.getMessage());
            cliAgent.updateJobStatusInDatabase(job, AgentState.error());
            return job;
        }
    }
}

