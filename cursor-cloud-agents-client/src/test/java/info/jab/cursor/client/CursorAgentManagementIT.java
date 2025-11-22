package info.jab.cursor.client;

import info.jab.cursor.client.impl.CursorAgentManagementImpl;
import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.AgentStatus;
import info.jab.cursor.client.model.DeleteAgentResponse;
import info.jab.cursor.client.model.FollowUpResponse;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WireMock integration test for CursorAgentManagement interface
 */
@DisplayName("CursorAgentManagement Integration Tests")
class CursorAgentManagementIT {

    private WireMockServer wireMockServer;
    private CursorAgentManagement cursorAgentManagement;
    private static final String TEST_AGENT_ID = "bc-test-agent-12345";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String WIREMOCK_BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080));
        wireMockServer.start();

        // Configure WireMock
        WireMock.configureFor("localhost", 8080);

        // Create CursorAgentManagement instance pointing to WireMock server
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(WIREMOCK_BASE_URL);
        DefaultApi defaultApi = new DefaultApi(apiClient);
        cursorAgentManagement = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Nested
    @DisplayName("Launch Agent Tests")
    class LaunchAgentTests {

        @Test
        @DisplayName("Should launch agent successfully when valid request provided")
        void should_launchAgentSuccessfully_when_validRequestProvided() {
            // Given
            String prompt = "Add installation instructions to readme";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "https://github.com/jabrena/churrera";

            stubFor(post(urlEqualTo("/v0/agents"))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-launch-201-ok.json")));

            // When
            AgentResponse response = cursorAgentManagement.launch(prompt, model, repository, true);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(TEST_AGENT_ID);
            assertThat(response.name()).isEqualTo("Add installation instructions to readme");
            assertThat(response.status()).isEqualTo(AgentStatus.CREATING);
            assertThat(response.source().repository().toString()).isEqualTo(repository);
            assertThat(response.source().ref()).isEqualTo("main");
            assertThat(response.target().branchName()).isEqualTo("cursor/add-installation-instructions-to-readme-2487");

            verify(postRequestedFor(urlEqualTo("/v0/agents"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withHeader("Content-Type", equalTo("application/json")));
        }

        @Test
        @DisplayName("Should throw exception when model validation error occurs")
        void should_throwException_when_modelValidationErrorOccurs() {
            // Given
            String prompt = "Add installation instructions to readme";
            String model = "invalid-model-that-does-not-exist";
            String repository = "https://github.com/jabrena/churrera";

            stubFor(post(urlEqualTo("/v0/agents"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-launch-400-model-validation-error.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.generated.client.ApiException.class);

            verify(postRequestedFor(urlEqualTo("/v0/agents")));
        }

        @Test
        @DisplayName("Should throw exception when repository validation error occurs")
        void should_throwException_when_repositoryValidationErrorOccurs() {
            // Given
            String prompt = "Add installation instructions to readme";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "https://github.com/invalid-org/invalid-repo";

            stubFor(post(urlEqualTo("/v0/agents"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-launch-400-repository-validation-error.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.generated.client.ApiException.class);

            verify(postRequestedFor(urlEqualTo("/v0/agents")));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void should_throwException_when_unauthorized() {
            // Given
            String prompt = "Add installation instructions to readme";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "https://github.com/jabrena/churrera";

            stubFor(post(urlEqualTo("/v0/agents"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-launch-401-unauthorized.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.generated.client.ApiException.class);

            verify(postRequestedFor(urlEqualTo("/v0/agents")));
        }
    }

    @Nested
    @DisplayName("Follow-up Tests")
    class FollowUpTests {

        @Test
        @DisplayName("Should add follow-up successfully when valid request provided")
        void should_addFollowUpSuccessfully_when_validRequestProvided() {
            // Given
            String prompt = "Also add unit tests for the new functionality";

            stubFor(post(urlEqualTo("/v0/agents/" + TEST_AGENT_ID + "/followup"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-followup-200-ok.json")));

            // When
            FollowUpResponse response = cursorAgentManagement.followUp(TEST_AGENT_ID, prompt);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(TEST_AGENT_ID);

            verify(postRequestedFor(urlEqualTo("/v0/agents/" + TEST_AGENT_ID + "/followup"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY)));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void should_throwException_when_unauthorized() {
            // Given
            String prompt = "Also add unit tests";

            stubFor(post(urlEqualTo("/v0/agents/" + TEST_AGENT_ID + "/followup"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-followup-401-unauthorized.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.followUp(TEST_AGENT_ID, prompt))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.generated.client.ApiException.class);

            verify(postRequestedFor(urlEqualTo("/v0/agents/" + TEST_AGENT_ID + "/followup")));
        }

        @Test
        @DisplayName("Should throw exception when agent not found")
        void should_throwException_when_agentNotFound() {
            // Given
            String prompt = "Also add unit tests";
            String nonExistentAgentId = "bc-nonexistent";

            stubFor(post(urlEqualTo("/v0/agents/" + nonExistentAgentId + "/followup"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-followup-404-not-found.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.followUp(nonExistentAgentId, prompt))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.generated.client.ApiException.class);

            verify(postRequestedFor(urlEqualTo("/v0/agents/" + nonExistentAgentId + "/followup")));
        }
    }

    @Nested
    @DisplayName("Delete Agent Tests")
    class DeleteAgentTests {

        @Test
        @DisplayName("Should delete agent successfully when valid ID provided")
        void should_deleteAgentSuccessfully_when_validIdProvided() {
            // Given
            stubFor(delete(urlEqualTo("/v0/agents/" + TEST_AGENT_ID))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-delete-200-ok.json")));

            // When
            DeleteAgentResponse response = cursorAgentManagement.delete(TEST_AGENT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(TEST_AGENT_ID);

            verify(deleteRequestedFor(urlEqualTo("/v0/agents/" + TEST_AGENT_ID))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY)));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void should_throwException_when_unauthorized() {
            // Given
            stubFor(delete(urlEqualTo("/v0/agents/" + TEST_AGENT_ID))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-delete-401-unauthorized.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.delete(TEST_AGENT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.generated.client.ApiException.class)
                .hasMessageContaining("Failed to delete agent");

            verify(deleteRequestedFor(urlEqualTo("/v0/agents/" + TEST_AGENT_ID)));
        }

        @Test
        @DisplayName("Should throw exception when agent not found")
        void should_throwException_when_agentNotFound() {
            // Given
            String nonExistentAgentId = "bc-nonexistent";

            stubFor(delete(urlEqualTo("/v0/agents/" + nonExistentAgentId))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-management/agent-delete-404-not-found.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.delete(nonExistentAgentId))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.generated.client.ApiException.class)
                .hasMessageContaining("Failed to delete agent");

            verify(deleteRequestedFor(urlEqualTo("/v0/agents/" + nonExistentAgentId)));
        }
    }
}

