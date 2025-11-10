package info.jab.cursor;

import info.jab.cursor.client.model.DeleteAgentResponse;
import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.ApiClient;
import info.jab.cursor.client.api.AgentManagementApi;
import info.jab.cursor.client.model.LaunchAgentRequest;
import info.jab.cursor.client.model.Prompt;
import info.jab.cursor.client.model.Source;
import info.jab.cursor.client.model.TargetRequest;
import info.jab.cursor.client.model.FollowUpRequest;
import info.jab.cursor.client.model.FollowUpResponse;
import info.jab.cursor.client.ApiException;

import java.net.URI;
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
    private final AgentManagementApi agentManagementApi;

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
        this.agentManagementApi = new AgentManagementApi(apiClient);
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
        LaunchAgentRequest request = createLaunchAgentRequest(prompt, model, repository, pr);

        // Launch the agent
        try {
            logger.debug("Launching agent with model: {}, repository: {}", model, repository);
            AgentResponse result = agentManagementApi.launchAgent(request, getAuthHeaders());
            logger.debug("Successfully launched agent: {}", result.getId());
            return result;
        } catch (ApiException e) {
            logger.error("Failed to launch agent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to launch agent: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a LaunchAgentRequest with the specified parameters.
     *
     * @param prompt     The prompt/instructions for the agent to execute
     * @param model      The LLM model to use
     * @param repository The repository URL where the agent should work
     * @param pr         Whether to automatically create a pull request when the agent completes
     * @return LaunchAgentRequest instance configured with the provided parameters
     */
    private LaunchAgentRequest createLaunchAgentRequest(String prompt, String model, String repository, boolean pr) {
        // Create the prompt
        Prompt promptObj = new Prompt();
        promptObj.setText(prompt);

        // Create the source (repository and branch)
        Source source = new Source();
        source.setRepository(URI.create(repository));
        source.setRef(DEFAULT_BRANCH);

        // Create the target configuration (optional)
        TargetRequest target = new TargetRequest();
        target.setAutoCreatePr(pr);

        // Create the launch request
        LaunchAgentRequest request = new LaunchAgentRequest();
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
        Prompt promptObj = new Prompt();
        promptObj.setText(prompt);

        // Create the follow-up request
        FollowUpRequest request = new FollowUpRequest();
        request.setPrompt(promptObj);

        // Follow-up the agent
        try {
            logger.debug("Following up agent: {}", agentId);
            FollowUpResponse response = agentManagementApi.addFollowUp(agentId, request, getAuthHeaders());
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
            DeleteAgentResponse response = agentManagementApi.deleteAgent(agentId, getAuthHeaders());
            logger.debug("Successfully deleted agent: {} with response: {}", agentId, response.getId());
            return response;
        } catch (ApiException e) {
            logger.error("Failed to delete agent {}: {}", agentId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete agent: " + e.getMessage(), e);
        }
    }
}
