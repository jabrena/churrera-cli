package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service for deleting jobs and their child jobs.
 */
public class JobDeletionService {
    private static final Logger logger = LoggerFactory.getLogger(JobDeletionService.class);

    private final JobRepository jobRepository;
    private final CLIAgent cliAgent;

    public JobDeletionService(JobRepository jobRepository, CLIAgent cliAgent) {
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
    }

    /**
     * Handles job deletion based on command-line flags.
     *
     * @param jobId the job ID
     * @param job the job
     * @param childJobs the list of child jobs (empty for non-parallel workflows)
     * @param deleteOnCompletion whether to delete on any completion
     * @param deleteOnSuccessCompletion whether to delete only on success completion
     */
    public void handleDeletion(String jobId, Job job, List<Job> childJobs,
                                boolean deleteOnCompletion, boolean deleteOnSuccessCompletion) {
        if (deleteOnCompletion) {
            deleteJobAndChildren(jobId, "--delete-on-completion");
        } else if (deleteOnSuccessCompletion && shouldDeleteOnSuccess(job, childJobs)) {
            deleteJobAndChildren(jobId, "--delete-on-success-completion");
        }
    }

    /**
     * Determines if job should be deleted based on success status.
     */
    private boolean shouldDeleteOnSuccess(Job job, List<Job> childJobs) {
        if (childJobs.isEmpty()) {
            return job.status().isSuccessful();
        } else {
            return isJobAndChildrenSuccessful(job.jobId(), childJobs);
        }
    }

    /**
     * Check if a job and all its child jobs completed successfully.
     * Package-private for testing.
     */
    boolean isJobAndChildrenSuccessful(String jobId, List<Job> childJobs) {
        try {
            // Check parent job status
            Job parentJob = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            if (!parentJob.status().isSuccessful()) {
                logger.info("Parent job {} did not complete successfully (status: {}), skipping deletion",
                    jobId, parentJob.status());
                return false;
            }

            // Check all child jobs are successful
            for (Job childJob : childJobs) {
                if (!childJob.status().isSuccessful()) {
                    logger.info("Child job {} did not complete successfully (status: {}), skipping deletion",
                        childJob.jobId(), childJob.status());
                    return false;
                }
            }

            logger.info("Parent job {} and all {} child jobs completed successfully", jobId, childJobs.size());
            return true;
        } catch (Exception e) {
            logger.error("Error checking job success status for {}: {}", jobId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete a job and all its child jobs recursively.
     * Package-private for testing.
     */
    void deleteJobAndChildren(String jobId, String reason) {
        try {
            logger.info("Deleting job {} and all child jobs ({} enabled)", jobId, reason);
            System.out.println("Deleting job and all child jobs...");

            // Delete child jobs recursively first
            deleteChildJobsRecursively(jobId);

            // Delete the parent job
            var jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isPresent()) {
                deleteJob(jobOpt.get());
                System.out.println("Job and all child jobs deleted successfully");
            }
        } catch (Exception e) {
            logger.error("Error deleting job {}: {}", jobId, e.getMessage(), e);
            System.err.println("Error deleting job: " + e.getMessage());
        }
    }

    /**
     * Recursively delete all child jobs of a parent job.
     * Package-private for testing.
     */
    void deleteChildJobsRecursively(String parentJobId) {
        List<Job> childJobs = jobRepository.findJobsByParentId(parentJobId);

        for (Job childJob : childJobs) {
            // First delete this child's children (depth-first)
            deleteChildJobsRecursively(childJob.jobId());

            // Then delete this child job
            deleteJob(childJob);
            logger.info("Deleted child job: {}", childJob.jobId());
        }
    }

    /**
     * Delete a single job including its Cursor agent and prompts.
     * Package-private for testing.
     */
    void deleteJob(Job job) {
        // Delete Cursor agent if it exists
        if (job.cursorAgentId() != null) {
            try {
                cliAgent.deleteAgent(job.cursorAgentId());
                logger.info("Deleted Cursor agent for job {}: {}", job.jobId(), job.cursorAgentId());
            } catch (Exception e) {
                logger.error("Failed to delete Cursor agent {} for job {}: {}",
                        job.cursorAgentId(), job.jobId(), e.getMessage());
                System.err.println("  ⚠️  Failed to delete Cursor agent for job " + job.jobId() + ": " + e.getMessage());
                // Continue with database deletion even if Cursor API fails
            }
        }

        // Delete all prompts from database
        jobRepository.deletePromptsByJobId(job.jobId());

        // Delete the job from database
        jobRepository.deleteById(job.jobId());
    }
}

