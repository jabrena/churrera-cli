package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.JobWithDetails;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.cursor.client.model.ConversationMessage;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;

import info.jab.churrera.util.PropertyResolver;
import info.jab.churrera.util.PmlConverter;

/**
 * Command to show logs for a specific job by its UUID.
 */
@CommandLine.Command(name = "jobs logs", description = "Show logs for specific job by UUID")
public class JobLogsCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(JobLogsCommand.class);

    private static final int JOB_ID_PREFIX_LENGTH = 8;

    private final JobRepository jobRepository;
    private final CLIAgent cliAgent;
    private final String jobId;

    // Public constructor for dependency injection
    public JobLogsCommand(JobRepository jobRepository, CLIAgent cliAgent, String jobId) {
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
        this.jobId = jobId;
    }

    @Override
    public void run() {
        logger.info("Retrieving job logs for jobId: {}", jobId);
        try {
            String resolvedJobId = resolveJobId(jobId);

            if (resolvedJobId == null) {
                logger.warn("Could not resolve jobId: {}", jobId);
                return; // message already printed in resolver
            }

            logger.debug("Resolved jobId: {} -> {}", jobId, resolvedJobId);
            Optional<JobWithDetails> jobDetailsOpt = jobRepository.findJobWithDetails(resolvedJobId);

            if (jobDetailsOpt.isEmpty()) {
                logger.warn("Job not found: {}", resolvedJobId);
                System.out.println("Job not found: " + resolvedJobId);
            } else {
                JobWithDetails jobDetails = jobDetailsOpt.get();
                Job job = jobDetails.getJob();
                List<Prompt> prompts = jobDetails.getPrompts();

                logger.info("Retrieved job details for jobId: {}, found {} prompts", resolvedJobId, prompts.size());
                System.out.println("=== Job Logs ===");
                System.out.println("Job ID: " + job.jobId());
                System.out.println("Path: " + job.path());
                System.out.println("Model: " + job.model());
                System.out.println("Repository: " + job.repository());
                System.out.println("Status: " + job.status());
                System.out.println("Created: " + job.createdAt());
                System.out.println("Last Update: " + job.lastUpdate());
                System.out.println();
                System.out.println("=== Log Entries ===");
                System.out.println("[" + job.createdAt() + "] Job created with path: " + job.path());

                // If job has cursorAgentId, try to fetch conversation
                if (job.cursorAgentId() != null) {
                    logger.debug("Fetching conversation for cursorAgentId: {}", job.cursorAgentId());
                    try {
                        System.out.println("\n=== Cursor Agent Conversation ===");
                        var conversation = cliAgent.getConversation(job.cursorAgentId());
                        if (conversation != null && conversation.messages() != null) {
                            logger.debug("Retrieved {} conversation messages", conversation.messages().size());
                            for (ConversationMessage message : conversation.messages()) {
                                System.out.println("[conversation] " + message.text());
                            }
                        } else {
                            logger.debug("No conversation messages available");
                            System.out.println("No conversation messages available");
                        }
                    } catch (Exception e) {
                        logger.error("Failed to fetch conversation for cursorAgentId: {}", job.cursorAgentId(), e);
                        System.out.println("Failed to fetch conversation: " + e.getMessage());
                    }
                } else {
                    logger.debug("No cursorAgentId found for job");
                }

                logger.debug("Displaying {} prompt entries", prompts.size());
                for (Prompt prompt : prompts) {
                    System.out.println("[" + prompt.createdAt() + "] Prompt created: " + prompt.promptId() + " (PML: "
                            + prompt.pmlFile() + ")");
                    System.out.println("[" + prompt.lastUpdate() + "] Prompt updated: " + prompt.promptId()
                            + " (Status: " + prompt.status() + ")");
                }
                logger.info("Successfully displayed job logs for jobId: {}", resolvedJobId);
            }
        } catch (BaseXException | QueryException e) {
            logger.error("Error retrieving job logs for jobId: {}", jobId, e);
            System.err.println("Error retrieving job logs: " + e.getMessage());
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
        logger.debug("Resolving jobId: {}", provided);
        // Try exact match first
        var exact = jobRepository.findById(provided);
        if (exact.isPresent()) {
            logger.debug("Found exact match for jobId: {}", provided);
            return provided;
        }

        // If 8-char prefix, try to resolve by startsWith
        if (provided != null && provided.length() == JOB_ID_PREFIX_LENGTH) {
            return resolveByPrefix(provided);
        }

        // Not exact, not 8-char prefix
        logger.warn("Job not found: {}", provided);
        System.out.println("Job not found: " + provided);
        return null;
    }

    private String resolveByPrefix(String provided) throws BaseXException, QueryException {
        logger.debug("Attempting prefix match for jobId prefix: {}", provided);
        List<Job> all = jobRepository.findAll();
        List<Job> matches = new java.util.ArrayList<>();
        for (Job j : all) {
            if (j.jobId() != null && j.jobId().startsWith(provided)) {
                matches.add(j);
            }
        }

        if (matches.isEmpty()) {
            logger.warn("No job found starting with prefix: {}", provided);
            System.out.println("No job found starting with: " + provided);
            return null;
        }
        if (matches.size() > 1) {
            handleAmbiguousPrefix(provided, matches);
            return null;
        }

        logger.debug("Found unique match for prefix {}: {}", provided, matches.get(0).jobId());
        return matches.get(0).jobId();
    }

    private void handleAmbiguousPrefix(String provided, List<Job> matches) {
        logger.warn("Ambiguous job prefix '{}' matches {} jobs", provided, matches.size());
        System.out.println("Ambiguous job prefix '" + provided + "' matches multiple jobs:");
        for (Job m : matches) {
            System.out.println("  - " + m.jobId());
        }
        System.out.println("Please specify a full UUID or a unique 8-char prefix.");
    }
}
