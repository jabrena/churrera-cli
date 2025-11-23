package info.jab.churrera.cli.service.handler;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.service.*;
import info.jab.churrera.workflow.WorkflowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * Handler for processing sequence workflows.
 */
public class SequenceWorkflowHandler {

    private static final Logger logger = LoggerFactory.getLogger(SequenceWorkflowHandler.class);

    private final JobRepository jobRepository;
    private final CLIAgent cliAgent;
    private final AgentLauncher agentLauncher;
    private final PromptProcessor promptProcessor;
    private final TimeoutManager timeoutManager;
    private final FallbackExecutor fallbackExecutor;

    @Inject
    public SequenceWorkflowHandler(JobRepository jobRepository, CLIAgent cliAgent,
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
     * Process a sequence workflow by launching it if needed and executing prompts.
     *
     * @param job the job to process
     * @param prompts the list of prompts
     * @param workflowData the workflow data
     */
    public void processWorkflow(Job job, List<Prompt> prompts, WorkflowData workflowData) {
        try {
            logger.trace("Starting to parse workflow for job: {} (status: {})", job.jobId(), job.status());

            // If job has no cursorAgentId, launch it with the first prompt
            boolean justLaunched = false;
            if (job.cursorAgentId() == null) {
                // Reset workflowStartTime before launching to ensure we start fresh
                job = timeoutManager.resetWorkflowStartTimeIfNeeded(job);

                logger.debug("Launching agent for job: {} (previous status: {})", job.jobId(), job.status());
                agentLauncher.launchJobAgent(job, workflowData);

                // Fetch the updated job from database after launching
                job = jobRepository.findById(job.jobId()).orElse(job);
                logger.info("Job {} updated with cursorAgentId: {} (status: {})", job.jobId(), job.cursorAgentId(), job.status());
                justLaunched = true;
            } else {
                // Job already has cursorAgentId - reset workflowStartTime if it's null or stale
                job = timeoutManager.resetStaleWorkflowStartTime(job);
            }

            // Check timeout continuously - this must happen even right after launching
            // to ensure timeout is detected as soon as it occurs
            if (job.cursorAgentId() != null && job.timeoutMillis() != null) {
                TimeoutManager.TimeoutCheckResult timeoutCheck = timeoutManager.checkTimeout(job);
                if (timeoutCheck.hasReachedTimeout()) {
                    // Only execute fallback if job is not already in terminal state and fallback hasn't been executed yet
                    if (!job.status().isTerminal() && (job.fallbackExecuted() == null || !job.fallbackExecuted())) {
                        logger.warn("Job {} has reached timeout ({}ms elapsed, {}ms limit). Executing fallback.",
                            job.jobId(), timeoutCheck.getElapsedMillis(), timeoutCheck.getTimeoutMillis());
                        fallbackExecutor.executeFallback(job, workflowData, timeoutCheck.getElapsedMillis(), timeoutCheck.getTimeoutMillis());
                        // Fetch updated job after fallback execution (may have new cursorAgentId or status)
                        job = jobRepository.findById(job.jobId()).orElse(job);
                        logger.info("Job {} updated after fallback execution, cursorAgentId: {}, status: {}",
                            job.jobId(), job.cursorAgentId(), job.status());
                        // Continue processing to monitor the fallback agent status
                    } else if (job.status().isTerminal()) {
                        logger.info("Job {} has reached timeout but is already in terminal state ({}), skipping fallback.",
                            job.jobId(), job.status());
                        return;
                    } else {
                        logger.debug("Job {} has reached timeout but fallback already executed, skipping.", job.jobId());
                        return;
                    }
                }
            }

            // Check current status and continue processing if needed
            // Don't monitor immediately after launching to allow parallel job processing
            if (job.cursorAgentId() != null && !justLaunched) {
                checkAndUpdateJobStatus(job, prompts, workflowData);
            } else if (justLaunched) {
                logger.trace("Job {} just launched, will check status on next polling cycle", job.jobId());
            }

        } catch (Exception e) {
            logger.error("Error processing job workflow {}: {}", job.jobId(), e.getMessage(), e);
        }
    }

    /**
     * Checks and updates the job status, processing remaining prompts if successful.
     *
     * @param job the job to check
     * @param prompts the list of prompts
     * @param workflowData the workflow data
     */
    private void checkAndUpdateJobStatus(Job job, List<Prompt> prompts, WorkflowData workflowData) {
        try {
            logger.info("Getting agent status for job: {}", job.jobId());
            AgentState currentStatus = cliAgent.getAgentStatus(job.cursorAgentId());

            // Update job status in database
            logger.trace("Job {} status polled: {} -> updating database", job.jobId(), currentStatus);
            cliAgent.updateJobStatusInDatabase(job, currentStatus);

            logger.trace("Job {} status updated to: {}", job.jobId(), currentStatus);

            // Don't block on monitoring - just check status and process if completed
            // This allows multiple jobs to be processed in parallel
            if (currentStatus.isActive()) {
                logger.trace("Job {} is still active, will check again on next polling cycle", job.jobId());
            } else if (currentStatus.isSuccessful()) {
                // Agent completed successfully, process remaining prompts
                logger.info("Job {} completed successfully, processing remaining prompts", job.jobId());
                promptProcessor.processRemainingPrompts(job, prompts, workflowData);
            } else if (currentStatus.isTerminal()) {
                logger.info("Job {} reached terminal state: {}", job.jobId(), currentStatus);
            }
        } catch (Exception statusError) {
            logger.error("Error getting agent status for job {}: {}", job.jobId(), statusError.getMessage());
            // Mark job as failed if we can't get status
            cliAgent.updateJobStatusInDatabase(job, AgentState.error());
        }
    }
}

