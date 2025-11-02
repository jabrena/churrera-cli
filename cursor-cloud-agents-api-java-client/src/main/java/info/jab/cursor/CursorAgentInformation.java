package info.jab.cursor;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.AgentsList;
import info.jab.cursor.client.model.ConversationResponse;

/**
 * Interface for agent information operations.
 *
 * @see <a href="https://docs.cursor.com/en/background-agent/api/list-agents">List Agents API</a>
 * @see <a href="https://docs.cursor.com/en/background-agent/api/agent-status">Agent Status API</a>
 * @see <a href="https://docs.cursor.com/en/background-agent/api/agent-conversation">Agent Conversation API</a>
 *
 * @see <a href="https://cursor.com/docs/background-agent/api/endpoints">Cursor Background Agent API Endpoints</a>
 * @see <a href="https://cursor.com/docs/background-agent/api/overview">Cursor Background Agent API Overview</a>
 */
public interface CursorAgentInformation {

    /**
     * Gets a list of agents with optional pagination.
     *
     * @param limit Maximum number of agents to return (optional, can be null)
     * @param cursor Pagination cursor for retrieving next page (optional, can be null)
     * @return AgentsList containing the list of agents
     */
    AgentsList getAgents(Integer limit, String cursor);

    /**
     * Gets the current status of a specific agent.
     *
     * @param agentId The ID of the agent to retrieve status for
     * @return Agent instance with current status information
     */
    AgentResponse getStatus(String agentId);

    /**
     * Gets the conversation history for a specific agent.
     *
     * @param agentId The ID of the agent to retrieve conversation for
     * @return ConversationResponse containing the agent's conversation history
     */
    ConversationResponse getAgentConversation(String agentId);

}
