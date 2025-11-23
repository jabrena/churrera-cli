package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.cursor.client.model.ConversationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for displaying agent conversation logs.
 */
public class JobLogDisplayService {
    private static final Logger logger = LoggerFactory.getLogger(JobLogDisplayService.class);

    private final JobRepository jobRepository;
    private final CLIAgent cliAgent;

    public JobLogDisplayService(JobRepository jobRepository, CLIAgent cliAgent) {
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
    }

    /**
     * Displays conversation logs for a job if it has a cursor agent ID.
     *
     * @param job the job to display logs for
     */
    public void displayLogsForJob(Job job) {
        if (job.cursorAgentId() == null) {
            logger.debug("Job {} does not have a cursorAgentId, skipping log display", job.jobId());
            return;
        }

        logger.info("Displaying conversation logs for job: {}", job.jobId());
        fetchAndDisplayConversation(job.jobId(), job.cursorAgentId());
    }

    /**
     * Fetches and displays the conversation for a cursor agent.
     *
     * @param jobId the job ID (for display purposes)
     * @param cursorAgentId the cursor agent ID
     */
    private void fetchAndDisplayConversation(String jobId, String cursorAgentId) {
        try {
            System.out.println("\n=== Cursor Agent Conversation (Job: " + jobId + ") ===");
            var conversation = cliAgent.getConversation(cursorAgentId);
            if (conversation != null && conversation.messages() != null) {
                logger.debug("Retrieved {} conversation messages for job {}", conversation.messages().size(), jobId);
                for (ConversationMessage message : conversation.messages()) {
                    System.out.println("[conversation] " + message.text());
                }
            } else {
                logger.debug("No conversation messages available for job {}", jobId);
                System.out.println("No conversation messages available");
            }
        } catch (Exception e) {
            logger.error("Failed to fetch conversation for cursorAgentId {} (job {}): {}",
                cursorAgentId, jobId, e.getMessage(), e);
            System.out.println("Failed to fetch conversation: " + e.getMessage());
        }
    }
}

