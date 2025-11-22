package info.jab.cursor.client.impl;

import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private CursorAgentInformationImpl impl;

    @BeforeEach
    void setUp() {
        impl = new CursorAgentInformationImpl(TEST_API_KEY, defaultApi);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @ParameterizedTest(name = "Should throw IllegalArgumentException when apiKey is {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when apiKey is invalid")
        void should_throwIllegalArgumentException_when_apiKeyIsInvalid(String apiKey) {
            // When & Then
            assertThatThrownBy(() -> new CursorAgentInformationImpl(apiKey, defaultApi))
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

        @ParameterizedTest(name = "Should throw IllegalArgumentException when limit is {0}")
        @ValueSource(ints = {0, -1, -10})
        @DisplayName("Should throw IllegalArgumentException when limit is invalid")
        void should_throwIllegalArgumentException_when_limitIsInvalid(int limit) {
            // When & Then
            assertThatThrownBy(() -> impl.getAgents(limit, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");
        }

        @ParameterizedTest(name = "Should throw IllegalArgumentException when cursor is {0}")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when cursor is invalid")
        void should_throwIllegalArgumentException_when_cursorIsInvalid(String cursor) {
            // When & Then
            assertThatThrownBy(() -> impl.getAgents(null, cursor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cursor cannot be empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
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

        @ParameterizedTest(name = "Should throw IllegalArgumentException when agentId is {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when agentId is invalid")
        void should_throwIllegalArgumentException_when_agentIdIsInvalid(String agentId) {
            // When & Then
            assertThatThrownBy(() -> impl.getStatus(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @ParameterizedTest(name = "Should throw RuntimeException when exception message is {0}")
        @MethodSource("exceptionMessageProvider")
        @DisplayName("Should throw RuntimeException when exception occurs")
        void should_throwRuntimeException_when_exceptionOccurs(String exceptionMessage, String expectedMessage) throws Exception {
            // Given
            RuntimeException exception = new RuntimeException(exceptionMessage);
            when(defaultApi.getAgent(anyString(), any())).thenThrow(exception);

            // When & Then
            var thrown = assertThatThrownBy(() -> impl.getStatus("test-id"))
                .isInstanceOf(RuntimeException.class)
                .hasCause(exception);

            if (expectedMessage != null) {
                thrown.hasMessageContaining(expectedMessage);
            }
        }

        private static Stream<Arguments> exceptionMessageProvider() {
            return Stream.of(
                Arguments.of("Unexpected value: UNKNOWN_STATUS", "Agent status contains unknown value"),
                Arguments.of("Some other error", null),
                Arguments.of(null, null)
            );
        }
    }

    @Nested
    @DisplayName("getAgentConversation() Tests")
    class GetAgentConversationTests {

        @ParameterizedTest(name = "Should throw IllegalArgumentException when agentId is {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when agentId is invalid")
        void should_throwIllegalArgumentException_when_agentIdIsInvalid(String agentId) {
            // When & Then
            assertThatThrownBy(() -> impl.getAgentConversation(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
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

