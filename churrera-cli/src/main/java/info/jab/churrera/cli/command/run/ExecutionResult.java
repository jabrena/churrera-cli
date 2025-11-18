package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;

import java.util.List;

/**
 * Result of job execution polling loop.
 */
public class ExecutionResult {
    private final AgentState finalStatus;
    private final boolean interrupted;
    private final List<Job> childJobs;

    public ExecutionResult(AgentState finalStatus, boolean interrupted) {
        this(finalStatus, interrupted, List.of());
    }

    public ExecutionResult(AgentState finalStatus, boolean interrupted, List<Job> childJobs) {
        this.finalStatus = finalStatus;
        this.interrupted = interrupted;
        this.childJobs = childJobs;
    }

    public AgentState getFinalStatus() {
        return finalStatus;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public List<Job> getChildJobs() {
        return childJobs;
    }
}

