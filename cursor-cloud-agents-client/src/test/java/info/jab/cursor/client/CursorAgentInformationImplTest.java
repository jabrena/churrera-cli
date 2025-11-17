package info.jab.cursor.client;

import info.jab.cursor.client.impl.CursorAgentInformationImpl;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInner;
import info.jab.cursor.generated.client.model.ListAgents200Response;
import info.jab.cursor.generated.client.model.GetAgentConversation200Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CursorAgentInformationImpl focusing on validation and branch coverage.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CursorAgentInformationImpl Unit Tests")
class CursorAgentInformationImplTest {

    @Mock
    private DefaultApi defaultApi;

    private CursorAgentInformationImpl cursorAgentInformation;
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_BASE_URL = "http://localhost:8080";
    private static final String TEST_AGENT_ID = "bc-test-agent-12345";

    @BeforeEach
    void setUp() {
        cursorAgentInformation = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);
    }

    @Nested
    @DisplayName("Get Status Validation Tests")
    class GetStatusValidationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is null")
        void should_throwIllegalArgumentException_when_agentIdIsNull() {
            // Given
            String agentId = null;

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getStatus(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is empty")
        void should_throwIllegalArgumentException_when_agentIdIsEmpty() {
            // Given
            String agentId = "";

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getStatus(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is whitespace only")
        void should_throwIllegalArgumentException_when_agentIdIsWhitespaceOnly() {
            // Given
            String agentId = "   ";

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getStatus(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }
    }

    @Nested
    @DisplayName("Get Status Exception Handling Tests")
    class GetStatusExceptionHandlingTests {

        @Test
        @DisplayName("Should throw RuntimeException with special message when status contains unexpected value")
        void should_throwRuntimeExceptionWithSpecialMessage_when_statusContainsUnexpectedValue() throws ApiException {
            // Given
            String agentId = TEST_AGENT_ID;
            ApiException unexpectedValueException = new ApiException("Unexpected value 'UNKNOWN_STATUS' for status");

            when(defaultApi.getAgent(eq(agentId), any())).thenThrow(unexpectedValueException);

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getStatus(agentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Agent status contains unknown value")
                .hasCause(unexpectedValueException);
        }

        @Test
        @DisplayName("Should throw RuntimeException when status exception message does not contain 'Unexpected value'")
        void should_throwRuntimeException_when_statusExceptionDoesNotContainUnexpectedValue() throws ApiException {
            // Given
            String agentId = TEST_AGENT_ID;
            ApiException otherException = new ApiException("Agent not found");

            when(defaultApi.getAgent(eq(agentId), any())).thenThrow(otherException);

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getStatus(agentId))
                .isInstanceOf(RuntimeException.class)
                .hasCause(otherException);
        }

        @Test
        @DisplayName("Should throw RuntimeException when status exception message is null")
        void should_throwRuntimeException_when_statusExceptionMessageIsNull() throws ApiException {
            // Given
            String agentId = TEST_AGENT_ID;
            ApiException exceptionWithNullMessage = new ApiException((String) null);

            when(defaultApi.getAgent(eq(agentId), any())).thenThrow(exceptionWithNullMessage);

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getStatus(agentId))
                .isInstanceOf(RuntimeException.class)
                .hasCause(exceptionWithNullMessage);
        }
    }

    @Nested
    @DisplayName("Get Agent Conversation Validation Tests")
    class GetAgentConversationValidationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is null")
        void should_throwIllegalArgumentException_when_agentIdIsNull() {
            // Given
            String agentId = null;

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getAgentConversation(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is empty")
        void should_throwIllegalArgumentException_when_agentIdIsEmpty() {
            // Given
            String agentId = "";

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getAgentConversation(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is whitespace only")
        void should_throwIllegalArgumentException_when_agentIdIsWhitespaceOnly() {
            // Given
            String agentId = "   ";

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getAgentConversation(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }
    }

    @Nested
    @DisplayName("Get Agent Conversation Exception Handling Tests")
    class GetAgentConversationExceptionHandlingTests {

        @Test
        @DisplayName("Should wrap exception in RuntimeException when getAgentConversation fails")
        void should_wrapExceptionInRuntimeException_when_getAgentConversationFails() throws ApiException {
            // Given
            String agentId = TEST_AGENT_ID;
            ApiException exception = new ApiException("Agent conversation not found");

            when(defaultApi.getAgentConversation(eq(agentId), any())).thenThrow(exception);

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getAgentConversation(agentId))
                .isInstanceOf(RuntimeException.class)
                .hasCause(exception);
        }
    }

    @Nested
    @DisplayName("Get Agents Exception Handling Tests")
    class GetAgentsExceptionHandlingTests {

        @Test
        @DisplayName("Should wrap exception in RuntimeException when getAgents fails")
        void should_wrapExceptionInRuntimeException_when_getAgentsFails() throws ApiException {
            // Given
            ApiException exception = new ApiException("Failed to retrieve agents");

            when(defaultApi.listAgents(any(), any(), any())).thenThrow(exception);

            // When & Then
            assertThatThrownBy(() -> cursorAgentInformation.getAgents(null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get agents")
                .hasCause(exception);
        }
    }
}

