package info.jab.cursor.client.impl;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.DeleteAgentResponse;
import info.jab.cursor.client.model.FollowUpResponse;
import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.model.CreateAgent201Response;
import info.jab.cursor.generated.client.model.DeleteAgent200Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CursorAgentManagementImpl class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CursorAgentManagementImpl Tests")
class CursorAgentManagementImplTest {

    @Mock
    private DefaultApi defaultApi;

    private static final String TEST_API_KEY = "test-api-key";

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when apiKey is null")
        void should_throwIllegalArgumentException_when_apiKeyIsNull() {
            // When & Then
            assertThatThrownBy(() -> new CursorAgentManagementImpl(null, defaultApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when apiKey is empty")
        void should_throwIllegalArgumentException_when_apiKeyIsEmpty() {
            // When & Then
            assertThatThrownBy(() -> new CursorAgentManagementImpl("", defaultApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when apiKey is blank")
        void should_throwIllegalArgumentException_when_apiKeyIsBlank() {
            // When & Then
            assertThatThrownBy(() -> new CursorAgentManagementImpl("   ", defaultApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when defaultApi is null")
        void should_throwIllegalArgumentException_when_defaultApiIsNull() {
            // When & Then
            assertThatThrownBy(() -> new CursorAgentManagementImpl(TEST_API_KEY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DefaultApi cannot be null");
        }
    }

    @Nested
    @DisplayName("launch() Tests")
    class LaunchTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is null")
        void should_throwIllegalArgumentException_when_promptIsNull() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch(null, "model", "repo", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is empty")
        void should_throwIllegalArgumentException_when_promptIsEmpty() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch("", "model", "repo", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is blank")
        void should_throwIllegalArgumentException_when_promptIsBlank() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch("   ", "model", "repo", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when model is null")
        void should_throwIllegalArgumentException_when_modelIsNull() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", null, "repo", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when model is empty")
        void should_throwIllegalArgumentException_when_modelIsEmpty() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", "", "repo", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when model is blank")
        void should_throwIllegalArgumentException_when_modelIsBlank() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", "   ", "repo", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when repository is null")
        void should_throwIllegalArgumentException_when_repositoryIsNull() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", "model", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when repository is empty")
        void should_throwIllegalArgumentException_when_repositoryIsEmpty() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", "model", "", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when repository is blank")
        void should_throwIllegalArgumentException_when_repositoryIsBlank() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", "model", "   ", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when pr is null")
        void should_throwIllegalArgumentException_when_prIsNull() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", "model", "repo", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PR cannot be null");
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);
            ApiException apiException = new ApiException(500, "Internal Server Error");
            when(defaultApi.createAgent(any(), any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", "model", "repo", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to launch agent")
                .hasCause(apiException);
        }
    }

    @Nested
    @DisplayName("followUp() Tests")
    class FollowUpTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is null")
        void should_throwIllegalArgumentException_when_agentIdIsNull() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.followUp(null, "prompt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is empty")
        void should_throwIllegalArgumentException_when_agentIdIsEmpty() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.followUp("", "prompt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is blank")
        void should_throwIllegalArgumentException_when_agentIdIsBlank() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.followUp("   ", "prompt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is null")
        void should_throwIllegalArgumentException_when_promptIsNull() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.followUp("agent-id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is empty")
        void should_throwIllegalArgumentException_when_promptIsEmpty() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.followUp("agent-id", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is blank")
        void should_throwIllegalArgumentException_when_promptIsBlank() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.followUp("agent-id", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);
            ApiException apiException = new ApiException(500, "Internal Server Error");
            when(defaultApi.addFollowup(anyString(), any(), any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(() -> impl.followUp("agent-id", "prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to follow up agent")
                .hasCause(apiException);
        }
    }

    @Nested
    @DisplayName("delete() Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is null")
        void should_throwIllegalArgumentException_when_agentIdIsNull() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.delete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is empty")
        void should_throwIllegalArgumentException_when_agentIdIsEmpty() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.delete(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is blank")
        void should_throwIllegalArgumentException_when_agentIdIsBlank() {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.delete("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
            CursorAgentManagementImpl impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);
            ApiException apiException = new ApiException(500, "Internal Server Error");
            when(defaultApi.deleteAgent(anyString(), any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(() -> impl.delete("agent-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete agent")
                .hasCause(apiException);
        }
    }
}

