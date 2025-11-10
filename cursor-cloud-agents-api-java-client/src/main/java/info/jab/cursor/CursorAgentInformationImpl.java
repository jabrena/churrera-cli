package info.jab.cursor;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.ApiClient;
import info.jab.cursor.client.api.AgentInformationApi;
import info.jab.cursor.client.model.AgentsList;
import info.jab.cursor.client.model.ConversationResponse;
import info.jab.cursor.client.ApiException;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the CursorAgentInformation interface that provides agent information operations.
 * This class encapsulates the complexity of creating API requests for agent information
 * and provides a clean interface for retrieving agent data.
 */
public class CursorAgentInformationImpl implements CursorAgentInformation {

    private static final Logger logger = LoggerFactory.getLogger(CursorAgentInformationImpl.class);

    private final String apiKey;
    private final AgentInformationApi agentInformationApi;

    /**
     * Creates a new CursorAgentInformationImpl with the specified API key and base URL.
     *
     * @param apiKey The API key for authentication with Cursor API
     * @param apiBaseUrl The base URL for the Cursor API
     */
    public CursorAgentInformationImpl(String apiKey, String apiBaseUrl) {
        this.apiKey = apiKey;

        // Initialize API client
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(apiBaseUrl);
        this.agentInformationApi = new AgentInformationApi(apiClient);
    }

    /**
     * Creates authentication headers with the API key.
     *
     * @return Map containing the Authorization header with Bearer token
     */
    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        return headers;
    }

    /**
     * Gets a list of agents with optional pagination.
     *
     * @param limit Maximum number of agents to return (optional, can be null)
     * @param cursor Pagination cursor for retrieving next page (optional, can be null)
     * @return AgentsList containing the list of agents
     */
    @Override
    public AgentsList getAgents(Integer limit, String cursor) {
        try {
            return agentInformationApi.listAgents(limit, cursor, getAuthHeaders());
        } catch (ApiException e) {
            logger.error("Failed to get agents: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get agents: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the current status of an agent.
     * This method performs a single status check.
     *
     * @param agentId The ID of the agent to check
     * @return The current Agent instance with updated status
     */
    @Override
    public AgentResponse getStatus(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            // Get current agent status - single API call
            return agentInformationApi.getAgent(agentId, getAuthHeaders());
        } catch (Exception statusException) {
            logger.error("Failed to get agent status: {}", statusException.getMessage(), statusException);
            // If status parsing fails due to unknown enum value, try to handle gracefully
            if (statusException.getMessage() != null && statusException.getMessage().contains("Unexpected value")) {
                // For now, re-throw the exception. The calling layer can handle unknown statuses
                throw new RuntimeException("Agent status contains unknown value: " + statusException.getMessage(), statusException);
            } else {
                throw new RuntimeException(statusException);
            }
        }
    }

    /**
     * Gets the conversation history for a specific agent.
     *
     * @param agentId The ID of the agent to retrieve conversation for
     * @return ConversationResponse containing the agent's conversation history
     */
    @Override
    public ConversationResponse getAgentConversation(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            return agentInformationApi.getAgentConversation(agentId, getAuthHeaders());
        } catch (ApiException e) {
            logger.error("Failed to get agent conversation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get agent conversation: " + e.getMessage(), e);
        }
    }
}
