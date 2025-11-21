package info.jab.cursor.client.impl;

import info.jab.cursor.client.CursorAgentGeneralEndpoints;
import info.jab.cursor.client.model.ApiKeyInfo;
import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.ApiException;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

/**
 * Implementation of the CursorAgentGeneralEndpoints interface that provides general endpoints operations.
 * This class encapsulates the complexity of creating API requests for general endpoints
 * and provides a clean interface for retrieving API information, models, and repositories.
 */
public class CursorAgentGeneralEndpointsImpl implements CursorAgentGeneralEndpoints {

    private static final Logger logger = LoggerFactory.getLogger(CursorAgentGeneralEndpointsImpl.class);

    private final String apiKey;
    private final DefaultApi defaultApi;

    /**
     * Creates a new CursorAgentGeneralEndpointsImpl with the specified API key and DefaultApi.
     * This constructor allows dependency injection for better testability and flexibility.
     *
     * @param apiKey The API key for authentication with Cursor API
     * @param defaultApi The DefaultApi instance to use (can be a mock in tests or real instance)
     */
    public CursorAgentGeneralEndpointsImpl(String apiKey, DefaultApi defaultApi) {
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

    @Override
    public ApiKeyInfo getApiKeyInfo() {
        try {
           return ApiKeyInfo.from(defaultApi.getMe(getAuthHeaders()));
        } catch (ApiException e) {
            logger.error("Failed to get API key info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get API key info: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getModels() {
        try {
            return defaultApi.listModels(getAuthHeaders()).getModels();
        } catch (ApiException e) {
            logger.error("Failed to get models: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get models: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getRepositories() {
        try {
            var response = defaultApi.listRepositories(getAuthHeaders());
            if (response == null) {
                return List.of();
            }
            var repositories = response.getRepositories();
            if (repositories == null) {
                return List.of();
            }
            return repositories.stream()
                .map(repo -> repo.getRepository() != null ? repo.getRepository().toString() : null)
                .filter(Objects::nonNull)
                .toList();
        } catch (ApiException e) {
            logger.error("Failed to get repositories: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get repositories: " + e.getMessage(), e);
        }
    }
}

