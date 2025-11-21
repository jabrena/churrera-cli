package info.jab.churrera.cli.service;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.CursorAgentManagement;
import info.jab.cursor.client.CursorAgentInformation;
import info.jab.cursor.client.CursorAgentGeneralEndpoints;
import info.jab.cursor.client.impl.CursorAgentManagementImpl;
import info.jab.cursor.client.impl.CursorAgentInformationImpl;
import info.jab.cursor.client.model.FollowUpResponse;
import info.jab.cursor.client.model.ConversationResponse;
import info.jab.cursor.client.model.ConversationMessage;
import java.util.List;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.util.PropertyResolver;
import info.jab.churrera.util.PmlConverter;
import info.jab.churrera.workflow.ExpressionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CLI-specific agent service that provides methods for launching, monitoring,
 * and managing Cursor agents with database integration.
 */
public class CLIAgent {

    private static final Logger logger = LoggerFactory.getLogger(CLIAgent.class);

    private final CursorAgentManagement cursorAgentManagement;
    private final CursorAgentInformation cursorAgentInformation;
    private final CursorAgentGeneralEndpoints cursorAgentGeneralEndpoints;
    private final JobRepository jobRepository;
    private final PmlConverter pmlConverter;

    public CLIAgent(JobRepository jobRepository, CursorAgentManagement cursorAgentManagement, CursorAgentInformation cursorAgentInformation, CursorAgentGeneralEndpoints cursorAgentGeneralEndpoints, PmlConverter pmlConverter) {
        this.cursorAgentManagement = cursorAgentManagement;
        this.cursorAgentInformation = cursorAgentInformation;
        this.cursorAgentGeneralEndpoints = cursorAgentGeneralEndpoints;
        this.jobRepository = jobRepository;
        this.pmlConverter = pmlConverter;
    }

    /**
     * Launch a new Cursor agent for the given job and prompt content.
     *
     * @param job the job to launch
     * @param promptContent the prompt content to use as the initial prompt
     * @param type the type of prompt ("pml" or "md")
     * @param bindValue optional value to replace <input>INPUT</input> placeholder (can be null)
     * @param pr whether to automatically create a pull request when the agent completes
     * @return the Cursor agent ID
     */
    public String launchAgentForJob(Job job, String promptContent, String type, String bindValue, boolean pr) {
        try {
            logger.info("🚀 launchAgentForJob - type: {}, bindValue: {}, pr: {}", type, bindValue, pr);

            // Convert to Markdown if needed
            String markdownContent = convertToMarkdown(promptContent, type);
            logger.info("📄 After XML->Markdown conversion (length: {})", markdownContent.length());

            // Apply bind value replacement if provided
            if (bindValue != null && !bindValue.isEmpty()) {
                logger.info("🔄 Applying bind value replacement with value: '{}'", bindValue);
                String before = markdownContent;
                markdownContent = ExpressionEvaluator.replaceInputPlaceholder(markdownContent, bindValue);
                boolean changed = !before.equals(markdownContent);
                logger.info("🔄 Replacement result: content changed = {}", changed);
            } else {
                logger.info("⏭️ Skipping bind value replacement (bindValue is {})",
                    bindValue == null ? "null" : "empty");
            }

            logger.debug("Prepared prompt content for agent launch (type: {})", type);

            AgentResponse cursorAgentResult = cursorAgentManagement.launch(markdownContent, job.model(), job.repository(), pr);
            return cursorAgentResult.id();
        } catch (Exception e) {
            logger.error("Failed to launch agent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to launch agent: " + e.getMessage(), e);
        }
    }

