package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.AgentState;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Command to show PR link for a specific job.
 */
@CommandLine.Command(
    name = "jobs pr",
    description = "Show PR link for a specific job (only for finished jobs)"
)
public class JobsPrCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(JobsPrCommand.class);

    private final JobRepository jobRepository;
    private final String jobId;
    private static final int JOB_ID_PREFIX_LENGTH = 8;

    public JobsPrCommand(JobRepository jobRepository, String jobId) {
        this.jobRepository = jobRepository;
        this.jobId = jobId;
    }

    @Override
    public void run() {
        try {
            String resolvedJobId = resolveJobId(jobId);

            if (resolvedJobId == null) {
                return; // message already printed in resolver
            }

            Optional<Job> jobOpt = jobRepository.findById(resolvedJobId);

            if (jobOpt.isEmpty()) {
                System.out.println("Job not found: " + resolvedJobId);
                logger.info("Job not found: {}", resolvedJobId);
                return;
            }

            Job job = jobOpt.get();
            AgentState status = job.status();

            if (status.isTerminal() && status.isSuccessful()) {
                // Job is finished successfully, show PR link
                if (job.cursorAgentId() != null) {
                    // Generate PR review URL based on repository
                    String prUrl = generatePrReviewUrl(job.repository());
                    System.out.println("🔍 Pull Request Review:");
                    System.out.println("   📋 " + prUrl);
                    System.out.println("   🔗 Agent Details: https://cursor.com/agents?selectedBcId=" + job.cursorAgentId());
                } else {
                    System.out.println("No Cursor agent ID found for this job.");
                }
            } else if (status.isTerminal() && status.isFailed()) {
                System.out.println("❌ Job failed or was terminated. No PR available.");
            } else {
                // Job is still running
                System.out.println("🔄 Job is still running. Status: " + status);
                System.out.println("   Please wait for the job to complete to view the PR link.");
            }

        } catch (BaseXException | QueryException e) {
            String errorMessage = "Error retrieving job PR info: " + e.getMessage();
            System.out.println(errorMessage);
            logger.error("Error retrieving job PR info: {}", e.getMessage());
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
        var exact = jobRepository.findById(provided);
        if (exact.isPresent()) {
            return provided;
        }

        // If 8-char prefix, try to resolve by startsWith
        if (provided != null && provided.length() == JOB_ID_PREFIX_LENGTH) {
            return resolveByPrefix(provided);
        }

        // Not exact, not 8-char prefix
        System.out.println("Job not found: " + provided);
        return null;
    }

    private String resolveByPrefix(String provided) throws BaseXException, QueryException {
        var all = jobRepository.findAll();
        var matches = new ArrayList<Job>();
        for (Job j : all) {
            if (j.jobId() != null && j.jobId().startsWith(provided)) {
                matches.add(j);
            }
        }

        if (matches.isEmpty()) {
            System.out.println("No job found starting with: " + provided);
            return null;
        }
        if (matches.size() > 1) {
            handleAmbiguousPrefix(provided, matches);
            return null;
        }

        return matches.get(0).jobId();
    }

    private void handleAmbiguousPrefix(String provided, List<Job> matches) {
        System.out.println("Ambiguous job prefix '" + provided + "' matches multiple jobs:");
        for (Job m : matches) {
            System.out.println("  - " + m.jobId());
        }
        System.out.println("Please specify a full UUID or a unique 8-char prefix.");
    }

    /**
     * Generate PR review URL based on repository URL.
     * This is a simplified implementation - in a real scenario, you might need
     * to parse the repository URL and construct the appropriate PR URL.
     */
    private String generatePrReviewUrl(String repository) {
        if (repository == null || repository.trim().isEmpty()) {
            return "Repository URL not available";
        }

        // If it's a GitHub URL, convert to PR review format
        if (repository.contains("github.com")) {
            // Remove .git suffix if present
            String cleanRepo = repository.replaceAll("\\.git$", "");
            return cleanRepo + "/pulls";
        }

        // For other repositories, return the repository URL
        return repository;
    }
}
