package info.jab.churrera.cli.service;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.util.PmlConverter;
import info.jab.churrera.util.PropertyResolver;
import info.jab.cursor.client.CursorAgentGeneralEndpoints;
import info.jab.cursor.client.CursorAgentInformation;
import info.jab.cursor.client.CursorAgentManagement;
import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.AgentStatus;
import info.jab.cursor.client.model.ConversationMessage;
import info.jab.cursor.client.model.ConversationResponse;
import info.jab.cursor.client.model.FollowUpResponse;
import info.jab.cursor.client.model.Source;
import info.jab.cursor.client.model.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CLIAgent.
 */
@ExtendWith(MockitoExtension.class)
class CLIAgentTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CursorAgentManagement cursorAgentManagement;

    @Mock
    private CursorAgentInformation cursorAgentInformation;

    @Mock
    private CursorAgentGeneralEndpoints cursorAgentGeneralEndpoints;

    // Helper method to create test AgentResponse
    private AgentResponse createTestAgentResponse(String id, AgentStatus status) {
        return new AgentResponse(
            id,
            "Test Agent",
            status,
            new Source(URI.create("https://github.com/test/repo"), "main"),
            new Target("cursor/test", URI.create("https://cursor.com/agents?id=test"), false, false, false),
            OffsetDateTime.now()
        );
    }

    // Helper method to create test FollowUpResponse
    private FollowUpResponse createTestFollowUpResponse(String id) {
        return new FollowUpResponse(id);
    }

    // Helper method to create test ConversationResponse
    private ConversationResponse createTestConversationResponse(String id, List<ConversationMessage> messages) {
        return new ConversationResponse(id, messages);
    }

    @Mock
    private PmlConverter mockPmlConverter;

    @Mock
    private PropertyResolver mockPropertyResolver;

    private CLIAgent cliAgent;
    private Job testJob;
    private Prompt testPrompt;

    @BeforeEach
    void setUp() {
        testJob = new Job("test-job-id",
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.creating(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        testPrompt = new Prompt(
            "prompt-1",
            "test-job-id",
            "prompt1.pml",
            "UNKNOWN",
            LocalDateTime.now(),
            LocalDateTime.now()
        );

    }

    @Test
    void testLaunchAgentForJob_Success() {

        // Given
        when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenReturn("converted markdown");

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(createTestAgentResponse("new-agent-id", AgentStatus.CREATING));

            // When
            String result = cliAgent.launchAgentForJob(testJob, "pml content", "pml", null, true);

            // Then
            assertEquals("new-agent-id", result);
            verify(cursorAgentManagement).launch("converted markdown", "test-model", "test-repo", true);
    }

    @Test
    void testLaunchAgentForJob_PmlConversionFallback() {

        // Given

            // First conversion fails, second succeeds
            when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenThrow(new RuntimeException("Conversion failed"));
        when(mockPmlConverter.toMarkdownFromContent(anyString()))
                .thenReturn("fallback markdown");

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(createTestAgentResponse("new-agent-id", AgentStatus.CREATING));

            // When
            String result = cliAgent.launchAgentForJob(testJob, "pml content", "pml", null, true);

            // Then
            assertEquals("new-agent-id", result);
            verify(cursorAgentManagement).launch("fallback markdown", "test-model", "test-repo", true);
    }

    @Test
    void testLaunchAgentForJob_AllConversionsFail_UsesOriginal() {

        // Given

            // Both conversions fail
            when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenThrow(new RuntimeException("Conversion failed"));
        when(mockPmlConverter.toMarkdownFromContent(anyString()))
                .thenThrow(new RuntimeException("Fallback failed"));

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(createTestAgentResponse("new-agent-id", AgentStatus.CREATING));

            // When
            String result = cliAgent.launchAgentForJob(testJob, "pml content", "pml", null, true);

            // Then
            assertEquals("new-agent-id", result);
            verify(cursorAgentManagement).launch("pml content", "test-model", "test-repo", true);
    }

    @Test
    void testLaunchAgentForJob_LaunchFailure() {

        // Given
        when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenReturn("converted markdown");

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Launch failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.launchAgentForJob(testJob, "pml content", "pml", null, true));
            assertTrue(exception.getMessage().contains("Failed to launch agent"));    }

    @Test
    void testFollowUpForPrompt_Success() {

        // Given
        when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenReturn("converted markdown");

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.followUp(anyString(), anyString()))
                .thenReturn(createTestFollowUpResponse("follow-up-id"));

            // When
            String result = cliAgent.followUpForPrompt("agent-id", "pml content", "pml", null);

            // Then
            assertEquals("follow-up-id", result);
            verify(cursorAgentManagement).followUp("agent-id", "converted markdown");
    }

    @Test
    void testFollowUpForPrompt_Failure() {

        // Given
        when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenReturn("converted markdown");

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.followUp(anyString(), anyString()))
                .thenThrow(new RuntimeException("Follow-up failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.followUpForPrompt("agent-id", "pml content", "pml", null));
            assertTrue(exception.getMessage().contains("Failed to send follow-up"));    }

    @Test
    void testMonitorAgent_TerminalState() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getStatus(anyString()))
                .thenReturn(createTestAgentResponse("agent-id", AgentStatus.FINISHED));

            // When
            AgentState result = cliAgent.monitorAgent("agent-id", 1);

            // Then
            assertNotNull(result);
            assertEquals(AgentState.finished(), result);
            verify(cursorAgentInformation).getStatus("agent-id");
    }

    @Test
    void testMonitorAgent_MultipleChecksBeforeTerminal() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getStatus(anyString()))
                .thenReturn(createTestAgentResponse("agent-id", AgentStatus.FINISHED));

            // When - Using delay of 0 to avoid actual sleep in test
            AgentState result = cliAgent.monitorAgent("agent-id", 0);

            // Then
            assertEquals(AgentState.finished(), result);
            // Should be called at least once
            verify(cursorAgentInformation, atLeastOnce()).getStatus("agent-id");
    }

    @Test
    void testMonitorAgent_InterruptedException() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getStatus(anyString()))
                .thenReturn(createTestAgentResponse("agent-id", AgentStatus.RUNNING));

            // Interrupt the current thread
            Thread.currentThread().interrupt();

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.monitorAgent("agent-id", 1));
            assertTrue(exception.getMessage().contains("Monitoring interrupted"));
            assertTrue(Thread.interrupted()); // Clear the interrupt flag
    }

    @Test
    void testMonitorAgent_StatusCheckErrorContinuesMonitoring() throws InterruptedException {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getStatus(anyString()))
                .thenThrow(new RuntimeException("Temporary error"))
                .thenReturn(createTestAgentResponse("agent-id", AgentStatus.FINISHED));

            // When
            Thread monitorThread = new Thread(() -> {
                AgentState result = cliAgent.monitorAgent("agent-id", 1);
                assertEquals(AgentState.finished(), result);
            });

            monitorThread.start();
            monitorThread.join(5000);

            // Then
            verify(cursorAgentInformation, atLeast(2)).getStatus("agent-id");
    }

    @Test
    void testGetConversation_Success() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            ConversationResponse testConversation = createTestConversationResponse("agent-id", List.of());
            when(cursorAgentInformation.getAgentConversation(anyString())).thenReturn(testConversation);

            // When
            ConversationResponse result = cliAgent.getConversation("agent-id");

            // Then
            assertEquals(testConversation, result);
            verify(cursorAgentInformation).getAgentConversation("agent-id");
    }

    @Test
    void testGetConversation_Failure() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getAgentConversation(anyString()))
                .thenThrow(new RuntimeException("Get conversation failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.getConversation("agent-id"));
            assertTrue(exception.getMessage().contains("Failed to get agent conversation"));    }

    @Test
    void testDeleteAgent_Success() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            // When
            cliAgent.deleteAgent("agent-id");

            // Then
            verify(cursorAgentManagement).delete("agent-id");
    }

    @Test
    void testDeleteAgent_Failure() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            doThrow(new RuntimeException("Delete failed")).when(cursorAgentManagement).delete(anyString());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.deleteAgent("agent-id"));
            assertTrue(exception.getMessage().contains("Failed to delete agent"));    }

    @Test
    void testUpdateJobCursorIdInDatabase_Success() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            // When
            cliAgent.updateJobCursorIdInDatabase(testJob, "new-agent-id", AgentState.creating());

            // Then
            verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testUpdateJobCursorIdInDatabase_Failure() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            doThrow(new RuntimeException("Save failed")).when(jobRepository).save(any(Job.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> {
                    AgentState state = AgentState.creating();
                    cliAgent.updateJobCursorIdInDatabase(testJob, "new-agent-id", state);
                });
            assertTrue(exception.getMessage().contains("Failed to update job in database"));    }

    @Test
    void testUpdateJobStatusInDatabase_Success() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            // When
            cliAgent.updateJobStatusInDatabase(testJob, AgentState.finished());

            // Then
            verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testUpdatePromptInDatabase_Success() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            // When
            cliAgent.updatePromptInDatabase(testPrompt, "COMPLETED");

            // Then
            verify(jobRepository).savePrompt(any(Prompt.class));
    }

    @Test
    void testUpdatePromptInDatabase_Failure() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            doThrow(new RuntimeException("Save failed")).when(jobRepository).savePrompt(any(Prompt.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.updatePromptInDatabase(testPrompt, "COMPLETED"));
            assertTrue(exception.getMessage().contains("Failed to update prompt in database"));    }

    @Test
    void testUpdateJobInDatabase_Success() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            // When
            cliAgent.updateJobInDatabase(testJob);

            // Then
            verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testGetAgentStatus_Success() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getStatus(anyString()))
                .thenReturn(createTestAgentResponse("agent-id", AgentStatus.RUNNING));

            // When
            AgentState result = cliAgent.getAgentStatus("agent-id");

            // Then
            assertEquals(AgentState.running(), result);
            verify(cursorAgentInformation).getStatus("agent-id");
    }

    @Test
    void testGetAgentStatus_Failure() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getStatus(anyString())).thenThrow(new RuntimeException("Status failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.getAgentStatus("agent-id"));
            assertTrue(exception.getMessage().contains("Failed to get agent status"));    }

    // Tests for bindValue replacement logic
    @Test
    void testLaunchAgentForJob_WithBindValue() {
        // Given
        when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenReturn("converted markdown with <input>INPUT</input>");

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

        when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean()))
            .thenReturn(createTestAgentResponse("new-agent-id", AgentStatus.CREATING));

        // When
        String result = cliAgent.launchAgentForJob(testJob, "pml content", "pml", "test-value", true);

        // Then
        assertEquals("new-agent-id", result);
        verify(cursorAgentManagement).launch("converted markdown with <input>test-value</input>", "test-model", "test-repo", true);
    }

    @Test
    void testLaunchAgentForJob_WithEmptyBindValue() {
        // Given
        when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenReturn("converted markdown");

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(createTestAgentResponse("new-agent-id", AgentStatus.CREATING));

            // When - empty bindValue should skip replacement
            String result = cliAgent.launchAgentForJob(testJob, "pml content", "pml", "", true);

            // Then
            assertEquals("new-agent-id", result);
            verify(cursorAgentManagement).launch("converted markdown", "test-model", "test-repo", true);
    }

    @Test
    void testLaunchAgentForJob_MarkdownType_NoConversion() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(createTestAgentResponse("new-agent-id", AgentStatus.CREATING));

            // When - md type should not trigger PML conversion
            String result = cliAgent.launchAgentForJob(testJob, "markdown content", "md", null, true);

            // Then
            assertEquals("new-agent-id", result);
            verify(cursorAgentManagement).launch("markdown content", "test-model", "test-repo", true);
    }

    @Test
    void testFollowUpForPrompt_WithBindValue() {
        // Given
        when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenReturn("converted markdown with <input>INPUT</input>");

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

        when(cursorAgentManagement.followUp(anyString(), anyString()))
            .thenReturn(createTestFollowUpResponse("follow-up-id"));

        // When
        String result = cliAgent.followUpForPrompt("agent-id", "pml content", "pml", "bound-value");

        // Then
        assertEquals("follow-up-id", result);
        verify(cursorAgentManagement).followUp("agent-id", "converted markdown with <input>bound-value</input>");
    }

    @Test
    void testFollowUpForPrompt_WithEmptyBindValue() {
        // Given
        when(mockPmlConverter.toMarkdownFromContent(anyString(), anyString()))
                .thenReturn("converted markdown");

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.followUp(anyString(), anyString()))
                .thenReturn(createTestFollowUpResponse("follow-up-id"));

            // When - empty bindValue should skip replacement
            String result = cliAgent.followUpForPrompt("agent-id", "pml content", "pml", "");

            // Then
            assertEquals("follow-up-id", result);
            verify(cursorAgentManagement).followUp("agent-id", "converted markdown");
    }

    @Test
    void testFollowUpForPrompt_MarkdownType_NoConversion() {
        // Given
        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentManagement.followUp(anyString(), anyString()))
                .thenReturn(createTestFollowUpResponse("follow-up-id"));

            // When - md type should not trigger PML conversion
            String result = cliAgent.followUpForPrompt("agent-id", "markdown content", "md", null);

            // Then
            assertEquals("follow-up-id", result);
            verify(cursorAgentManagement).followUp("agent-id", "markdown content");
    }

    @Test
    void testGetConversationContent_WithNullMessages() {
        // Given
        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getAgentConversation(anyString()))
                .thenReturn(createTestConversationResponse("agent-id", null));

            // When
            String result = cliAgent.getConversationContent("agent-id");

            // Then
            assertEquals("", result);
            verify(cursorAgentInformation).getAgentConversation("agent-id");
    }

    @Test
    void testGetConversationContent_WithMessagesContainingNullText() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            ConversationMessage mockMessage = new ConversationMessage("msg-id", "user_message", null);
            when(cursorAgentInformation.getAgentConversation(anyString()))
                .thenReturn(createTestConversationResponse("agent-id", List.of(mockMessage)));

            // When
            String result = cliAgent.getConversationContent("agent-id");

            // Then
            assertEquals("", result);
            verify(cursorAgentInformation).getAgentConversation("agent-id");
    }

    @Test
    void testGetConversationContent_Failure() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getAgentConversation(anyString()))
                .thenThrow(new RuntimeException("Conversation content failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.getConversationContent("agent-id"));
            assertTrue(exception.getMessage().contains("Failed to get agent conversation content"));    }

    @Test
    void testMonitorAgent_InterruptedDuringSleepAfterError() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            when(cursorAgentInformation.getStatus(anyString()))
                .thenThrow(new RuntimeException("Temporary error"));

            // Interrupt the thread before calling monitorAgent
            Thread.currentThread().interrupt();

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.monitorAgent("agent-id", 1));
            assertTrue(exception.getMessage().contains("Monitoring interrupted"));
            assertTrue(Thread.interrupted()); // Clear the interrupt flag
    }

    @Test
    void testUpdateJobStatusInDatabase_Failure() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            doThrow(new RuntimeException("Save failed")).when(jobRepository).save(any(Job.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> {
                    AgentState state = AgentState.finished();
                    cliAgent.updateJobStatusInDatabase(testJob, state);
                });
            assertTrue(exception.getMessage().contains("Failed to update job status in database"));    }

    @Test
    void testUpdateJobInDatabase_Failure() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, cursorAgentGeneralEndpoints, mockPmlConverter);

            doThrow(new RuntimeException("Save failed")).when(jobRepository).save(any(Job.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.updateJobInDatabase(testJob));
            assertTrue(exception.getMessage().contains("Failed to update job in database"));    }
}
