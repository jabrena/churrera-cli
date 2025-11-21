package info.jab.churrera.cli.service;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.ParallelWorkflowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for executing fallback actions when jobs timeout.
 */
public class FallbackExecutor {

    private static final Logger logger = LoggerFactory.getLogger(FallbackExecutor.class);

    private final CLIAgent cliAgent;
    private final JobRepository jobRepository;
    private final WorkflowFileService workflowFileService;

    public FallbackExecutor(CLIAgent cliAgent, JobRepository jobRepository, 
                           WorkflowFileService workflowFileService) {
        this.cliAgent = cliAgent;
        this.jobRepository = jobRepository;
        this.workflowFileService = workflowFileService;
    }

    /**
     * Execute fallback prompt when timeout is reached.
     *
     * @param job the job that timed out
     * @param workflowData the workflow data
     * @param elapsedMillis the elapsed time in milliseconds (for logging)
     * @param timeoutMillis the timeout limit in milliseconds (for logging)
     */
    public void executeFallback(Job job, WorkflowData workflowData, long elapsedMillis, long timeoutMillis) {
        try {
            // Check if job is already in a terminal state - don't execute fallback if already finished
            if (job.status().isTerminal()) {
                logger.info("Job {} is already in terminal state ({}), skipping fallback execution.", job.jobId(), job.status());
                System.out.println("ℹ️  Fallback skipped for job: " + job.jobId() + " (status: " + job.status() + " - already terminal)");
                return;
            }

            // Check if fallback has already been executed
            if (job.fallbackExecuted() != null && job.fallbackExecuted()) {
                logger.debug("Fallback already executed for job {}, skipping.", job.jobId());
                return;
            }

            String fallbackSrc = job.fallbackSrc();
            if (fallbackSrc == null || fallbackSrc.trim().isEmpty()) {
                logger.warn("Job {} timed out but no fallback-src specified. Marking as failed.", job.jobId());
                cliAgent.updateJobStatusInDatabase(job, AgentState.ERROR());
                return;
            }

            // Print message when executing fallback due to timeout
            System.out.println("⚠️  FALLBACK ACTION TRIGGERED for job: " + job.jobId());
            logger.info("Executing fallback prompt '{}' for job {} (timeout: {}ms elapsed >= {}ms limit)",
                fallbackSrc, job.jobId(), elapsedMillis, timeoutMillis);

            // Read fallback file
            String fallbackContent = workflowFileService.readPromptFile(job.path(), fallbackSrc);

            // Determine type from file extension
            String type = workflowFileService.inferTypeFromExtension(fallbackSrc);

            // Get bind value from job result if present
            String bindValue = null;
            if (job.result() != null) {
                bindValue = job.result();
            }

            // If job has cursorAgentId, send as follow-up, otherwise launch with fallback
            if (job.cursorAgentId() != null) {
                logger.info("Sending fallback prompt as follow-up to job {}", job.jobId());
                String followUpId = cliAgent.followUpForPrompt(job.cursorAgentId(), fallbackContent, type, bindValue);
                logger.info("Fallback prompt sent as follow-up {} for job {}", followUpId, job.jobId());
            } else {
                logger.info("Launching job {} with fallback prompt", job.jobId());
                String cursorAgentId = cliAgent.launchAgentForJob(job, fallbackContent, type, bindValue, true);
                cliAgent.updateJobCursorIdInDatabase(job, cursorAgentId, AgentState.CREATING());

                // Set workflowStartTime if timeout is configured
                if (job.timeoutMillis() != null) {
                    Job updatedJob = job.withWorkflowStartTime(LocalDateTime.now());
                    jobRepository.save(updatedJob);
                }
                logger.info("Job {} launched with fallback prompt, cursorAgentId: {}", job.jobId(), cursorAgentId);
            }

            // Mark fallback as executed
            Job updatedJob = job.withFallbackExecuted(true);
            jobRepository.save(updatedJob);
            logger.info("Marked fallback as executed for job {}", job.jobId());
            System.out.println("✅ Fallback prompt sent successfully for job: " + job.jobId());
            System.out.println("   Continuing to monitor the fallback agent status...\n");

        } catch (Exception e) {
            logger.error("Error executing fallback for job {}: {}", job.jobId(), e.getMessage(), e);
            try {
                cliAgent.updateJobStatusInDatabase(job, AgentState.ERROR());
            } catch (Exception updateError) {
                logger.error("Error updating job status to FAILED: {}", updateError.getMessage());
            }
        }
    }

