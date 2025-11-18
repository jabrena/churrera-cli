package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Completion checker for non-parallel workflows.
 */
public class SimpleWorkflowCompletionChecker implements CompletionChecker {
    private static final Logger logger = LoggerFactory.getLogger(SimpleWorkflowCompletionChecker.class);

    @Override
    public CompletionCheckResult checkCompletion(Job job, String jobId) {
        if (job.status().isTerminal()) {
            logger.info("Job {} reached terminal state: {}", jobId, job.status());
            System.out.println("\nJob completed with status: " + job.status());
            return new CompletionCheckResult(true, job.status(), new ArrayList<>());
        }
        return new CompletionCheckResult(false, null, new ArrayList<>());
    }
}

