package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.Job;

/**
 * Interface for checking job completion status.
 */
public interface CompletionChecker {
    /**
     * Checks if a job has completed.
     *
     * @param job the job to check
     * @param jobId the job ID
     * @return completion check result
     */
    CompletionCheckResult checkCompletion(Job job, String jobId);
}

