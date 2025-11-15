package info.jab.cursor.client;

import info.jab.cursor.client.impl.CursorAgentInformationImpl;
import info.jab.cursor.generated.client.model.AgentResponse;
import info.jab.cursor.generated.client.model.AgentsList;
import info.jab.cursor.generated.client.model.ConversationResponse;
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
        cursorAgentInformation = new CursorAgentInformationImpl(TEST_API_KEY, WIREMOCK_BASE_URL);
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
            assertThat(response.getAgents()).hasSize(1);
            assertThat(response.getNextCursor()).isEqualTo("bc-359da03f-3712-490c-9e32-025bef880af7");

            AgentResponse agent = response.getAgents().get(0);
            assertThat(agent.getId()).isEqualTo(TEST_AGENT_ID);
            assertThat(agent.getName()).isEqualTo("Add installation instructions to readme");
            assertThat(agent.getStatus()).isEqualTo(AgentResponse.StatusEnum.COMPLETED);
            assertThat(agent.getSource().getRepository().toString()).isEqualTo("https://github.com/jabrena/churrera");
            assertThat(agent.getSource().getRef()).isEqualTo("main");

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
            assertThat(response.getId()).isEqualTo(TEST_AGENT_ID);
            assertThat(response.getName()).isEqualTo("Add installation instructions to readme");
            assertThat(response.getStatus()).isEqualTo(AgentResponse.StatusEnum.COMPLETED);
            assertThat(response.getSource().getRepository().toString()).isEqualTo("https://github.com/jabrena/churrera");
            assertThat(response.getSource().getRef()).isEqualTo("main");
            assertThat(response.getTarget().getBranchName()).isEqualTo("cursor/add-installation-instructions-to-readme-2487");
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getUpdatedAt()).isNotNull();

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
            assertThat(response.getId()).isEqualTo(TEST_AGENT_ID);
            assertThat(response.getMessages()).isNotNull();
            assertThat(response.getMessages()).hasSize(4);
            assertThat(response.getMessages().get(0).getType()).isEqualTo("user_message");
            assertThat(response.getMessages().get(0).getText()).isEqualTo("Add installation instructions to readme");

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

