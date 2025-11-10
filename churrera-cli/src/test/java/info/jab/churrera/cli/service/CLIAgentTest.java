package info.jab.churrera.cli.service;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.cursor.CursorAgentManagement;
import info.jab.cursor.CursorAgentInformation;
import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.ConversationResponse;
import info.jab.cursor.client.model.FollowUpResponse;
import info.jab.churrera.agent.AgentState;
import info.jab.churrera.util.PmlConverter;
import info.jab.churrera.util.PropertyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private AgentResponse mockAgent;

    @Mock
    private FollowUpResponse mockFollowUpResponse;

    @Mock
    private ConversationResponse mockConversationResponse;

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
            AgentState.CREATING,LocalDateTime.now(), LocalDateTime.now(), null, null, null);

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(mockAgent);
        when(mockAgent.getId()).thenReturn("new-agent-id");

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(mockAgent);
        when(mockAgent.getId()).thenReturn("new-agent-id");

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(mockAgent);
        when(mockAgent.getId()).thenReturn("new-agent-id");

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentManagement.followUp(anyString(), anyString())).thenReturn(mockFollowUpResponse);
        when(mockFollowUpResponse.getId()).thenReturn("follow-up-id");

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentManagement.followUp(anyString(), anyString()))
                .thenThrow(new RuntimeException("Follow-up failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.followUpForPrompt("agent-id", "pml content", "pml", null));
            assertTrue(exception.getMessage().contains("Failed to send follow-up"));    }

    @Test
    void testMonitorAgent_TerminalState() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentInformation.getStatus(anyString())).thenReturn(mockAgent);
        when(mockAgent.getStatus()).thenReturn(AgentResponse.StatusEnum.FINISHED);

            // When
            AgentState result = cliAgent.monitorAgent("agent-id", 1);

            // Then
            assertNotNull(result);
            assertEquals(AgentState.FINISHED, result);
            verify(cursorAgentInformation).getStatus("agent-id");
    }

    @Test
    void testMonitorAgent_MultipleChecksBeforeTerminal() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentInformation.getStatus(anyString())).thenReturn(mockAgent);
            // Always return FINISHED
            when(mockAgent.getStatus()).thenReturn(AgentResponse.StatusEnum.FINISHED);

            // When - Using delay of 0 to avoid actual sleep in test
            AgentState result = cliAgent.monitorAgent("agent-id", 0);

            // Then
            assertEquals(AgentState.FINISHED, result);
            // Should be called at least once
            verify(cursorAgentInformation, atLeastOnce()).getStatus("agent-id");
    }

    @Test
    void testMonitorAgent_InterruptedException() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentInformation.getStatus(anyString())).thenReturn(mockAgent);
        when(mockAgent.getStatus()).thenReturn(AgentResponse.StatusEnum.RUNNING);

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentInformation.getStatus(anyString()))
                .thenThrow(new RuntimeException("Temporary error"))
                .thenReturn(mockAgent);
        when(mockAgent.getStatus()).thenReturn(AgentResponse.StatusEnum.FINISHED);

            // When
            Thread monitorThread = new Thread(() -> {
                AgentState result = cliAgent.monitorAgent("agent-id", 1);
                assertEquals(AgentState.FINISHED, result);
            });

            monitorThread.start();
            monitorThread.join(5000);

            // Then
            verify(cursorAgentInformation, atLeast(2)).getStatus("agent-id");
    }

    @Test
    void testGetConversation_Success() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentInformation.getAgentConversation(anyString())).thenReturn(mockConversationResponse);

            // When
            ConversationResponse result = cliAgent.getConversation("agent-id");

            // Then
            assertEquals(mockConversationResponse, result);
            verify(cursorAgentInformation).getAgentConversation("agent-id");
    }

    @Test
    void testGetConversation_Failure() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentInformation.getAgentConversation(anyString()))
                .thenThrow(new RuntimeException("Get conversation failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.getConversation("agent-id"));
            assertTrue(exception.getMessage().contains("Failed to get agent conversation"));    }

    @Test
    void testDeleteAgent_Success() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            // When
            cliAgent.deleteAgent("agent-id");

            // Then
            verify(cursorAgentManagement).delete("agent-id");
    }

    @Test
    void testDeleteAgent_Failure() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            doThrow(new RuntimeException("Delete failed")).when(cursorAgentManagement).delete(anyString());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.deleteAgent("agent-id"));
            assertTrue(exception.getMessage().contains("Failed to delete agent"));    }

    @Test
    void testUpdateJobCursorIdInDatabase_Success() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            // When
            cliAgent.updateJobCursorIdInDatabase(testJob, "new-agent-id", AgentState.CREATING);

            // Then
            verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testUpdateJobCursorIdInDatabase_Failure() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            doThrow(new RuntimeException("Save failed")).when(jobRepository).save(any(Job.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.updateJobCursorIdInDatabase(testJob, "new-agent-id", AgentState.CREATING));
            assertTrue(exception.getMessage().contains("Failed to update job in database"));    }

    @Test
    void testUpdateJobStatusInDatabase_Success() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            // When
            cliAgent.updateJobStatusInDatabase(testJob, AgentState.FINISHED);

            // Then
            verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testUpdatePromptInDatabase_Success() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            // When
            cliAgent.updatePromptInDatabase(testPrompt, "COMPLETED");

            // Then
            verify(jobRepository).savePrompt(any(Prompt.class));
    }

    @Test
    void testUpdatePromptInDatabase_Failure() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            doThrow(new RuntimeException("Save failed")).when(jobRepository).savePrompt(any(Prompt.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.updatePromptInDatabase(testPrompt, "COMPLETED"));
            assertTrue(exception.getMessage().contains("Failed to update prompt in database"));    }

    @Test
    void testUpdateJobInDatabase_Success() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            // When
            cliAgent.updateJobInDatabase(testJob);

            // Then
            verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testGetAgentStatus_Success() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentInformation.getStatus(anyString())).thenReturn(mockAgent);
        when(mockAgent.getStatus()).thenReturn(AgentResponse.StatusEnum.RUNNING);

            // When
            AgentState result = cliAgent.getAgentStatus("agent-id");

            // Then
            assertEquals(AgentState.RUNNING, result);
            verify(cursorAgentInformation).getStatus("agent-id");
    }

    @Test
    void testGetAgentStatus_Failure() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

        when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(mockAgent);
        when(mockAgent.getId()).thenReturn("new-agent-id");

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(mockAgent);
        when(mockAgent.getId()).thenReturn("new-agent-id");

            // When - empty bindValue should skip replacement
            String result = cliAgent.launchAgentForJob(testJob, "pml content", "pml", "", true);

            // Then
            assertEquals("new-agent-id", result);
            verify(cursorAgentManagement).launch("converted markdown", "test-model", "test-repo", true);
    }

    @Test
    void testLaunchAgentForJob_MarkdownType_NoConversion() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentManagement.launch(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(mockAgent);
        when(mockAgent.getId()).thenReturn("new-agent-id");

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

        when(cursorAgentManagement.followUp(anyString(), anyString())).thenReturn(mockFollowUpResponse);
        when(mockFollowUpResponse.getId()).thenReturn("follow-up-id");

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentManagement.followUp(anyString(), anyString())).thenReturn(mockFollowUpResponse);
        when(mockFollowUpResponse.getId()).thenReturn("follow-up-id");

            // When - empty bindValue should skip replacement
            String result = cliAgent.followUpForPrompt("agent-id", "pml content", "pml", "");

            // Then
            assertEquals("follow-up-id", result);
            verify(cursorAgentManagement).followUp("agent-id", "converted markdown");
    }

    @Test
    void testFollowUpForPrompt_MarkdownType_NoConversion() {
        // Given
        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentManagement.followUp(anyString(), anyString())).thenReturn(mockFollowUpResponse);
        when(mockFollowUpResponse.getId()).thenReturn("follow-up-id");

            // When - md type should not trigger PML conversion
            String result = cliAgent.followUpForPrompt("agent-id", "markdown content", "md", null);

            // Then
            assertEquals("follow-up-id", result);
            verify(cursorAgentManagement).followUp("agent-id", "markdown content");
    }

    @Test
    void testGetConversationContent_WithNullMessages() {
        // Given
        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentInformation.getAgentConversation(anyString())).thenReturn(mockConversationResponse);
        when(mockConversationResponse.getMessages()).thenReturn(null);

            // When
            String result = cliAgent.getConversationContent("agent-id");

            // Then
            assertEquals("", result);
            verify(cursorAgentInformation).getAgentConversation("agent-id");
    }

    @Test
    void testGetConversationContent_WithMessagesContainingNullText() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            var mockMessage = mock(info.jab.cursor.client.model.ConversationMessage.class);
        when(mockMessage.getText()).thenReturn(null);

            when(cursorAgentInformation.getAgentConversation(anyString())).thenReturn(mockConversationResponse);
        when(mockConversationResponse.getMessages()).thenReturn(java.util.List.of(mockMessage));

            // When
            String result = cliAgent.getConversationContent("agent-id");

            // Then
            assertEquals("", result);
            verify(cursorAgentInformation).getAgentConversation("agent-id");
    }

    @Test
    void testGetConversationContent_Failure() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            when(cursorAgentInformation.getAgentConversation(anyString()))
                .thenThrow(new RuntimeException("Conversation content failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.getConversationContent("agent-id"));
            assertTrue(exception.getMessage().contains("Failed to get agent conversation content"));    }

    @Test
    void testMonitorAgent_InterruptedDuringSleepAfterError() {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

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

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            doThrow(new RuntimeException("Save failed")).when(jobRepository).save(any(Job.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.updateJobStatusInDatabase(testJob, AgentState.FINISHED));
            assertTrue(exception.getMessage().contains("Failed to update job status in database"));    }

    @Test
    void testUpdateJobInDatabase_Failure() throws Exception {

        // Given

        cliAgent = new CLIAgent(jobRepository, cursorAgentManagement, cursorAgentInformation, mockPmlConverter, mockPropertyResolver);

            doThrow(new RuntimeException("Save failed")).when(jobRepository).save(any(Job.class));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cliAgent.updateJobInDatabase(testJob));
            assertTrue(exception.getMessage().contains("Failed to update job in database"));    }
}
