package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;

import java.util.List;

/**
 * Result of job completion check.
 */
public class CompletionCheckResult {
    private final boolean completed;
    private final AgentState finalStatus;
    private final List<Job> childJobs;

    /**
     * Creates a new completion check result.
     *
     * @param completed whether the job is completed
     * @param finalStatus the final status of the job (null if not completed)
     * @param childJobs the list of child jobs (empty for non-parallel workflows)
     */
    public CompletionCheckResult(boolean completed, AgentState finalStatus, List<Job> childJobs) {
        this.completed = completed;
        this.finalStatus = finalStatus;
        this.childJobs = childJobs;
    }

    /**
     * Returns whether the job is completed.
     *
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Returns the final status of the job.
     *
     * @return the final status (null if not completed)
     */
    public AgentState getFinalStatus() {
        return finalStatus;
    }

    /**
     * Returns the list of child jobs.
     *
     * @return the list of child jobs (empty for non-parallel workflows)
     */
    public List<Job> getChildJobs() {
        return childJobs;
    }
}

