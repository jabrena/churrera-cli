package info.jab.cursor.client.impl;

import info.jab.cursor.client.CursorAgentManagement;
import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.DeleteAgentResponse;
import info.jab.cursor.client.model.FollowUpResponse;
import info.jab.cursor.generated.client.ApiClient;
import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.model.CreateAgentRequest;
import info.jab.cursor.generated.client.model.CreateAgentRequestPrompt;
import info.jab.cursor.generated.client.model.CreateAgentRequestSource;
import info.jab.cursor.generated.client.model.CreateAgentRequestTarget;
import info.jab.cursor.generated.client.model.AddFollowupRequest;
import info.jab.cursor.generated.client.ApiException;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the CursorAgentManagement interface that provides agent management operations.
 * This class encapsulates the complexity of creating API requests for agent management
 * and provides a clean interface for launching, following up, and deleting agents.
 */
public class CursorAgentManagementImpl implements CursorAgentManagement {

    private static final Logger logger = LoggerFactory.getLogger(CursorAgentManagementImpl.class);

    private static final String DEFAULT_BRANCH = "main";

    private final String apiKey;
    private final DefaultApi defaultApi;

    /**
     * Creates a new CursorAgentManagementImpl with the specified API key and base URL.
     *
     * @param apiKey The API key for authentication with Cursor API
     * @param apiBaseUrl The base URL for the Cursor API
     */
    public CursorAgentManagementImpl(String apiKey, String apiBaseUrl) {
        this.apiKey = apiKey;

        // Initialize API client
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(apiBaseUrl);
        this.defaultApi = new DefaultApi(apiClient);
    }

    /**
     * Constructor for testing purposes.
     * Allows injection of a mock DefaultApi for unit testing.
     * This constructor is public to allow tests in different packages to use it.
     *
     * @param apiKey The API key for authentication with Cursor API
     * @param defaultApi The DefaultApi instance to use (typically a mock in tests)
     */
    public CursorAgentManagementImpl(String apiKey, DefaultApi defaultApi) {
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
     * Launches a Cursor agent with the specified parameters.
     *
     * @param prompt The prompt/instructions for the agent to execute
     * @param model The LLM model to use (e.g., "claude-4-sonnet")
     * @param repository The repository URL where the agent should work
     * @param pr Whether to automatically create a pull request when the agent completes
     * @return Agent instance representing the launched agent
     */
    @Override
    public AgentResponse launch(String prompt, String model, String repository, boolean pr) {
        // Preconditions
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be null or empty");
        }
        if (repository == null || repository.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository cannot be null or empty");
        }

        // Create the launch request
        CreateAgentRequest request = createLaunchAgentRequest(prompt, model, repository, pr);

        // Launch the agent
        try {
            logger.debug("Launching agent with model: {}, repository: {}", model, repository);
            AgentResponse result = AgentResponse.from(defaultApi.createAgent(request, getAuthHeaders()));
            logger.debug("Successfully launched agent: {}", result.id());
            return result;
        } catch (ApiException e) {
            logger.error("Failed to launch agent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to launch agent: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a CreateAgentRequest with the specified parameters.
     *
     * @param prompt     The prompt/instructions for the agent to execute
     * @param model      The LLM model to use
     * @param repository The repository URL where the agent should work
     * @param pr         Whether to automatically create a pull request when the agent completes
     * @return CreateAgentRequest instance configured with the provided parameters
     */
    private CreateAgentRequest createLaunchAgentRequest(String prompt, String model, String repository, boolean pr) {
        // Create the prompt
        CreateAgentRequestPrompt promptObj = new CreateAgentRequestPrompt();
        promptObj.setText(prompt);

        // Create the source (repository and branch)
        CreateAgentRequestSource source = new CreateAgentRequestSource();
        source.setRepository(repository);
        source.setRef(DEFAULT_BRANCH);

        // Create the target configuration (optional)
        CreateAgentRequestTarget target = new CreateAgentRequestTarget();
        target.setAutoCreatePr(pr);

        // Create the launch request
        CreateAgentRequest request = new CreateAgentRequest();
        request.setPrompt(promptObj);
        request.setSource(source);
        request.setModel(model);
        request.setTarget(target);

        return request;
    }

    @Override
    public FollowUpResponse followUp(String agentId, String prompt) {
        // Preconditions
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        // Create the prompt
        info.jab.cursor.generated.client.model.AddFollowupRequestPrompt promptObj = new info.jab.cursor.generated.client.model.AddFollowupRequestPrompt();
        promptObj.setText(prompt);

        // Create the follow-up request
        AddFollowupRequest request = new AddFollowupRequest();
        request.setPrompt(promptObj);

        // Follow-up the agent
        try {
            logger.debug("Following up agent: {}", agentId);
            FollowUpResponse response = FollowUpResponse.from(defaultApi.addFollowup(agentId, request, getAuthHeaders()));
            logger.debug("Successfully followed up agent: {}", agentId);
            return response;
        } catch (ApiException e) {
            logger.error("Failed to follow up agent: {}", agentId, e.getMessage(), e);
            throw new RuntimeException("Failed to follow up agent " + agentId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public DeleteAgentResponse delete(String agentId) {
        // Preconditions
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        logger.debug("Deleting agent: {}", agentId);

        try {
            logger.debug("Deleting agent: {}", agentId);
            DeleteAgentResponse response = DeleteAgentResponse.from(defaultApi.deleteAgent(agentId, getAuthHeaders()));
            logger.debug("Successfully deleted agent: {} with response: {}", agentId, response.id());
            return response;
        } catch (ApiException e) {
            logger.error("Failed to delete agent {}: {}", agentId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete agent: " + e.getMessage(), e);
        }
    }
}

