package info.jab.cursor;

import info.jab.cursor.client.ApiClient;
import info.jab.cursor.client.api.GeneralEndpointsApi;
import info.jab.cursor.client.model.ApiKeyInfo;
import info.jab.cursor.client.model.RepositoriesList;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the CursorAgentGeneralEndpoints interface that provides general endpoints operations.
 * This class encapsulates the complexity of creating API requests for general endpoints
 * and provides a clean interface for retrieving API information, models, and repositories.
 */
public class CursorAgentGeneralEndpointsImpl implements CursorAgentGeneralEndpoints {

    private static final Logger logger = LoggerFactory.getLogger(CursorAgentGeneralEndpointsImpl.class);

    private final String apiKey;
    private final GeneralEndpointsApi generalEndpointsApi;

    /**
     * Creates a new CursorAgentGeneralEndpointsImpl with the specified API key and base URL.
     *
     * @param apiKey The API key for authentication with Cursor API
     * @param apiBaseUrl The base URL for the Cursor API
     */
    public CursorAgentGeneralEndpointsImpl(String apiKey, String apiBaseUrl) {
        this.apiKey = apiKey;

        // Initialize API client
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(apiBaseUrl);
        this.generalEndpointsApi = new GeneralEndpointsApi(apiClient);
    }

    @Override
    public ApiKeyInfo getApiKeyInfo() {
        // Prepare authentication headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);

        try {
           return generalEndpointsApi.getApiKeyInfo(headers);
        } catch (Exception e) {
            //info.jab.cursor.client.ApiException;
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getModels() {

        // Prepare authentication headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);

        try {
            return generalEndpointsApi.listModels(headers).getModels();
        } catch (Exception e) {
            //info.jab.cursor.client.ApiException;
            throw new RuntimeException(e);
        }
    }

    @Override
    public RepositoriesList getRepositories() {

        // Prepare authentication headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);

        try {
            return generalEndpointsApi.listRepositories(headers);
        } catch (Exception e) {
            //info.jab.cursor.client.ApiException;
            throw new RuntimeException(e);
        }
    }
}
