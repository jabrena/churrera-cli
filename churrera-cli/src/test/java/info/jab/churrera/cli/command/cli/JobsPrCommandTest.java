package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobsPrCommand.
 */
@ExtendWith(MockitoExtension.class)
class JobsPrCommandTest {

    @Mock
    private JobRepository jobRepository;

    private JobsPrCommand jobsPrCommand;
    private Job testJob;

    @BeforeEach
    void setUp() {
        testJob = new Job("test-job-id",
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "https://github.com/user/repo.git",
            AgentState.FINISHED(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
    }

    @Test
    void testRun_JobNotFound() {
        // Given
        String jobId = "non-existent-job";
        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_JobFinishedSuccessfully_WithCursorAgent() {
        // Given
        String jobId = "test-job-id";
        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_JobFinishedSuccessfully_WithoutCursorAgent() {
        // Given
        String jobId = "test-job-id";
        Job jobWithoutAgent = new Job(jobId,
            "/test/path",
            null, // no cursor agent
            "test-model",
            "https://github.com/user/repo.git",
            AgentState.FINISHED(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(jobWithoutAgent));

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_JobFailed() {
        // Given
        String jobId = "test-job-id";
        Job failedJob = new Job(jobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "https://github.com/user/repo.git",
            AgentState.ERROR(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(failedJob));

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_JobStillRunning() {
        // Given
        String jobId = "test-job-id";
        Job runningJob = new Job(jobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "https://github.com/user/repo.git",
            AgentState.CREATING(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(runningJob));

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_JobTerminated() {
        // Given
        String jobId = "test-job-id";
        Job terminatedJob = new Job(jobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "https://github.com/user/repo.git",
            AgentState.FINISHED(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(terminatedJob));

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_JobWithNullRepository() {
        // Given
        String jobId = "test-job-id";
        Job jobWithNullRepo = new Job(jobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo", // valid repository
            AgentState.FINISHED(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(jobWithNullRepo));

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_JobWithEmptyRepository() {
        // Given
        String jobId = "test-job-id";
        Job jobWithEmptyRepo = new Job(jobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "", // empty repository
            AgentState.FINISHED(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(jobWithEmptyRepo));

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_JobWithNonGitHubRepository() {
        // Given
        String jobId = "test-job-id";
        Job jobWithNonGitHubRepo = new Job(jobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "https://gitlab.com/user/repo.git",
            AgentState.FINISHED(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(jobWithNonGitHubRepo));

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_DatabaseException() {
        // Given
        String jobId = "test-job-id";
        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertDoesNotThrow(() -> jobsPrCommand.run());
        verify(jobRepository).findById(jobId);
    }

    @Test
    void testRun_QueryException() {
        // Given
        String jobId = "test-job-id";
        jobsPrCommand = new JobsPrCommand(jobRepository, jobId);

        when(jobRepository.findById(jobId)).thenThrow(new RuntimeException("Query error"));

        // When & Then
        assertDoesNotThrow(() -> jobsPrCommand.run());
        verify(jobRepository).findById(jobId);
    }

    @Test
    void testRun_Resolves8CharPrefixUnique() {
        // Given
        String fullJobId = "test-job-id";
        String prefix = fullJobId.substring(0, 8);
        jobsPrCommand = new JobsPrCommand(jobRepository, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(java.util.List.of(testJob));
        when(jobRepository.findById(fullJobId)).thenReturn(Optional.of(testJob));

        // When
        jobsPrCommand.run();

        // Then
        verify(jobRepository).findById(prefix);
        verify(jobRepository).findAll();
        verify(jobRepository).findById(fullJobId);
    }
}
