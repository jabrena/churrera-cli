package info.jab.churrera.cli.service.handler;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.*;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.SequenceInfo;
import info.jab.churrera.workflow.WorkflowData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ParallelWorkflowHandler.
 */
@ExtendWith(MockitoExtension.class)
class ParallelWorkflowHandlerTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CLIAgent cliAgent;

    @Mock
    private AgentLauncher agentLauncher;

    @Mock
    private TimeoutManager timeoutManager;

    @Mock
    private FallbackExecutor fallbackExecutor;

    @Mock
    private ResultExtractor resultExtractor;

    private ParallelWorkflowHandler handler;
    private Job testJob;
    private WorkflowData testWorkflowData;
    private ParallelWorkflowData testParallelData;

    @BeforeEach
    void setUp() {
        handler = new ParallelWorkflowHandler(jobRepository, cliAgent, agentLauncher, timeoutManager, fallbackExecutor, resultExtractor);

        testJob = new Job("job-id", "/path/workflow.xml", null, "model", "repo", AgentState.creating(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        testParallelData = mock(ParallelWorkflowData.class);
        lenient().when(testParallelData.getSequences()).thenReturn(List.of(mock(SequenceInfo.class)));

        testWorkflowData = new WorkflowData(
            new PromptInfo("launch.pml", "pml"),
            "test-model",
            "test-repo",
            List.of(),
            testParallelData, null, null
        );
    }

    @Test
    void testProcessWorkflow_LaunchParentJob() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(testJob, testWorkflowData);

        // Then
        verify(agentLauncher).launchJobAgent(testJob, testWorkflowData);
        verify(jobRepository, atLeastOnce()).findById("job-id"); // Called after launch and potentially after status check
    }

    @Test
    void testProcessWorkflow_TimeoutReached_ExecuteFallback() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id").withWorkflowStartTime(LocalDateTime.now().minusSeconds(2));
        when(testParallelData.getTimeoutMillis()).thenReturn(1000L);
        when(timeoutManager.getElapsedMillis(jobWithAgent)).thenReturn(2000L);

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then
        verify(fallbackExecutor).executeFallbackForParallelChildren(jobWithAgent, testParallelData);
    }

    @Test
    void testProcessWorkflow_TimeoutReached_FallbackAlreadyExecuted() {
        // Given
        Job jobWithFallback = testJob.withCursorAgentId("agent-id").withWorkflowStartTime(LocalDateTime.now().minusSeconds(2))
            .withFallbackExecuted(true);
        when(testParallelData.getTimeoutMillis()).thenReturn(1000L);
        when(timeoutManager.getElapsedMillis(jobWithFallback)).thenReturn(2000L);

        // When
        handler.processWorkflow(jobWithFallback, testWorkflowData);

        // Then
        verify(fallbackExecutor, never()).executeFallbackForParallelChildren(any(), any());
    }

    @Test
    void testProcessWorkflow_StatusActive() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(testParallelData.getTimeoutMillis()).thenReturn(null);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.running());
        verify(resultExtractor, never()).extractResults(any(), any());
    }

    @Test
    void testProcessWorkflow_StatusSuccessful_ExtractResults() throws IOException {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(testParallelData.getTimeoutMillis()).thenReturn(null);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.finished());
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));
        List<Object> resultList = List.of("item1", "item2");
        when(resultExtractor.extractResults(jobWithAgent, testParallelData)).thenReturn(resultList);

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then
        verify(resultExtractor).extractResults(jobWithAgent, testParallelData);
        verify(jobRepository, atLeastOnce()).save(any(Job.class)); // Child jobs created
    }

    @Test
    void testProcessWorkflow_StatusSuccessful_ExtractionFails() throws IOException {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(testParallelData.getTimeoutMillis()).thenReturn(null);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.finished());
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));
        when(resultExtractor.extractResults(jobWithAgent, testParallelData)).thenReturn(null);

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.error());
        verify(jobRepository, never()).save(any(Job.class)); // No child jobs created
    }

    @Test
    void testProcessWorkflow_StatusTerminal() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id").withStatus(AgentState.error());
        lenient().when(testParallelData.getTimeoutMillis()).thenReturn(null);
        lenient().when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.error());

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then
        verify(resultExtractor, never()).extractResults(any(), any());
    }

    @Test
    void testProcessWorkflow_TerminalStatus_SkipPolling() {
        // Given
        Job terminalJob = testJob.withCursorAgentId("agent-id").withStatus(AgentState.finished());
        when(testParallelData.getTimeoutMillis()).thenReturn(null);

        // When
        handler.processWorkflow(terminalJob, testWorkflowData);

        // Then
        verify(cliAgent, never()).getAgentStatus(anyString());
    }

    @Test
    void testProcessWorkflow_StatusCheckException() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(testParallelData.getTimeoutMillis()).thenReturn(null);
        when(cliAgent.getAgentStatus("agent-id")).thenThrow(new RuntimeException("API error"));

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.error());
    }

    @Test
    void testProcessWorkflow_Exception() {
        // Given
        // Use a mock that will throw when getParallelWorkflowData is called
        WorkflowData mockWorkflowData = mock(WorkflowData.class);
        when(mockWorkflowData.getParallelWorkflowData()).thenThrow(new RuntimeException("Error"));

        // When - Should not throw, exception is caught internally
        assertDoesNotThrow(() -> handler.processWorkflow(testJob, mockWorkflowData));
    }

    @Test
    void testProcessWorkflow_TimeoutCheckBeforePolling() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id").withWorkflowStartTime(LocalDateTime.now().minusSeconds(2));
        when(testParallelData.getTimeoutMillis()).thenReturn(1000L);
        when(timeoutManager.getElapsedMillis(jobWithAgent)).thenReturn(2000L);

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then
        verify(fallbackExecutor).executeFallbackForParallelChildren(jobWithAgent, testParallelData);
        verify(cliAgent, never()).getAgentStatus(anyString());
    }

    @Test
    void testProcessWorkflow_NoTimeout() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(testParallelData.getTimeoutMillis()).thenReturn(null);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then
        verify(cliAgent).getAgentStatus("agent-id");
        verify(fallbackExecutor, never()).executeFallbackForParallelChildren(any(), any());
    }

    @Test
    void testProcessWorkflow_TimeoutNotReached() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id").withWorkflowStartTime(LocalDateTime.now().minusSeconds(1));
        when(testParallelData.getTimeoutMillis()).thenReturn(10000L);
        when(timeoutManager.getElapsedMillis(jobWithAgent)).thenReturn(5000L);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then
        verify(fallbackExecutor, never()).executeFallbackForParallelChildren(any(), any());
        verify(cliAgent).getAgentStatus("agent-id");
    }

    @Test
    void testProcessWorkflow_TimeoutWithNullWorkflowStartTime() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id").withWorkflowStartTime(null);
        when(testParallelData.getTimeoutMillis()).thenReturn(1000L);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then - timeout check should not be called if workflowStartTime is null
        verify(timeoutManager, never()).getElapsedMillis(any());
        verify(cliAgent).getAgentStatus("agent-id");
    }

    @Test
    void testProcessWorkflow_CreateChildJobs_EmptySequences() throws IOException {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(testParallelData.getTimeoutMillis()).thenReturn(null);
        when(testParallelData.getSequences()).thenReturn(List.of());
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.finished());
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));
        List<Object> resultList = List.of("item1", "item2");
        when(resultExtractor.extractResults(jobWithAgent, testParallelData)).thenReturn(resultList);

        // When
        handler.processWorkflow(jobWithAgent, testWorkflowData);

        // Then - should handle empty sequences gracefully
        verify(resultExtractor).extractResults(jobWithAgent, testParallelData);
        verify(jobRepository, never()).save(any(Job.class)); // No child jobs created due to empty sequences
    }
}

