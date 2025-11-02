package info.jab.churrera.workflow;

/**
 * Represents the type of workflow execution pattern.
 */
public enum WorkflowType {
    /**
     * Sequential workflow execution.
     */
    SEQUENCE,

    /**
     * Parallel workflow execution that creates child jobs.
     */
    PARALLEL
}

