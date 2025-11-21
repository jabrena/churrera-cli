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
            List<info.jab.churrera.workflow.PromptInfo> sequencePrompts = sequenceInfo.getPrompts();
            if (sequencePrompts.isEmpty()) {
                logger.error("No prompts found in sequence for child job: {}", job.jobId());
                return;
            }

            // First prompt is launch, rest are updates
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

            // Check timeout for child job (inherit from parent parallel workflow if not set)
            Long timeoutMillis = job.timeoutMillis();
            if (timeoutMillis == null) {
                timeoutMillis = parallelData.getTimeoutMillis();
            }

            // Process as standard sequence workflow
            boolean justLaunched = false;
            if (job.cursorAgentId() == null) {
                logger.info("Launching child job agent: {}", job.jobId());
                agentLauncher.launchJobAgent(job, childWorkflowData);

                // Fetch the updated job from database after launching
                job = jobRepository.findById(job.jobId()).orElse(job);
                logger.info("Child job {} updated with cursorAgentId: {}", job.jobId(), job.cursorAgentId());
                justLaunched = true;
            }

            // Non-blocking poll and process prompts per cycle for child jobs
            // Always check status if job has cursorAgentId (even if database shows terminal)
            // to ensure database is synchronized with actual agent status
            if (job.cursorAgentId() != null && !justLaunched) {
                // Check timeout again before processing
                if (timeoutMillis != null && job.workflowStartTime() != null) {
                    long elapsedMillis = timeoutManager.getElapsedMillis(job);
                    logger.debug("Child job {} timeout check: elapsed={}ms, limit={}ms, workflowStartTime={}",
                        job.jobId(), elapsedMillis, timeoutMillis, job.workflowStartTime());
                    if (elapsedMillis >= timeoutMillis) {
                        // Only execute fallback if job is not already in terminal state and fallback hasn't been executed yet
                        if (!job.status().isTerminal() && (job.fallbackExecuted() == null || !job.fallbackExecuted())) {
                            logger.warn("Child job {} has reached timeout ({}ms elapsed, {}ms limit). Executing fallback.",
                                job.jobId(), elapsedMillis, timeoutMillis);
                            String fallbackSrc = job.fallbackSrc();
                            if (fallbackSrc == null) {
                                fallbackSrc = parallelData.getFallbackSrc();
                            }
                            if (fallbackSrc != null) {
                                fallbackExecutor.executeFallback(job.withFallbackSrc(fallbackSrc), childWorkflowData, elapsedMillis, timeoutMillis);
                                // Fetch updated job after fallback execution (may have new cursorAgentId or status)
                                job = jobRepository.findById(job.jobId()).orElse(job);
                                logger.info("Child job {} updated after fallback execution, cursorAgentId: {}, status: {}",
                                    job.jobId(), job.cursorAgentId(), job.status());
                                // Continue processing to monitor the fallback agent status
                            }
                        } else if (job.status().isTerminal()) {
                            logger.info("Child job {} has reached timeout but is already in terminal state ({}), skipping fallback.",
                                job.jobId(), job.status());
                            // Don't return here - still check agent status to ensure database is synchronized
                        } else {
                            logger.debug("Child job {} has reached timeout but fallback already executed, skipping.", job.jobId());
                            // Don't return here - still check agent status to ensure database is synchronized
                        }
                    }
                }
                // Always check agent status to ensure database is synchronized with actual agent state
                // This is critical for child jobs in parallel workflows where agents may finish
                // but database status hasn't been updated yet
                job = checkAndUpdateChildJobStatus(job, prompts, childWorkflowData);
            } else if (justLaunched) {
                logger.info("Child job {} just launched, will check status on next polling cycle", job.jobId());
            }

        } catch (Exception e) {
            logger.error("Error processing child job workflow {}: {}", job.jobId(), e.getMessage(), e);
        }
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
            cliAgent.updateJobStatusInDatabase(job, AgentState.ERROR());
            return job;
        }
    }
}

