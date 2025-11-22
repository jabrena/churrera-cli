package info.jab.cursor.client;

import info.jab.cursor.client.impl.CursorAgentGeneralEndpointsImpl;
import info.jab.cursor.client.model.ApiKeyInfo;
import info.jab.cursor.generated.client.ApiClient;
import info.jab.cursor.generated.client.api.DefaultApi;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WireMock integration test for CursorAgentGeneralEndpoints interface
 */
@DisplayName("CursorAgentGeneralEndpoints Integration Tests")
class CursorAgentGeneralEndpointsIT {

    private WireMockServer wireMockServer;
    private CursorAgentGeneralEndpoints cursorAgentGeneralEndpoints;
    private static final String TEST_API_KEY = "test-api-key";
    private static final String WIREMOCK_BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080));
        wireMockServer.start();

        // Configure WireMock
        WireMock.configureFor("localhost", 8080);

        // Create CursorAgentGeneralEndpoints instance pointing to WireMock server
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(WIREMOCK_BASE_URL);
        DefaultApi defaultApi = new DefaultApi(apiClient);
        cursorAgentGeneralEndpoints = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Nested
    @DisplayName("Get API Key Info Tests")
    class GetApiKeyInfoTests {

        @Test
        @DisplayName("Should get API key info successfully when authorized")
        void should_getApiKeyInfoSuccessfully_when_authorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/me"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-general/api-key-info-200-ok.json")));

            // When
            ApiKeyInfo response = cursorAgentGeneralEndpoints.getApiKeyInfo();

            // Then
            assertThat(response)
                .isNotNull()
                .satisfies(apiKeyInfo -> {
                    assertThat(apiKeyInfo.apiKeyName()).isEqualTo("API_KEY_TOKEN_V2");
                    assertThat(apiKeyInfo.userEmail()).isEqualTo("bren@juanantonio.info");
                    assertThat(apiKeyInfo.createdAt()).isNotNull();
                });

            verify(getRequestedFor(urlEqualTo("/v0/me"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY)));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void should_throwException_when_unauthorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/me"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-general/api-key-info-401-unauthorized.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentGeneralEndpoints.getApiKeyInfo())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get API key info");

            verify(getRequestedFor(urlEqualTo("/v0/me")));
        }
    }

    @Nested
    @DisplayName("List Models Tests")
    class ListModelsTests {

        @Test
        @DisplayName("Should list models successfully when authorized")
        void should_listModelsSuccessfully_when_authorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/models"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-general/models-list-200-ok.json")));

            // When
            List<String> response = cursorAgentGeneralEndpoints.getModels();

            // Then
            assertThat(response)
                .isNotNull()
                .hasSize(5)
                .containsExactly(
                    "claude-4.5-sonnet-thinking",
                    "default",
                    "gpt-5",
                    "gpt-5-high",
                    "code-supernova-1-million"
                );

            verify(getRequestedFor(urlEqualTo("/v0/models"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY)));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void should_throwException_when_unauthorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/models"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-general/models-list-401-unauthorized.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentGeneralEndpoints.getModels())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get models");

            verify(getRequestedFor(urlEqualTo("/v0/models")));
        }
    }

    @Nested
    @DisplayName("List Repositories Tests")
    class ListRepositoriesTests {

        @Test
        @DisplayName("Should list repositories successfully when authorized")
        void should_listRepositoriesSuccessfully_when_authorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/repositories"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-general/repositories-list-200-ok.json")));

            // When
            List<String> response = cursorAgentGeneralEndpoints.getRepositories();

            // Then
            assertThat(response)
                .isNotNull()
                .hasSize(18)
                .contains("https://github.com/jabrena/churrera");

            // Verify the first repository URL
            assertThat(response.get(0))
                .isEqualTo("https://github.com/jabrena/101-cursor");

            verify(getRequestedFor(urlEqualTo("/v0/repositories"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY)));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void should_throwException_when_unauthorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/repositories"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-general/repositories-list-401-unauthorized.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentGeneralEndpoints.getRepositories())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get repositories");

            verify(getRequestedFor(urlEqualTo("/v0/repositories")));
        }
    }
}

