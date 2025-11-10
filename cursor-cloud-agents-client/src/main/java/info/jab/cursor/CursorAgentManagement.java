package info.jab.cursor;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.FollowUpResponse;
import info.jab.cursor.client.model.DeleteAgentResponse;

/**
 * Interface for launching Cursor agents with simplified parameters.
 * This interface provides an abstraction layer for interacting with the Cursor API
 * to launch coding agents on repositories.
 *
 * @see <a href="https://docs.cursor.com/en/background-agent/api/launch-an-agent">Launch Agent API</a>
 * @see <a href="https://docs.cursor.com/en/background-agent/api/add-followup">Add Follow-up API</a>
 * @see <a href="https://docs.cursor.com/en/background-agent/api/delete-agent">Delete Agent API</a>
 *
 * @see <a href="https://cursor.com/docs/background-agent/api/endpoints">Cursor Background Agent API Endpoints</a>
 * @see <a href="https://cursor.com/docs/background-agent/api/overview">Cursor Background Agent API Overview</a>
 */
public interface CursorAgentManagement {

    /**
     * Launches a Cursor agent with the specified parameters.
     *
     * @param prompt The prompt/instructions for the agent to execute
     * @param model The LLM model to use (e.g., "claude-4-sonnet")
     * @param repository The repository URL where the agent should work
     * @param pr Whether to automatically create a pull request when the agent completes
     * @return Agent instance representing the launched agent
     */
    AgentResponse launch(String prompt, String model, String repository, boolean pr);

    /**
     * Gets the current status of an agent.
     * This method performs a single status check.
     *
     * @param agentId The ID of the agent to check
     * @return The current Agent instance with updated status
     */
    FollowUpResponse followUp(String agentId, String prompt);

    /**
     * Deletes a Cursor agent by its ID.
     *
     * @param agentId The ID of the agent to delete
     */
    DeleteAgentResponse delete(String agentId);
}
