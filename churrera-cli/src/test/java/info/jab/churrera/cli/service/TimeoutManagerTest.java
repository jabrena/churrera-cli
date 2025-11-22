package info.jab.churrera.cli.service;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TimeoutManager.
 */
@ExtendWith(MockitoExtension.class)
class TimeoutManagerTest {

    @Mock
    private JobRepository jobRepository;

    private TimeoutManager timeoutManager;

    @BeforeEach
    void setUp() {
        timeoutManager = new TimeoutManager(jobRepository);
    }

    @Test
    void testResetWorkflowStartTimeIfNeeded_NoTimeout() {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        // When
        Job result = timeoutManager.resetWorkflowStartTimeIfNeeded(job);

        // Then
        assertEquals(job, result);
        verifyNoInteractions(jobRepository);
    }

    @Test
    void testResetWorkflowStartTimeIfNeeded_WithTimeout() throws Exception {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, null, null, null);
        LocalDateTime newStartTime = LocalDateTime.now();
        Job updatedJob = job.withWorkflowStartTime(newStartTime);
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(updatedJob));

        // When
        Job result = timeoutManager.resetWorkflowStartTimeIfNeeded(job);

        // Then
        assertNotNull(result);
        verify(jobRepository).save(any(Job.class));
        verify(jobRepository).findById("job-id");
    }

    @Test
    void testResetWorkflowStartTimeIfNeeded_Exception() throws Exception {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, null, null, null);
        doThrow(new RuntimeException("DB error")).when(jobRepository).save(any(Job.class));

        // When
        Job result = timeoutManager.resetWorkflowStartTimeIfNeeded(job);

        // Then
        assertEquals(job, result); // Should return original job on error
    }

    @Test
    void testResetStaleWorkflowStartTime_NoTimeout() {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        // When
        Job result = timeoutManager.resetStaleWorkflowStartTime(job);

        // Then
        assertEquals(job, result);
        verifyNoInteractions(jobRepository);
    }

    @Test
    void testResetStaleWorkflowStartTime_TerminalStatus() {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.FINISHED(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, null, null, null);

        // When
        Job result = timeoutManager.resetStaleWorkflowStartTime(job);

        // Then
        assertEquals(job, result);
        verifyNoInteractions(jobRepository);
    }

    @Test
    void testResetStaleWorkflowStartTime_NullStartTime() throws Exception {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, null, null, null);
        LocalDateTime newStartTime = LocalDateTime.now();
        Job updatedJob = job.withWorkflowStartTime(newStartTime);
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(updatedJob));

        // When
        Job result = timeoutManager.resetStaleWorkflowStartTime(job);

        // Then
        assertNotNull(result);
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testResetStaleWorkflowStartTime_StaleStartTime() throws Exception {
        // Given
        LocalDateTime staleTime = LocalDateTime.now().minusHours(1);
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, staleTime, null, null);
        LocalDateTime newStartTime = LocalDateTime.now();
        Job updatedJob = job.withWorkflowStartTime(newStartTime);
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(updatedJob));

        // When
        Job result = timeoutManager.resetStaleWorkflowStartTime(job);

        // Then
        assertNotNull(result);
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testResetStaleWorkflowStartTime_FreshStartTime() {
        // Given
        LocalDateTime freshTime = LocalDateTime.now().minusSeconds(1);
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, freshTime, null, null);

        // When
        Job result = timeoutManager.resetStaleWorkflowStartTime(job);

        // Then
        assertEquals(job, result);
        verifyNoInteractions(jobRepository);
    }

    @Test
    void testResetStaleWorkflowStartTime_Exception() throws Exception {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, null, null, null);
        doThrow(new RuntimeException("DB error")).when(jobRepository).save(any(Job.class));

        // When
        Job result = timeoutManager.resetStaleWorkflowStartTime(job);

        // Then
        assertEquals(job, result);
    }

    @Test
    void testEnsureWorkflowStartTimeSet_NoTimeout() {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        // When
        Job result = timeoutManager.ensureWorkflowStartTimeSet(job);

        // Then
        assertEquals(job, result);
        verifyNoInteractions(jobRepository);
    }

    @Test
    void testEnsureWorkflowStartTimeSet_HasStartTime() {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, startTime, null, null);

        // When
        Job result = timeoutManager.ensureWorkflowStartTimeSet(job);

        // Then
        assertEquals(job, result);
        verifyNoInteractions(jobRepository);
    }

    @Test
    void testEnsureWorkflowStartTimeSet_NullStartTime() throws Exception {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, null, null, null);
        LocalDateTime newStartTime = LocalDateTime.now();
        Job updatedJob = job.withWorkflowStartTime(newStartTime);
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(updatedJob));

        // When
        Job result = timeoutManager.ensureWorkflowStartTimeSet(job);

        // Then
        assertNotNull(result);
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testEnsureWorkflowStartTimeSet_Exception() throws Exception {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, null, null, null);
        doThrow(new RuntimeException("DB error")).when(jobRepository).save(any(Job.class));

        // When
        Job result = timeoutManager.ensureWorkflowStartTimeSet(job);

        // Then
        assertEquals(job, result);
    }

    @Test
    void testHasReachedTimeout_NoAgentId() {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, LocalDateTime.now(), null, null);

        // When
        boolean result = timeoutManager.hasReachedTimeout(job);

        // Then
        assertFalse(result);
    }

    @Test
    void testHasReachedTimeout_NoTimeout() {
        // Given
        Job job = new Job("job-id", "/path", "agent-id", "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, LocalDateTime.now(), null, null);

        // When
        boolean result = timeoutManager.hasReachedTimeout(job);

        // Then
        assertFalse(result);
    }

    @Test
    void testHasReachedTimeout_NotReached() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(1);
        Job job = new Job("job-id", "/path", "agent-id", "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 10000L, startTime, null, null);

        // When
        boolean result = timeoutManager.hasReachedTimeout(job);

        // Then
        assertFalse(result);
    }

    @Test
    void testHasReachedTimeout_Reached() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(2);
        Job job = new Job("job-id", "/path", "agent-id", "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, startTime, null, null);

        // When
        boolean result = timeoutManager.hasReachedTimeout(job);

        // Then
        assertTrue(result);
    }

    @Test
    void testGetElapsedMillis_NoTimeout() {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, LocalDateTime.now(), null, null);

        // When
        long result = timeoutManager.getElapsedMillis(job);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testGetElapsedMillis_NoStartTime() {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, null, null, null);

        // When
        long result = timeoutManager.getElapsedMillis(job);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testGetElapsedMillis_WithElapsedTime() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(5);
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, startTime, null, null);

        // When
        long result = timeoutManager.getElapsedMillis(job);

        // Then
        assertTrue(result >= 5000);
    }

    @Test
    void testCheckTimeout_NoAgentId() {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, LocalDateTime.now(), null, null);

        // When
        TimeoutManager.TimeoutCheckResult result = timeoutManager.checkTimeout(job);

        // Then
        assertFalse(result.hasReachedTimeout());
        assertEquals(0, result.getElapsedMillis());
    }

    @Test
    void testCheckTimeout_NoTimeout() {
        // Given
        Job job = new Job("job-id", "/path", "agent-id", "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, LocalDateTime.now(), null, null);

        // When
        TimeoutManager.TimeoutCheckResult result = timeoutManager.checkTimeout(job);

        // Then
        assertFalse(result.hasReachedTimeout());
        assertEquals(0, result.getElapsedMillis());
    }

    @Test
    void testCheckTimeout_NotReached() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(1);
        Job job = new Job("job-id", "/path", "agent-id", "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 10000L, startTime, null, null);

        // When
        TimeoutManager.TimeoutCheckResult result = timeoutManager.checkTimeout(job);

        // Then
        assertFalse(result.hasReachedTimeout());
        assertTrue(result.getElapsedMillis() >= 1000);
    }

    @Test
    void testCheckTimeout_Reached() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(2);
        Job job = new Job("job-id", "/path", "agent-id", "model", "repo", AgentState.CREATING(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, startTime, null, null);

        // When
        TimeoutManager.TimeoutCheckResult result = timeoutManager.checkTimeout(job);

        // Then
        assertTrue(result.hasReachedTimeout());
        assertTrue(result.getElapsedMillis() >= 2000);
        assertEquals(1000L, result.getTimeoutMillis());
    }
}
