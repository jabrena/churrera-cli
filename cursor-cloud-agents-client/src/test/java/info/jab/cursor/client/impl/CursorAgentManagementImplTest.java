package info.jab.cursor.client.impl;

import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private CursorAgentManagementImpl impl;

    @BeforeEach
    void setUp() {
        impl = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);
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
            assertThatThrownBy(() -> new CursorAgentManagementImpl(apiKey, defaultApi))
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

        @ParameterizedTest(name = "Should throw IllegalArgumentException when prompt is {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when prompt is invalid")
        void should_throwIllegalArgumentException_when_promptIsInvalid(String prompt) {
            // When & Then
            assertThatThrownBy(() -> impl.launch(prompt, "model", "repo", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        }

        @ParameterizedTest(name = "Should throw IllegalArgumentException when model is {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when model is invalid")
        void should_throwIllegalArgumentException_when_modelIsInvalid(String model) {
            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", model, "repo", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model cannot be null or empty");
        }

        @ParameterizedTest(name = "Should throw IllegalArgumentException when repository is {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when repository is invalid")
        void should_throwIllegalArgumentException_when_repositoryIsInvalid(String repository) {
            // When & Then
            assertThatThrownBy(() -> impl.launch("prompt", "model", repository, true))
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

        @ParameterizedTest(name = "Should throw IllegalArgumentException when agentId is {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when agentId is invalid")
        void should_throwIllegalArgumentException_when_agentIdIsInvalid(String agentId) {
            // When & Then
            assertThatThrownBy(() -> impl.followUp(agentId, "prompt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @ParameterizedTest(name = "Should throw IllegalArgumentException when prompt is {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when prompt is invalid")
        void should_throwIllegalArgumentException_when_promptIsInvalid(String prompt) {
            // When & Then
            assertThatThrownBy(() -> impl.followUp("agent-id", prompt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
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

        @ParameterizedTest(name = "Should throw IllegalArgumentException when agentId is {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw IllegalArgumentException when agentId is invalid")
        void should_throwIllegalArgumentException_when_agentIdIsInvalid(String agentId) {
            // When & Then
            assertThatThrownBy(() -> impl.delete(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
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

