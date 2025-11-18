package info.jab.cursor.client;

import info.jab.cursor.client.model.ApiKeyInfo;
import java.util.List;

/**
 * Interface for general endpoints operations.
 *
 * @see <a href="https://docs.cursor.com/en/background-agent/api/api-key-info">API Key Info API</a>
 * @see <a href="https://docs.cursor.com/en/background-agent/api/list-models">List Models API</a>
 * @see <a href="https://docs.cursor.com/en/background-agent/api/list-repositories">List Repositories API</a>
 *
 * @see <a href="https://cursor.com/docs/background-agent/api/endpoints">Cursor Background Agent API Endpoints</a>
 * @see <a href="https://cursor.com/docs/background-agent/api/overview">Cursor Background Agent API Overview</a>
 */
public interface CursorAgentGeneralEndpoints {

    /**
     * Gets the API key used by this agent.
     *
     * @return The API key
     */
    ApiKeyInfo getApiKeyInfo();

    /**
     * Gets the models available for this agent.
     *
     * @return The models
     */
    List<String> getModels();

    /**
     * Gets the repositories available for this agent.
     *
     * @return The repositories as a list of URLs
     */
    List<String> getRepositories();
}
