package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowType;

/**
 * Factory for creating completion checkers based on workflow type.
 */
public class CompletionCheckerFactory {
    private final JobRepository jobRepository;

    public CompletionCheckerFactory(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Creates a completion checker for the given workflow type.
     *
     * @param workflowType the workflow type
     * @return appropriate completion checker
     */
    public CompletionChecker create(WorkflowType workflowType) {
        if (workflowType == WorkflowType.PARALLEL) {
            return new ParallelWorkflowCompletionChecker(jobRepository);
        } else {
            return new SimpleWorkflowCompletionChecker();
        }
    }
}

