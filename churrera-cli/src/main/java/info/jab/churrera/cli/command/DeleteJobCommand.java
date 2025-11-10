package info.jab.churrera.cli.command;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.cli.model.Job;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.List;

/**
 * Command to delete a job by its UUID.
 * If the job is a parent of other jobs, all child jobs will be deleted as well
 * (cascade delete).
 */
@CommandLine.Command(name = "jobs delete", description = "Delete job by UUID (cascade deletes child jobs)")
public class DeleteJobCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DeleteJobCommand.class);

    private static final int JOB_ID_PREFIX_LENGTH = 8;

    private final JobRepository jobRepository;
    private final CLIAgent cliAgent;
    private final String jobId;

    /**
     * Constructor for DeleteJobCommand.
     *
     * @param jobRepository the job repository
     * @param cliAgent      the CLI agent instance (shared from JobProcessor)
     * @param jobId         the job ID to delete
     */
    public DeleteJobCommand(JobRepository jobRepository, CLIAgent cliAgent, String jobId) {
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
        this.jobId = jobId;
    }

    @Override
    public void run() {
        try {
            // Resolve provided ID (full UUID or 8-char prefix) to a concrete job ID
            String resolvedJobId = resolveJobId(jobId);

            if (resolvedJobId == null) {
                return; // message already printed in resolver
            }

            // Check if job exists with the resolved ID
            var jobOpt = jobRepository.findById(resolvedJobId);
            if (jobOpt.isEmpty()) {
                System.out.println("Job not found: " + resolvedJobId);
                return;
            }

            Job job = jobOpt.get();

            // Delete child jobs recursively first
            deleteChildJobsRecursively(resolvedJobId);

            // Delete the parent job
            deleteJob(job);

            System.out.println("Job and all child jobs deleted from Database");

        } catch (BaseXException | QueryException e) {
            logger.error("Error deleting job {}: {}", jobId, e.getMessage());
            System.err.println("Error deleting job: " + e.getMessage());
        }
    }

    /**
     * Resolve a user-provided job identifier to a full job ID. Accepts either:
     * - Full UUID (exact match)
     * - First 8 characters of a jobId, if uniquely identifying exactly one job
     *
     * Prints helpful messages for not found or ambiguous prefix cases.
     */
    private String resolveJobId(String provided) throws BaseXException, QueryException {
        // Try exact match first
        String exactMatch = tryExactMatch(provided);
        if (exactMatch != null) {
            return exactMatch;
        }

        // If 8-char prefix, try to resolve by startsWith
        if (isEightCharPrefix(provided)) {
            return resolveByPrefix(provided);
        }

        // Not exact, not 8-char prefix
        System.out.println("Job not found: " + provided);
        return null;
    }

    /**
     * Try to find an exact match for the provided job ID.
     *
     * @param provided the job ID to search for
     * @return the job ID if found, null otherwise
     */
    String tryExactMatch(String provided) throws BaseXException, QueryException {
        if (provided == null) {
            return null;
        }
        var exact = jobRepository.findById(provided);
        if (exact.isPresent()) {
            return provided;
        }
        return null;
    }

    /**
     * Check if the provided string is an 8-character prefix.
     *
     * @param provided the string to check
     * @return true if the string is exactly 8 characters, false otherwise
     */
    boolean isEightCharPrefix(String provided) {
        return provided != null && provided.length() == JOB_ID_PREFIX_LENGTH;
    }

    /**
     * Resolve a job ID by 8-character prefix matching.
     *
     * @param prefix the 8-character prefix to match
     * @return the full job ID if uniquely matched, null otherwise
     */
    String resolveByPrefix(String prefix) throws BaseXException, QueryException {
        List<Job> all = jobRepository.findAll();
        List<Job> matches = findMatchingJobsByPrefix(all, prefix);

        if (matches.isEmpty()) {
            System.out.println("No job found starting with: " + prefix);
            return null;
        }
        if (matches.size() > 1) {
            System.out.println("Ambiguous job prefix '" + prefix + "' matches multiple jobs:");
            for (Job m : matches) {
                System.out.println("  - " + m.jobId());
            }
            System.out.println("Please specify a full UUID or a unique 8-char prefix.");
            return null;
        }

        return matches.get(0).jobId();
    }

    /**
     * Find all jobs that start with the given prefix.
     *
     * @param allJobs the list of all jobs to search
     * @param prefix  the prefix to match
     * @return list of matching jobs
     */
    List<Job> findMatchingJobsByPrefix(List<Job> allJobs, String prefix) {
        List<Job> matches = new java.util.ArrayList<>();
        for (Job j : allJobs) {
            if (j.jobId() != null && j.jobId().startsWith(prefix)) {
                matches.add(j);
            }
        }
        return matches;
    }

    /**
     * Recursively delete all child jobs of a parent job.
     *
     * @param parentJobId the parent job ID
     */
    private void deleteChildJobsRecursively(String parentJobId) throws BaseXException, QueryException {
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
     *
     * @param job the job to delete
     */
    private void deleteJob(Job job) throws BaseXException, QueryException {
        // Delete Cursor agent if it exists
        if (job.cursorAgentId() != null) {
            try {
                cliAgent.deleteAgent(job.cursorAgentId());
                logger.info("Deleted Cursor agent for job {}: {}", job.jobId(), job.cursorAgentId());
            } catch (Exception e) {
                logger.error("Failed to delete Cursor agent {} for job {}: {}",
                        job.cursorAgentId(), job.jobId(), e.getMessage());
                System.err
                        .println("  ⚠️  Failed to delete Cursor agent for job " + job.jobId() + ": " + e.getMessage());
                // Continue with database deletion even if Cursor API fails
            }
        }

        // Delete all prompts from database
        jobRepository.deletePromptsByJobId(job.jobId());

        // Delete the job from database
        jobRepository.deleteById(job.jobId());
    }
}
