package info.jab.cursor;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.DeleteAgentResponse;
import info.jab.cursor.client.model.FollowUpResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

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
        cursorAgentManagement = new CursorAgentManagementImpl(TEST_API_KEY, WIREMOCK_BASE_URL);
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
            assertThat(response.getId()).isEqualTo(TEST_AGENT_ID);
            assertThat(response.getName()).isEqualTo("Add installation instructions to readme");
            assertThat(response.getStatus()).isEqualTo(AgentResponse.StatusEnum.CREATING);
            assertThat(response.getSource().getRepository().toString()).isEqualTo(repository);
            assertThat(response.getSource().getRef()).isEqualTo("main");
            assertThat(response.getTarget().getBranchName()).isEqualTo("cursor/add-installation-instructions-to-readme-2487");

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

            // When
            Throwable thrown = catchThrowable(() -> cursorAgentManagement.launch(prompt, model, repository, true));

            // Then
            assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.client.ApiException.class);

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

            // When
            Throwable thrown = catchThrowable(() -> cursorAgentManagement.launch(prompt, model, repository, true));

            // Then
            assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.client.ApiException.class);

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

            // When
            Throwable thrown = catchThrowable(() -> cursorAgentManagement.launch(prompt, model, repository, true));

            // Then
            assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.client.ApiException.class);

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
            assertThat(response.getId()).isEqualTo(TEST_AGENT_ID);

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

            // When
            Throwable thrown = catchThrowable(() -> cursorAgentManagement.followUp(TEST_AGENT_ID, prompt));

            // Then
            assertThat(thrown)
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(info.jab.cursor.client.ApiException.class);

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

            // When
            Throwable thrown = catchThrowable(() -> cursorAgentManagement.followUp(nonExistentAgentId, prompt));

            // Then
            assertThat(thrown)
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(info.jab.cursor.client.ApiException.class);

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
            assertThat(response.getId()).isEqualTo(TEST_AGENT_ID);

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

            // When
            Throwable thrown = catchThrowable(() -> cursorAgentManagement.delete(TEST_AGENT_ID));

            // Then
            assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.client.ApiException.class)
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

            // When
            Throwable thrown = catchThrowable(() -> cursorAgentManagement.delete(nonExistentAgentId));

            // Then
            assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(info.jab.cursor.client.ApiException.class)
                .hasMessageContaining("Failed to delete agent");

            verify(deleteRequestedFor(urlEqualTo("/v0/agents/" + nonExistentAgentId)));
        }
    }
}

