package info.jab.churrera.cli.service;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Manages timeout checking and workflow start time tracking for jobs.
 */
public class TimeoutManager {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutManager.class);

    private final JobRepository jobRepository;

    @Inject
    public TimeoutManager(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Reset workflow start time for a job if needed (when launching or if stale).
     *
     * @param job the job to check
     * @return the updated job with reset workflow start time if needed
     */
    public Job resetWorkflowStartTimeIfNeeded(Job job) {
        if (job.timeoutMillis() == null) {
            return job;
        }

        try {
            // Reset workflowStartTime before launching to ensure we start fresh
            // This handles the case where a job was previously processed but is being restarted
            Job jobWithResetTime = job.withWorkflowStartTime(LocalDateTime.now());
            jobRepository.save(jobWithResetTime);
            Job updatedJob = jobRepository.findById(job.jobId()).orElse(job);
            logger.info("Reset workflowStartTime for job {} with timeout {}ms (starting fresh)",
                job.jobId(), job.timeoutMillis());
            return updatedJob;
        } catch (Exception e) {
            logger.error("Error resetting workflow start time for job {}: {}", job.jobId(), e.getMessage());
            return job;
        }
    }

    /**
     * Reset workflow start time for an existing job if it's null or stale.
     * A stale workflowStartTime is one that's much older than the timeout (e.g., > 2x timeout),
     * indicating it's from a previous run, not the current active execution.
     *
     * @param job the job to check
     * @return the updated job with reset workflow start time if needed
     */
    public Job resetStaleWorkflowStartTime(Job job) {
        if (job.timeoutMillis() == null || job.status().isTerminal()) {
            return job;
        }

        boolean shouldReset = false;
        if (job.workflowStartTime() == null) {
            shouldReset = true;
            logger.info("workflowStartTime is null for job {} with timeout, setting it to now", job.jobId());
        } else {
            long elapsedMillis = Duration.between(job.workflowStartTime(), LocalDateTime.now()).toMillis();
            // If elapsed time is more than 2x the timeout, it's likely stale from a previous run
            // Reset it so the timeout starts fresh for this execution
            if (elapsedMillis > (job.timeoutMillis() * 2)) {
                shouldReset = true;
                logger.info("workflowStartTime for job {} is stale ({}ms elapsed, {}ms timeout, >2x timeout). Resetting to now for fresh timeout tracking.",
                    job.jobId(), elapsedMillis, job.timeoutMillis());
            }
        }

        if (shouldReset) {
            try {
                Job jobWithResetTime = job.withWorkflowStartTime(LocalDateTime.now());
                jobRepository.save(jobWithResetTime);
                Job updatedJob = jobRepository.findById(job.jobId()).orElse(job);
                logger.info("Reset workflowStartTime for existing job {} with timeout {}ms (starting fresh)",
                    job.jobId(), job.timeoutMillis());
                return updatedJob;
            } catch (Exception e) {
                logger.error("Error resetting stale workflow start time for job {}: {}", job.jobId(), e.getMessage());
                return job;
            }
        }

        return job;
    }

    /**
     * Ensure workflow start time is set for a job with timeout.
     *
     * @param job the job to check
     * @return the updated job with workflow start time set if needed
     */
    public Job ensureWorkflowStartTimeSet(Job job) {
        if (job.timeoutMillis() != null && job.workflowStartTime() == null) {
            try {
                logger.warn("workflowStartTime is null for job {} with timeout, setting it to now", job.jobId());
                Job jobWithTime = job.withWorkflowStartTime(LocalDateTime.now());
                jobRepository.save(jobWithTime);
                return jobRepository.findById(job.jobId()).orElse(job);
            } catch (Exception e) {
                logger.error("Error ensuring workflow start time set for job {}: {}", job.jobId(), e.getMessage());
                return job;
            }
        }
        return job;
    }

    /**
     * Check if a job has reached its timeout.
     *
     * @param job the job to check
     * @return true if timeout has been reached, false otherwise
     */
    public boolean hasReachedTimeout(Job job) {
        if (job.cursorAgentId() == null || job.timeoutMillis() == null) {
            return false;
        }

        Job jobWithTime = ensureWorkflowStartTimeSet(job);
        long elapsedMillis = Duration.between(jobWithTime.workflowStartTime(), LocalDateTime.now()).toMillis();
        logger.info("Job {} timeout check: elapsed={}ms, limit={}ms, workflowStartTime={}",
            job.jobId(), elapsedMillis, job.timeoutMillis(), jobWithTime.workflowStartTime());
        return elapsedMillis >= job.timeoutMillis();
    }

    /**
     * Get elapsed time in milliseconds for a job.
     *
     * @param job the job to check
     * @return elapsed time in milliseconds, or 0 if timeout is not configured
     */
    public long getElapsedMillis(Job job) {
        if (job.timeoutMillis() == null || job.workflowStartTime() == null) {
            return 0;
        }
        return Duration.between(job.workflowStartTime(), LocalDateTime.now()).toMillis();
    }

    /**
     * Check if a job has reached its timeout and return elapsed time.
     *
     * @param job the job to check
     * @return TimeoutCheckResult with timeout status and elapsed time
     */
    public TimeoutCheckResult checkTimeout(Job job) {
        if (job.cursorAgentId() == null || job.timeoutMillis() == null) {
            return new TimeoutCheckResult(false, 0, job.timeoutMillis());
        }

        Job jobWithTime = ensureWorkflowStartTimeSet(job);
        long elapsedMillis = Duration.between(jobWithTime.workflowStartTime(), LocalDateTime.now()).toMillis();
        logger.info("Job {} timeout check: elapsed={}ms, limit={}ms, workflowStartTime={}",
            job.jobId(), elapsedMillis, job.timeoutMillis(), jobWithTime.workflowStartTime());
        boolean hasReachedTimeout = elapsedMillis >= job.timeoutMillis();
        return new TimeoutCheckResult(hasReachedTimeout, elapsedMillis, job.timeoutMillis());
    }

    /**
     * Result of a timeout check.
     */
    public static class TimeoutCheckResult {
        private final boolean hasReachedTimeout;
        private final long elapsedMillis;
        private final Long timeoutMillis;

        public TimeoutCheckResult(boolean hasReachedTimeout, long elapsedMillis, Long timeoutMillis) {
            this.hasReachedTimeout = hasReachedTimeout;
            this.elapsedMillis = elapsedMillis;
            this.timeoutMillis = timeoutMillis;
        }

        public boolean hasReachedTimeout() {
            return hasReachedTimeout;
        }

        public long getElapsedMillis() {
            return elapsedMillis;
        }

        public Long getTimeoutMillis() {
            return timeoutMillis;
        }
    }
}

