package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.cursor.client.model.ConversationResponse;
import info.jab.cursor.client.model.ConversationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for JobLogDisplayService.
 */
@ExtendWith(MockitoExtension.class)
class JobLogDisplayServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CLIAgent cliAgent;

    private JobLogDisplayService service;

    @BeforeEach
    void setUp() {
        service = new JobLogDisplayService(jobRepository, cliAgent);
    }

    @Test
    void testDisplayLogsForJob_NoCursorAgentId() {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo",
            AgentState.creating(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        // When
        service.displayLogsForJob(job);

        // Then
        verify(cliAgent, never()).getConversation(anyString());
    }

    @Test
    void testDisplayLogsForJob_WithCursorAgentId_ConversationWithMessages() {
        // Given
        Job job = new Job("job-id", "/path", "cursor-agent-123", "model", "repo",
            AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        ConversationMessage msg1 = new ConversationMessage("msg1", "user", "message1");
        ConversationMessage msg2 = new ConversationMessage("msg2", "assistant", "message2");
        List<ConversationMessage> messages = List.of(msg1, msg2);
        ConversationResponse conversation = new ConversationResponse("conv-id", messages);

        when(cliAgent.getConversation("cursor-agent-123")).thenReturn(conversation);

        // When
        service.displayLogsForJob(job);

        // Then
        verify(cliAgent).getConversation("cursor-agent-123");
    }

    @Test
    void testDisplayLogsForJob_WithCursorAgentId_NullConversation() {
        // Given
        Job job = new Job("job-id", "/path", "cursor-agent-123", "model", "repo",
            AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        when(cliAgent.getConversation("cursor-agent-123")).thenReturn(null);

        // When
        service.displayLogsForJob(job);

        // Then
        verify(cliAgent).getConversation("cursor-agent-123");
    }

    @Test
    void testDisplayLogsForJob_WithCursorAgentId_ConversationWithNullMessages() {
        // Given
        Job job = new Job("job-id", "/path", "cursor-agent-123", "model", "repo",
            AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        ConversationResponse conversation = new ConversationResponse("conv-id", null);
        when(cliAgent.getConversation("cursor-agent-123")).thenReturn(conversation);

        // When
        service.displayLogsForJob(job);

        // Then
        verify(cliAgent).getConversation("cursor-agent-123");
    }

    @Test
    void testDisplayLogsForJob_WithCursorAgentId_EmptyMessages() {
        // Given
        Job job = new Job("job-id", "/path", "cursor-agent-123", "model", "repo",
            AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        ConversationResponse conversation = new ConversationResponse("conv-id", new ArrayList<>());
        when(cliAgent.getConversation("cursor-agent-123")).thenReturn(conversation);

        // When
        service.displayLogsForJob(job);

        // Then
        verify(cliAgent).getConversation("cursor-agent-123");
    }

    @Test
    void testDisplayLogsForJob_WithCursorAgentId_Exception() {
        // Given
        Job job = new Job("job-id", "/path", "cursor-agent-123", "model", "repo",
            AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        when(cliAgent.getConversation("cursor-agent-123"))
            .thenThrow(new RuntimeException("API error"));

        // When
        service.displayLogsForJob(job);

        // Then - should handle exception gracefully
        verify(cliAgent).getConversation("cursor-agent-123");
    }
}

