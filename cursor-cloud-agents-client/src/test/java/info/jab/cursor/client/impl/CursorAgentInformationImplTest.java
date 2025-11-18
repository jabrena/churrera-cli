package info.jab.cursor.client.impl;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.AgentsList;
import info.jab.cursor.client.model.ConversationResponse;
import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.model.ListAgents200Response;
import info.jab.cursor.generated.client.model.GetAgentConversation200Response;
import info.jab.cursor.generated.client.model.CreateAgent201Response;
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
 * Unit tests for CursorAgentInformationImpl class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CursorAgentInformationImpl Tests")
class CursorAgentInformationImplTest {

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
            assertThatThrownBy(() -> new CursorAgentInformationImpl(null, defaultApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when apiKey is empty")
        void should_throwIllegalArgumentException_when_apiKeyIsEmpty() {
            // When & Then
            assertThatThrownBy(() -> new CursorAgentInformationImpl("", defaultApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when apiKey is blank")
        void should_throwIllegalArgumentException_when_apiKeyIsBlank() {
            // When & Then
            assertThatThrownBy(() -> new CursorAgentInformationImpl("   ", defaultApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when defaultApi is null")
        void should_throwIllegalArgumentException_when_defaultApiIsNull() {
            // When & Then
            assertThatThrownBy(() -> new CursorAgentInformationImpl(TEST_API_KEY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DefaultApi cannot be null");
        }
    }

    @Nested
    @DisplayName("getAgents() Tests")
    class GetAgentsTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when limit is zero")
        void should_throwIllegalArgumentException_when_limitIsZero() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getAgents(0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when limit is negative")
        void should_throwIllegalArgumentException_when_limitIsNegative() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getAgents(-1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when cursor is empty string")
        void should_throwIllegalArgumentException_when_cursorIsEmptyString() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getAgents(null, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cursor cannot be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when cursor is blank")
        void should_throwIllegalArgumentException_when_cursorIsBlank() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getAgents(null, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cursor cannot be empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);
            ApiException apiException = new ApiException(500, "Internal Server Error");
            when(defaultApi.listAgents(any(), any(), any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(() -> impl.getAgents(null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get agents")
                .hasCause(apiException);
        }
    }

    @Nested
    @DisplayName("getStatus() Tests")
    class GetStatusTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is null")
        void should_throwIllegalArgumentException_when_agentIdIsNull() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is empty")
        void should_throwIllegalArgumentException_when_agentIdIsEmpty() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getStatus(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is blank")
        void should_throwIllegalArgumentException_when_agentIdIsBlank() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getStatus("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException with specific message when exception contains 'Unexpected value'")
        void should_throwRuntimeExceptionWithSpecificMessage_when_exceptionContainsUnexpectedValue() throws Exception {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);
            RuntimeException exception = new RuntimeException("Unexpected value: UNKNOWN_STATUS");
            when(defaultApi.getAgent(anyString(), any())).thenThrow(exception);

            // When & Then
            assertThatThrownBy(() -> impl.getStatus("test-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Agent status contains unknown value")
                .hasCause(exception);
        }

        @Test
        @DisplayName("Should throw RuntimeException when exception does not contain 'Unexpected value'")
        void should_throwRuntimeException_when_exceptionDoesNotContainUnexpectedValue() throws Exception {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);
            RuntimeException exception = new RuntimeException("Some other error");
            when(defaultApi.getAgent(anyString(), any())).thenThrow(exception);

            // When & Then
            assertThatThrownBy(() -> impl.getStatus("test-id"))
                .isInstanceOf(RuntimeException.class)
                .hasCause(exception);
        }

        @Test
        @DisplayName("Should throw RuntimeException when exception message is null")
        void should_throwRuntimeException_when_exceptionMessageIsNull() throws Exception {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);
            RuntimeException exception = new RuntimeException((String) null);
            when(defaultApi.getAgent(anyString(), any())).thenThrow(exception);

            // When & Then
            assertThatThrownBy(() -> impl.getStatus("test-id"))
                .isInstanceOf(RuntimeException.class)
                .hasCause(exception);
        }
    }

    @Nested
    @DisplayName("getAgentConversation() Tests")
    class GetAgentConversationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is null")
        void should_throwIllegalArgumentException_when_agentIdIsNull() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getAgentConversation(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is empty")
        void should_throwIllegalArgumentException_when_agentIdIsEmpty() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getAgentConversation(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is blank")
        void should_throwIllegalArgumentException_when_agentIdIsBlank() {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);

            // When & Then
            assertThatThrownBy(() -> impl.getAgentConversation("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
            CursorAgentInformationImpl impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);
            ApiException apiException = new ApiException(500, "Internal Server Error");
            when(defaultApi.getAgentConversation(anyString(), any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(() -> impl.getAgentConversation("test-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get agent conversation")
                .hasCause(apiException);
        }
    }
}