    /**
     * Send a follow-up prompt to an existing Cursor agent.
     *
     * @param cursorAgentId the Cursor agent ID
     * @param promptContent the prompt content for the follow-up
     * @param type the type of prompt ("pml" or "md")
     * @param bindValue optional value to replace <input>INPUT</input> placeholder (can be null)
     * @return the follow-up response ID
     */
    public String followUpForPrompt(String cursorAgentId, String promptContent, String type, String bindValue) {
        try {
            logger.info("📨 followUpForPrompt - type: {}, bindValue: {}", type, bindValue);

            // Convert to Markdown if needed
            String markdownContent = convertToMarkdown(promptContent, type);
            logger.info("📄 After XML->Markdown conversion (length: {})", markdownContent.length());

            // Apply bind value replacement if provided
            if (bindValue != null && !bindValue.isEmpty()) {
                logger.info("🔄 Applying bind value replacement with value: '{}'", bindValue);
                String before = markdownContent;
                markdownContent = ExpressionEvaluator.replaceInputPlaceholder(markdownContent, bindValue);
                boolean changed = !before.equals(markdownContent);
                logger.info("🔄 Replacement result: content changed = {}", changed);
            } else {
                logger.info("⏭️ Skipping bind value replacement (bindValue is {})",
                    bindValue == null ? "null" : "empty");
            }

            logger.debug("Prepared prompt content for follow-up (type: {})", type);
            FollowUpResponse response = cursorAgentManagement.followUp(cursorAgentId, markdownContent);
            return response.id();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send follow-up for agent " + cursorAgentId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Monitor an agent's status until it reaches a terminal state.
     *
     * @param cursorAgentId the Cursor agent ID to monitor
     * @param delaySeconds the delay between status checks
     * @return the final agent state
     */
    public AgentState monitorAgent(String cursorAgentId, int delaySeconds) {
        int checkCount = 0;
        AgentState lastState = AgentState.CREATING();

        while (true) {
            try {
                checkCount++;
                AgentResponse currentAgent = cursorAgentInformation.getStatus(cursorAgentId);
                AgentState agentState = AgentState.of(currentAgent);
                lastState = agentState;

                // Check if agent has completed or failed
                if (agentState.isTerminal()) {
                    break;
                }

                // Wait for the next check
                Thread.sleep(delaySeconds * 1000L);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Monitoring interrupted", e);
            } catch (Exception e) {
                System.err.println("⚠️  Error during status check: " + e.getMessage());
                // Continue monitoring despite errors
                try {
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Monitoring interrupted", ie);
                }
            }
        }

        return lastState;
    }

    /**
     * Get the conversation history for a specific agent.
     *
     * @param cursorAgentId the Cursor agent ID
     * @return the conversation response
     */
    public ConversationResponse getConversation(String cursorAgentId) {
        try {
            return cursorAgentInformation.getAgentConversation(cursorAgentId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get agent conversation for " + cursorAgentId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Get the conversation content as a string for a specific agent.
     * This extracts all messages from the conversation and concatenates them.
     *
     * @param cursorAgentId the Cursor agent ID
     * @return the conversation content as a string
     */
    public String getConversationContent(String cursorAgentId) {
        try {
            ConversationResponse conversation = cursorAgentInformation.getAgentConversation(cursorAgentId);
            StringBuilder content = new StringBuilder();

            if (conversation.messages() != null) {
                for (ConversationMessage message : conversation.messages()) {
                    if (message.text() != null) {
                        content.append(message.text()).append("\n");
                    }
                }
            }

            return content.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get agent conversation content for " + cursorAgentId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Delete a Cursor agent.
     *
     * @param cursorAgentId the Cursor agent ID to delete
     */
    public void deleteAgent(String cursorAgentId) {
        try {
            cursorAgentManagement.delete(cursorAgentId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete agent " + cursorAgentId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Update the job in the database with the Cursor agent ID and status.
     *
     * @param job the job to update
     * @param cursorAgentId the Cursor agent ID
     * @param status the current status
     */
    public void updateJobCursorIdInDatabase(Job job, String cursorAgentId, AgentState status) {
        try {
            Job updatedJob = job.withCursorAgentId(cursorAgentId).withStatus(status);
            jobRepository.save(updatedJob);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update job in database: " + e.getMessage(), e);
        }
    }

    /**
     * Update the job status in the database.
     *
     * @param job the job to update
     * @param status the new status
     */
    public void updateJobStatusInDatabase(Job job, AgentState status) {
        try {
            Job updatedJob = job.withStatus(status);
            jobRepository.save(updatedJob);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update job status in database: " + e.getMessage(), e);
        }
    }

    /**
     * Update a prompt in the database with the given status.
     *
     * @param prompt the prompt to update
     * @param status the new status
     */
    public void updatePromptInDatabase(Prompt prompt, String status) {
        try {
            Prompt updatedPrompt = prompt.withStatus(status);
            jobRepository.savePrompt(updatedPrompt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update prompt in database: " + e.getMessage(), e);
        }
    }

    /**
     * Update a job's last update timestamp in the database.
     *
     * @param job the job to update
     */
    public void updateJobInDatabase(Job job) {
        try {
            Job updatedJob = job.withPath(job.path()); // This updates the lastUpdate timestamp
            jobRepository.save(updatedJob);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update job in database: " + e.getMessage(), e);
        }
    }

    /**
     * Get the current status of a Cursor agent.
     *
     * @param cursorAgentId the Cursor agent ID
     * @return the current agent state
     */
    public AgentState getAgentStatus(String cursorAgentId) {
        try {
            AgentResponse agent = cursorAgentInformation.getStatus(cursorAgentId);
            return AgentState.of(agent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get agent status for " + cursorAgentId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Get the list of available models from the Cursor API.
     *
     * @return list of available model names
     */
    public List<String> getModels() {
        try {
            return cursorAgentGeneralEndpoints.getModels();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get models: " + e.getMessage(), e);
        }
    }

    /**
     * Get the list of available repository URLs from the Cursor API.
     *
     * @return list of available repository URLs
     */
    public List<String> getRepositories() {
        try {
            return cursorAgentGeneralEndpoints.getRepositories();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get repositories: " + e.getMessage(), e);
        }
    }

    /**
     * Convert prompt content to Markdown based on type.
     * If type is "pml", use PmlConverter. Otherwise, return content as-is.
     *
     * @param promptContent the prompt content
     * @param type the type of prompt ("pml" or "md")
     * @return the Markdown content
     */
    private String convertToMarkdown(String promptContent, String type) {
        if ("pml".equalsIgnoreCase(type)) {
            // Convert PML to Markdown using XSLT transformation
            try {
                // Try the direct path first
                return pmlConverter.toMarkdownFromContent(promptContent, "xslt/pml-to-md.xsl");
            } catch (Exception e) {
                logger.error("Failed to convert PML to Markdown with xslt/pml-to-md.xsl: {}", e.getMessage());
                try {
                    // Fallback to the default path
                    return pmlConverter.toMarkdownFromContent(promptContent);
                } catch (Exception e2) {
                    logger.error("Failed to convert PML to Markdown with default path: {}", e2.getMessage());
                    // Fallback to original content if conversion fails
                    return promptContent;
                }
            }
        } else {
            // For non-PML types (e.g., "md"), return content as-is
            logger.debug("Using prompt content directly (type: {})", type);
            return promptContent;
        }
    }
}