    /**
     * Execute fallback for all unfinished child jobs in a parallel workflow when timeout is reached.
     *
     * @param parentJob the parent parallel job
     * @param parallelData the parallel workflow data
     */
    public void executeFallbackForParallelChildren(Job parentJob, ParallelWorkflowData parallelData) {
        try {
            // Check if fallback has already been executed for this parent job
            if (parentJob.fallbackExecuted() != null && parentJob.fallbackExecuted()) {
                logger.debug("Fallback already executed for parallel workflow {}, skipping.", parentJob.jobId());
                return;
            }

            String fallbackSrc = parallelData.getFallbackSrc();
            if (fallbackSrc == null || fallbackSrc.trim().isEmpty()) {
                logger.warn("Parallel workflow {} timed out but no fallback-src specified. Marking as failed.", parentJob.jobId());
                cliAgent.updateJobStatusInDatabase(parentJob, AgentState.ERROR());
                return;
            }

            // Find all child jobs that are not finished
            List<Job> allJobs = jobRepository.findAll();
            List<Job> unfinishedChildren = new ArrayList<>();
            for (Job childJob : allJobs) {
                if (parentJob.jobId().equals(childJob.parentJobId()) && !childJob.status().isTerminal()) {
                    unfinishedChildren.add(childJob);
                }
            }

            System.out.println("⚠️  FALLBACK ACTION TRIGGERED (Parallel Workflow) for job: " + parentJob.jobId());
            logger.info("Executing fallback '{}' for {} unfinished child jobs of parallel workflow {}",
                fallbackSrc, unfinishedChildren.size(), parentJob.jobId());

            // Read fallback file
            String fallbackContent = workflowFileService.readPromptFile(parentJob.path(), fallbackSrc);

            // Determine type from file extension
            String type = workflowFileService.inferTypeFromExtension(fallbackSrc);

            // Execute fallback for each unfinished child
            for (Job childJob : unfinishedChildren) {
                executeFallbackForChild(childJob, fallbackContent, type);
            }

            // Mark fallback as executed for parent job
            Job updatedParentJob = parentJob.withFallbackExecuted(true);
            jobRepository.save(updatedParentJob);
            logger.info("Marked fallback as executed for parallel workflow {}", parentJob.jobId());

        } catch (Exception e) {
            logger.error("Error executing fallback for parallel children: {}", e.getMessage(), e);
        }
    }

    /**
     * Execute fallback for a single child job.
     */
    private void executeFallbackForChild(Job childJob, String fallbackContent, String type) {
        try {
            // Check if fallback has already been executed for this child
            if (childJob.fallbackExecuted() != null && childJob.fallbackExecuted()) {
                logger.debug("Fallback already executed for child job {}, skipping.", childJob.jobId());
                return;
            }

            String bindValue = childJob.result();

            if (childJob.cursorAgentId() != null) {
                logger.info("Sending fallback prompt as follow-up to child job {}", childJob.jobId());
                String followUpId = cliAgent.followUpForPrompt(childJob.cursorAgentId(), fallbackContent, type, bindValue);
                logger.info("Fallback prompt sent as follow-up {} for child job {}", followUpId, childJob.jobId());
            } else {
                logger.info("Launching child job {} with fallback prompt", childJob.jobId());
                String cursorAgentId = cliAgent.launchAgentForJob(childJob, fallbackContent, type, bindValue, true);
                cliAgent.updateJobCursorIdInDatabase(childJob, cursorAgentId, AgentState.CREATING());

                // Set workflowStartTime if timeout is configured
                if (childJob.timeoutMillis() != null) {
                    Job updatedJob = childJob.withWorkflowStartTime(LocalDateTime.now());
                    jobRepository.save(updatedJob);
                }
                logger.info("Child job {} launched with fallback prompt, cursorAgentId: {}", childJob.jobId(), cursorAgentId);
            }

            // Mark fallback as executed for this child
            Job updatedChildJob = childJob.withFallbackExecuted(true);
            jobRepository.save(updatedChildJob);
            logger.info("Marked fallback as executed for child job {}", childJob.jobId());
        } catch (Exception e) {
            logger.error("Error executing fallback for child job {}: {}", childJob.jobId(), e.getMessage(), e);
        }
    }
}

