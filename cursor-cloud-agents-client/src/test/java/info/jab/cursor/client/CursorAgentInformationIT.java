package info.jab.cursor.client;

import info.jab.cursor.client.impl.CursorAgentInformationImpl;
import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.AgentStatus;
import info.jab.cursor.client.model.AgentsList;
import info.jab.cursor.client.model.ConversationResponse;
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
 * WireMock integration test for CursorAgentInformation interface
 */
@DisplayName("CursorAgentInformation Integration Tests")
class CursorAgentInformationIT {

    private WireMockServer wireMockServer;
    private CursorAgentInformation cursorAgentInformation;
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

        // Create CursorAgentInformation instance pointing to WireMock server
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(WIREMOCK_BASE_URL);
        DefaultApi defaultApi = new DefaultApi(apiClient);
        cursorAgentInformation = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Nested
    @DisplayName("List Agents Tests")
    class ListAgentsTests {

        @Test
        @DisplayName("Should list agents successfully when authorized")
        void should_listAgentsSuccessfully_when_authorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/agents"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-information/agents-list-200-ok.json")));

            // When
            AgentsList response = cursorAgentInformation.getAgents(null, null);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.agents()).hasSize(1);
            assertThat(response.nextCursor()).isEqualTo("bc-359da03f-3712-490c-9e32-025bef880af7");

            AgentResponse agent = response.agents().get(0);
            assertThat(agent.id()).isEqualTo(TEST_AGENT_ID);
            assertThat(agent.name()).isEqualTo("Add installation instructions to readme");
            assertThat(agent.status()).isEqualTo(AgentStatus.FINISHED);
            assertThat(agent.source().repository().toString()).isEqualTo("https://github.com/jabrena/churrera");
            assertThat(agent.source().ref()).isEqualTo("main");

            verify(getRequestedFor(urlEqualTo("/v0/agents"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY)));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void should_throwException_when_unauthorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/agents"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-information/agents-list-401-unauthorized.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getAgents(null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get agents");

            verify(getRequestedFor(urlEqualTo("/v0/agents")));
        }
    }

    @Nested
    @DisplayName("Get Agent Status Tests")
    class GetAgentStatusTests {

        @Test
        @DisplayName("Should get agent status successfully when valid ID provided")
        void should_getAgentStatusSuccessfully_when_validIdProvided() {
            // Given
            stubFor(get(urlEqualTo("/v0/agents/" + TEST_AGENT_ID))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-information/agent-status-200-ok.json")));

            // When
            AgentResponse response = cursorAgentInformation.getStatus(TEST_AGENT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(TEST_AGENT_ID);
            assertThat(response.name()).isEqualTo("Add installation instructions to readme");
            assertThat(response.source().repository().toString()).isEqualTo("https://github.com/jabrena/churrera");
            assertThat(response.source().ref()).isEqualTo("main");
            assertThat(response.target().branchName()).isEqualTo("cursor/add-installation-instructions-to-readme-2487");
            assertThat(response.createdAt()).isNotNull();

            verify(getRequestedFor(urlEqualTo("/v0/agents/" + TEST_AGENT_ID))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY)));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void should_throwException_when_unauthorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/agents/" + TEST_AGENT_ID))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-information/agent-status-401-unauthorized.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getStatus(TEST_AGENT_ID))
                .isInstanceOf(RuntimeException.class);

            verify(getRequestedFor(urlEqualTo("/v0/agents/" + TEST_AGENT_ID)));
        }

        @Test
        @DisplayName("Should throw exception when agent not found")
        void should_throwException_when_agentNotFound() {
            // Given
            String nonExistentAgentId = "bc-nonexistent";

            stubFor(get(urlEqualTo("/v0/agents/" + nonExistentAgentId))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-information/agent-status-404-not-found.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getStatus(nonExistentAgentId))
                .isInstanceOf(RuntimeException.class);

            verify(getRequestedFor(urlEqualTo("/v0/agents/" + nonExistentAgentId)));
        }
    }

    @Nested
    @DisplayName("Get Agent Conversation Tests")
    class GetAgentConversationTests {

        @Test
        @DisplayName("Should get agent conversation successfully when valid ID provided")
        void should_getAgentConversationSuccessfully_when_validIdProvided() {
            // Given
            stubFor(get(urlEqualTo("/v0/agents/" + TEST_AGENT_ID + "/conversation"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-information/agent-conversation-200-ok.json")));

            // When
            ConversationResponse response = cursorAgentInformation.getAgentConversation(TEST_AGENT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(TEST_AGENT_ID);
            assertThat(response.messages()).isNotNull();
            assertThat(response.messages()).hasSize(4);
            assertThat(response.messages().get(0).type()).isEqualTo("user_message");
            assertThat(response.messages().get(0).text()).isEqualTo("Add installation instructions to readme");

            verify(getRequestedFor(urlEqualTo("/v0/agents/" + TEST_AGENT_ID + "/conversation"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY)));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized")
        void should_throwException_when_unauthorized() {
            // Given
            stubFor(get(urlEqualTo("/v0/agents/" + TEST_AGENT_ID + "/conversation"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-information/agent-conversation-401-unauthorized.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getAgentConversation(TEST_AGENT_ID))
                .isInstanceOf(RuntimeException.class);

            verify(getRequestedFor(urlEqualTo("/v0/agents/" + TEST_AGENT_ID + "/conversation")));
        }

        @Test
        @DisplayName("Should throw exception when agent not found")
        void should_throwException_when_agentNotFound() {
            // Given
            String nonExistentAgentId = "bc-nonexistent";

            stubFor(get(urlEqualTo("/v0/agents/" + nonExistentAgentId + "/conversation"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("cursor-agent-information/agent-conversation-404-not-found.json")));

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getAgentConversation(nonExistentAgentId))
                .isInstanceOf(RuntimeException.class);

            verify(getRequestedFor(urlEqualTo("/v0/agents/" + nonExistentAgentId + "/conversation")));
        }
    }
}

