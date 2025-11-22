package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.JobWithDetails;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.cursor.client.model.ConversationMessage;
import info.jab.cursor.client.model.ConversationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobLogsCommand.
 */
@ExtendWith(MockitoExtension.class)
class JobLogsCommandTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CLIAgent cliAgent;

    private JobLogsCommand jobLogsCommand;
    private Job testJob;
    private List<Prompt> testPrompts;

    @BeforeEach
    void setUp() {
        testJob = new Job("test-job-id",
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        testPrompts = List.of(
            new Prompt(
                "prompt-1",
                "test-job-id",
                "prompt1.pml",
                "COMPLETED",
                LocalDateTime.now(),
                LocalDateTime.now()
            ),
            new Prompt(
                "prompt-2",
                "test-job-id",
                "prompt2.pml",
                "SENT",
                LocalDateTime.now(),
                LocalDateTime.now()
            )
        );
    }

    @Test
    void testRun_JobNotFound() {
        // Given
        String jobId = "non-existent-job";
        jobLogsCommand = new JobLogsCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // When
        jobLogsCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, never()).findJobWithDetails(anyString());
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testRun_JobFoundWithoutCursorAgent() {
        // Given
        String jobId = "test-job-id";
        Job jobWithoutAgent = new Job(jobId,
            "/test/path",
            null, // no cursor agent
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobLogsCommand = new JobLogsCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(jobWithoutAgent));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(jobWithoutAgent, testPrompts)));

        // When
        jobLogsCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testRun_JobFoundWithCursorAgent_Success() {
        // Given
        String jobId = "test-job-id";
        jobLogsCommand = new JobLogsCommand(jobRepository, cliAgent, jobId);

        ConversationMessage message1 = new ConversationMessage("msg-1", "user_message", "User: Hello");
        ConversationMessage message2 = new ConversationMessage("msg-2", "agent_message", "Agent: Hi there!");

        ConversationResponse conversation = new ConversationResponse("agent-id", List.of(message1, message2));

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(testJob, testPrompts)));
        when(cliAgent.getConversation("cursor-agent-123")).thenReturn(conversation);

        // When
        jobLogsCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verify(cliAgent).getConversation("cursor-agent-123");
    }

    @Test
    void testRun_JobFoundWithCursorAgent_ConversationFails() {
        // Given
        String jobId = "test-job-id";
        jobLogsCommand = new JobLogsCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(testJob, testPrompts)));
        when(cliAgent.getConversation("cursor-agent-123"))
            .thenThrow(new IllegalArgumentException("""
                API key not found. Please provide it via:
                  1. .env file: CURSOR_API_KEY=YOUR_API_KEY
                  2. Environment variable: export CURSOR_API_KEY=YOUR_API_KEY
                """));

        // When
        jobLogsCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verify(cliAgent).getConversation("cursor-agent-123");
    }

    @Test
    void testRun_JobFoundWithCursorAgent_NullConversation() {
        // Given
        String jobId = "test-job-id";
        jobLogsCommand = new JobLogsCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(testJob, testPrompts)));
        when(cliAgent.getConversation("cursor-agent-123")).thenReturn(null);

        // When
        jobLogsCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verify(cliAgent).getConversation("cursor-agent-123");
    }

    @Test
    void testRun_JobFoundWithCursorAgent_EmptyMessages() {
        // Given
        String jobId = "test-job-id";
        jobLogsCommand = new JobLogsCommand(jobRepository, cliAgent, jobId);

        ConversationResponse conversation = new ConversationResponse("agent-id", null);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(testJob, testPrompts)));
        when(cliAgent.getConversation("cursor-agent-123")).thenReturn(conversation);

        // When
        jobLogsCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verify(cliAgent).getConversation("cursor-agent-123");
    }

    @Test
    void testRun_DatabaseException() {
        // Given
        String jobId = "test-job-id";
        jobLogsCommand = new JobLogsCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertDoesNotThrow(() -> jobLogsCommand.run());
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testRun_QueryException() {
        // Given
        String jobId = "test-job-id";
        jobLogsCommand = new JobLogsCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId)).thenThrow(new RuntimeException("Query error"));

        // When & Then
        assertDoesNotThrow(() -> jobLogsCommand.run());
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testRun_Resolves8CharPrefixUnique() {
        // Given
        String fullJobId = "test-job-id";
        String prefix = fullJobId.substring(0, 8); // 8-char prefix
        jobLogsCommand = new JobLogsCommand(jobRepository, cliAgent, prefix);

        // Repository resolution
        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(List.of(testJob));
        when(jobRepository.findJobWithDetails(fullJobId))
            .thenReturn(Optional.of(new JobWithDetails(testJob, testPrompts)));

        // When
        jobLogsCommand.run();

        // Then
        verify(jobRepository).findById(prefix);
        verify(jobRepository).findAll();
        verify(jobRepository).findJobWithDetails(fullJobId);
        // Conversation is attempted
        verify(cliAgent).getConversation("cursor-agent-123");
    }
}
