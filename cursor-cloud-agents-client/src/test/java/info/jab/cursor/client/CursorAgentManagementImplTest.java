package info.jab.cursor.client;

import info.jab.cursor.client.impl.CursorAgentManagementImpl;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.model.CreateAgent201Response;
import info.jab.cursor.generated.client.model.DeleteAgent200Response;
import info.jab.cursor.generated.client.model.CreateAgentRequest;
import info.jab.cursor.generated.client.model.CreateAgent201ResponseSource;
import info.jab.cursor.generated.client.model.CreateAgent201ResponseTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CursorAgentManagementImpl focusing on validation and branch coverage.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CursorAgentManagementImpl Unit Tests")
class CursorAgentManagementImplTest {

    @Mock
    private DefaultApi defaultApi;

    private CursorAgentManagementImpl cursorAgentManagement;
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_BASE_URL = "http://localhost:8080";
    private static final String TEST_AGENT_ID = "bc-test-agent-12345";

    @BeforeEach
    void setUp() {
        cursorAgentManagement = new CursorAgentManagementImpl(TEST_API_KEY, defaultApi);
    }

    @Nested
    @DisplayName("Launch Agent Validation Tests")
    class LaunchAgentValidationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is null")
        void should_throwIllegalArgumentException_when_promptIsNull() {
            // Given
            String prompt = null;
            String model = "claude-4.5-sonnet-thinking";
            String repository = "https://github.com/jabrena/churrera";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is empty")
        void should_throwIllegalArgumentException_when_promptIsEmpty() {
            // Given
            String prompt = "";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "https://github.com/jabrena/churrera";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is whitespace only")
        void should_throwIllegalArgumentException_when_promptIsWhitespaceOnly() {
            // Given
            String prompt = "   ";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "https://github.com/jabrena/churrera";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when model is null")
        void should_throwIllegalArgumentException_when_modelIsNull() {
            // Given
            String prompt = "Add installation instructions";
            String model = null;
            String repository = "https://github.com/jabrena/churrera";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Model cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when model is empty")
        void should_throwIllegalArgumentException_when_modelIsEmpty() {
            // Given
            String prompt = "Add installation instructions";
            String model = "";
            String repository = "https://github.com/jabrena/churrera";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Model cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when model is whitespace only")
        void should_throwIllegalArgumentException_when_modelIsWhitespaceOnly() {
            // Given
            String prompt = "Add installation instructions";
            String model = "\t\n";
            String repository = "https://github.com/jabrena/churrera";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Model cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when repository is null")
        void should_throwIllegalArgumentException_when_repositoryIsNull() {
            // Given
            String prompt = "Add installation instructions";
            String model = "claude-4.5-sonnet-thinking";
            String repository = null;

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Repository cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when repository is empty")
        void should_throwIllegalArgumentException_when_repositoryIsEmpty() {
            // Given
            String prompt = "Add installation instructions";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Repository cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when repository is whitespace only")
        void should_throwIllegalArgumentException_when_repositoryIsWhitespaceOnly() {
            // Given
            String prompt = "Add installation instructions";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "  ";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Repository cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }
    }

    @Nested
    @DisplayName("Follow-up Validation Tests")
    class FollowUpValidationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is null")
        void should_throwIllegalArgumentException_when_agentIdIsNull() {
            // Given
            String agentId = null;
            String prompt = "Also add unit tests";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.followUp(agentId, prompt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is empty")
        void should_throwIllegalArgumentException_when_agentIdIsEmpty() {
            // Given
            String agentId = "";
            String prompt = "Also add unit tests";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.followUp(agentId, prompt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is whitespace only")
        void should_throwIllegalArgumentException_when_agentIdIsWhitespaceOnly() {
            // Given
            String agentId = "   ";
            String prompt = "Also add unit tests";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.followUp(agentId, prompt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is null")
        void should_throwIllegalArgumentException_when_promptIsNull() {
            // Given
            String agentId = TEST_AGENT_ID;
            String prompt = null;

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.followUp(agentId, prompt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is empty")
        void should_throwIllegalArgumentException_when_promptIsEmpty() {
            // Given
            String agentId = TEST_AGENT_ID;
            String prompt = "";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.followUp(agentId, prompt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when prompt is whitespace only")
        void should_throwIllegalArgumentException_when_promptIsWhitespaceOnly() {
            // Given
            String agentId = TEST_AGENT_ID;
            String prompt = "\t\n";

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.followUp(agentId, prompt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }
    }

    @Nested
    @DisplayName("Delete Agent Validation Tests")
    class DeleteAgentValidationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when agentId is null")
        void should_throwIllegalArgumentException_when_agentIdIsNull() {
            // Given
            String agentId = null;

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.delete(agentId))
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
            assertThatThrownBy(() -> cursorAgentManagement.delete(agentId))
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
            assertThatThrownBy(() -> cursorAgentManagement.delete(agentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Agent ID cannot be null or empty");

            verifyNoInteractions(defaultApi);
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should wrap ApiException in RuntimeException when launch fails")
        void should_wrapApiExceptionInRuntimeException_when_launchFails() throws ApiException {
            // Given
            String prompt = "Add installation instructions";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "https://github.com/jabrena/churrera";
            ApiException apiException = new ApiException(400, "Bad Request");

            when(defaultApi.createAgent(any(), any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.launch(prompt, model, repository, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to launch agent")
                .hasCause(apiException);
        }

        @Test
        @DisplayName("Should wrap ApiException in RuntimeException when followUp fails")
        void should_wrapApiExceptionInRuntimeException_when_followUpFails() throws ApiException {
            // Given
            String agentId = TEST_AGENT_ID;
            String prompt = "Also add unit tests";
            ApiException apiException = new ApiException(404, "Not Found");

            when(defaultApi.addFollowup(eq(agentId), any(), any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.followUp(agentId, prompt))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to follow up agent")
                .hasCause(apiException);
        }

        @Test
        @DisplayName("Should wrap ApiException in RuntimeException when delete fails")
        void should_wrapApiExceptionInRuntimeException_when_deleteFails() throws ApiException {
            // Given
            String agentId = TEST_AGENT_ID;
            ApiException apiException = new ApiException(404, "Not Found");

            when(defaultApi.deleteAgent(eq(agentId), any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(() -> cursorAgentManagement.delete(agentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete agent")
                .hasCause(apiException);
        }
    }

    @Nested
    @DisplayName("PR Flag Tests")
    class PRFlagTests {

        @Test
        @DisplayName("Should set autoCreatePr to true when pr is true")
        void should_setAutoCreatePrToTrue_when_prIsTrue() throws ApiException {
            // Given
            String prompt = "Add installation instructions";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "https://github.com/jabrena/churrera";
            boolean pr = true;

            CreateAgent201Response mockResponse = new CreateAgent201Response();
            mockResponse.setId(TEST_AGENT_ID);
            mockResponse.setName("Test Agent");
            mockResponse.setStatus(CreateAgent201Response.StatusEnum.CREATING);
            mockResponse.setCreatedAt(java.time.OffsetDateTime.now());
            CreateAgent201ResponseSource source = new CreateAgent201ResponseSource();
            source.setRepository(repository);
            source.setRef("main");
            mockResponse.setSource(source);
            CreateAgent201ResponseTarget target = new CreateAgent201ResponseTarget();
            target.setBranchName("cursor/test");
            target.setUrl("https://cursor.com/agents?id=" + TEST_AGENT_ID);
            target.setAutoCreatePr(false);
            mockResponse.setTarget(target);
            when(defaultApi.createAgent(any(), any())).thenReturn(mockResponse);

            // When
            cursorAgentManagement.launch(prompt, model, repository, pr);

            // Then
            ArgumentCaptor<CreateAgentRequest> requestCaptor = ArgumentCaptor.forClass(CreateAgentRequest.class);
            verify(defaultApi).createAgent(requestCaptor.capture(), any());
            CreateAgentRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getTarget()).isNotNull();
            assertThat(capturedRequest.getTarget().getAutoCreatePr()).isTrue();
        }

        @Test
        @DisplayName("Should set autoCreatePr to false when pr is false")
        void should_setAutoCreatePrToFalse_when_prIsFalse() throws ApiException {
            // Given
            String prompt = "Add installation instructions";
            String model = "claude-4.5-sonnet-thinking";
            String repository = "https://github.com/jabrena/churrera";
            boolean pr = false;

            CreateAgent201Response mockResponse = new CreateAgent201Response();
            mockResponse.setId(TEST_AGENT_ID);
            mockResponse.setName("Test Agent");
            mockResponse.setStatus(CreateAgent201Response.StatusEnum.CREATING);
            mockResponse.setCreatedAt(java.time.OffsetDateTime.now());
            CreateAgent201ResponseSource source = new CreateAgent201ResponseSource();
            source.setRepository(repository);
            source.setRef("main");
            mockResponse.setSource(source);
            CreateAgent201ResponseTarget target = new CreateAgent201ResponseTarget();
            target.setBranchName("cursor/test");
            target.setUrl("https://cursor.com/agents?id=" + TEST_AGENT_ID);
            target.setAutoCreatePr(false);
            mockResponse.setTarget(target);
            when(defaultApi.createAgent(any(), any())).thenReturn(mockResponse);

            // When
            cursorAgentManagement.launch(prompt, model, repository, pr);

            // Then
            ArgumentCaptor<CreateAgentRequest> requestCaptor = ArgumentCaptor.forClass(CreateAgentRequest.class);
            verify(defaultApi).createAgent(requestCaptor.capture(), any());
            CreateAgentRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getTarget()).isNotNull();
            assertThat(capturedRequest.getTarget().getAutoCreatePr()).isFalse();
        }
    }
}

