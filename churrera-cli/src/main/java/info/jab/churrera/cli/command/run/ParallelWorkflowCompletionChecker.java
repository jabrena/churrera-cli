package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Completion checker for parallel workflows.
 */
public class ParallelWorkflowCompletionChecker implements CompletionChecker {
    private static final Logger logger = LoggerFactory.getLogger(ParallelWorkflowCompletionChecker.class);

    private final JobRepository jobRepository;

    public ParallelWorkflowCompletionChecker(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public CompletionCheckResult checkCompletion(Job job, String jobId) {
        List<Job> childJobs = findChildJobs(jobId);

        if (!job.status().isTerminal()) {
            return new CompletionCheckResult(false, null, childJobs);
        }

        // If no child jobs exist, parent completion is sufficient
        if (childJobs.isEmpty()) {
            logger.info("Parent job {} reached terminal state with no child jobs", jobId);
            System.out.println("\nJob completed with status: " + job.status());
            return new CompletionCheckResult(true, job.status(), childJobs);
        }

        // Check if all child jobs are terminal
        if (!areAllChildrenTerminal(childJobs)) {
            int activeChildren = countActiveChildren(childJobs);
            logger.debug("Parent job {} is terminal but {} of {} child jobs are still active",
                jobId, activeChildren, childJobs.size());
            return new CompletionCheckResult(false, null, childJobs);
        }

        // All children are terminal
        logger.info("Parent job {} and all {} child jobs reached terminal state", jobId, childJobs.size());
        System.out.println("\nJob completed with status: " + job.status());
        System.out.println("All " + childJobs.size() + " child jobs completed.");

        AgentState finalStatus = determineFinalStatusForParallelWorkflow(job, childJobs);
        return new CompletionCheckResult(true, finalStatus, childJobs);
    }

    /**
     * Finds all child jobs for a given parent job ID.
     */
    private List<Job> findChildJobs(String parentJobId) {
        List<Job> allJobs;
        try {
            allJobs = jobRepository.findAll();
        } catch (BaseXException | QueryException e) {
            throw new RuntimeException("Error retrieving all jobs: " + e.getMessage(), e);
        }
        List<Job> childJobs = new ArrayList<>();
        for (Job j : allJobs) {
            if (parentJobId.equals(j.parentJobId())) {
                childJobs.add(j);
            }
        }
        return childJobs;
    }

    /**
     * Checks if all child jobs are terminal.
     */
    private boolean areAllChildrenTerminal(List<Job> childJobs) {
        for (Job childJob : childJobs) {
            if (!childJob.status().isTerminal()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Counts the number of active (non-terminal) child jobs.
     */
    private int countActiveChildren(List<Job> childJobs) {
        int count = 0;
        for (Job childJob : childJobs) {
            if (!childJob.status().isTerminal()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Determines the final status for a parallel workflow based on parent and child job statuses.
     */
    private AgentState determineFinalStatusForParallelWorkflow(Job parentJob, List<Job> childJobs) {
        boolean allSuccessful = parentJob.status().isSuccessful();
        AgentState finalStatus = null;

        for (Job childJob : childJobs) {
            if (!childJob.status().isSuccessful()) {
                allSuccessful = false;
                // If any child failed, use that status as final
                if (finalStatus == null || finalStatus.isSuccessful()) {
                    finalStatus = childJob.status();
                }
            }
        }

        // If all successful, use parent status (FINISHED)
        return allSuccessful ? parentJob.status() : finalStatus;
    }
}

