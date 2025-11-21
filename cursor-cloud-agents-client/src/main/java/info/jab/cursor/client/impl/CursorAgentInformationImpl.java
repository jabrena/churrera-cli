package info.jab.cursor.client.impl;

import info.jab.cursor.client.CursorAgentInformation;
import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.AgentsList;
import info.jab.cursor.client.model.ConversationResponse;
import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.ApiException;

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
    private final DefaultApi defaultApi;

    /**
     * Creates a new CursorAgentInformationImpl with the specified API key and DefaultApi.
     * This constructor allows dependency injection for better testability and flexibility.
     *
     * @param apiKey The API key for authentication with Cursor API
     * @param defaultApi The DefaultApi instance to use (can be a mock in tests or real instance)
     */
    public CursorAgentInformationImpl(String apiKey, DefaultApi defaultApi) {
        // Preconditions
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        if (defaultApi == null) {
            throw new IllegalArgumentException("DefaultApi cannot be null");
        }

        this.apiKey = apiKey;
        this.defaultApi = defaultApi;
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
        // Preconditions
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        if (cursor != null && cursor.trim().isEmpty()) {
            throw new IllegalArgumentException("Cursor cannot be empty");
        }

        try {
            return AgentsList.from(defaultApi.listAgents(limit, cursor, getAuthHeaders()));
        } catch (ApiException e) {
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
        // Preconditions
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            // Get current agent status - single API call
            return AgentResponse.from(defaultApi.getAgent(agentId, getAuthHeaders()));
        } catch (Exception statusException) {
            // If status parsing fails due to unknown enum value, try to handle gracefully
            if (statusException.getMessage() != null && statusException.getMessage().contains("Unexpected value")) {
                // For now, re-throw the exception. The calling layer can handle unknown statuses
                throw new RuntimeException("Agent status contains unknown value: " + statusException.getMessage(), statusException);
            } else {
                throw new RuntimeException("Failed to get agent status for " + agentId + ": " + statusException.getMessage(), statusException);
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
        // Preconditions
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            return ConversationResponse.from(defaultApi.getAgentConversation(agentId, getAuthHeaders()));
        } catch (ApiException e) {
            throw new RuntimeException("Failed to get agent conversation for " + agentId + ": " + e.getMessage(), e);
        }
    }
}

