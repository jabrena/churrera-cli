package info.jab.churrera.cli.service.handler;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.*;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.WorkflowData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SequenceWorkflowHandler.
 */
@ExtendWith(MockitoExtension.class)
class SequenceWorkflowHandlerTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CLIAgent cliAgent;

    @Mock
    private AgentLauncher agentLauncher;

    @Mock
    private PromptProcessor promptProcessor;

    @Mock
    private TimeoutManager timeoutManager;

    @Mock
    private FallbackExecutor fallbackExecutor;

    private SequenceWorkflowHandler handler;
    private Job testJob;
    private List<Prompt> testPrompts;
    private WorkflowData testWorkflowData;

    @BeforeEach
    void setUp() {
        handler = new SequenceWorkflowHandler(jobRepository, cliAgent, agentLauncher, promptProcessor, timeoutManager, fallbackExecutor);

        testJob = new Job("job-id", "/path/workflow.xml", null, "model", "repo", AgentState.creating(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        testPrompts = List.of(
            new Prompt("prompt-1", "job-id", "prompt1.pml", "UNKNOWN", LocalDateTime.now(), LocalDateTime.now())
        );

        testWorkflowData = new WorkflowData(
            new PromptInfo("launch.pml", "pml"),
            "test-model",
            "test-repo",
            List.of(new PromptInfo("prompt1.pml", "pml")),
            null, null, null
        );
    }

    @Test
    void testProcessWorkflow_LaunchNewJob() {
        // Given
        when(timeoutManager.resetWorkflowStartTimeIfNeeded(any(Job.class))).thenReturn(testJob);
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(testJob, testPrompts, testWorkflowData);

        // Then
        verify(agentLauncher).launchJobAgent(testJob, testWorkflowData);
        verify(jobRepository).findById("job-id");
    }

    @Test
    void testProcessWorkflow_ExistingJob_ResetStaleTime() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithAgent)).thenReturn(jobWithAgent);
        // checkTimeout is only called if timeoutMillis is not null, so no stub needed here
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());
        lenient().when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData);

        // Then
        verify(timeoutManager).resetStaleWorkflowStartTime(jobWithAgent);
        verify(agentLauncher, never()).launchJobAgent(any(), any());
    }

    @Test
    void testProcessWorkflow_TimeoutReached_ExecuteFallback() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id").withTimeoutMillis(1000L);
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithAgent)).thenReturn(jobWithAgent);
        when(timeoutManager.checkTimeout(jobWithAgent))
            .thenReturn(new TimeoutManager.TimeoutCheckResult(true, 1500L, 1000L));
        Job jobAfterFallback = jobWithAgent.withFallbackExecuted(false);
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobAfterFallback));

        // When
        handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData);

        // Then
        verify(fallbackExecutor).executeFallback(jobWithAgent, testWorkflowData, 1500L, 1000L);
    }

    @Test
    void testProcessWorkflow_TimeoutReached_TerminalStatus() {
        // Given
        Job terminalJob = testJob.withCursorAgentId("agent-id").withTimeoutMillis(1000L).withStatus(AgentState.finished());
        when(timeoutManager.resetStaleWorkflowStartTime(terminalJob)).thenReturn(terminalJob);
        when(timeoutManager.checkTimeout(terminalJob))
            .thenReturn(new TimeoutManager.TimeoutCheckResult(true, 1500L, 1000L));

        // When
        handler.processWorkflow(terminalJob, testPrompts, testWorkflowData);

        // Then
        verify(fallbackExecutor, never()).executeFallback(any(), any(), anyLong(), anyLong());
    }

    @Test
    void testProcessWorkflow_TimeoutReached_FallbackAlreadyExecuted() {
        // Given
        Job jobWithFallback = testJob.withCursorAgentId("agent-id").withTimeoutMillis(1000L).withFallbackExecuted(true);
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithFallback)).thenReturn(jobWithFallback);
        when(timeoutManager.checkTimeout(jobWithFallback))
            .thenReturn(new TimeoutManager.TimeoutCheckResult(true, 1500L, 1000L));

        // When
        handler.processWorkflow(jobWithFallback, testPrompts, testWorkflowData);

        // Then
        verify(fallbackExecutor, never()).executeFallback(any(), any(), anyLong(), anyLong());
    }

    @Test
    void testProcessWorkflow_StatusActive() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithAgent)).thenReturn(jobWithAgent);
        // checkTimeout is only called if timeoutMillis is not null, so no stub needed here
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());
        lenient().when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.running());
        verify(promptProcessor, never()).processRemainingPrompts(any(), any(), any());
    }

    @Test
    void testProcessWorkflow_StatusSuccessful() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithAgent)).thenReturn(jobWithAgent);
        // checkTimeout is only called if timeoutMillis is not null, so no stub needed here
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.finished());
        lenient().when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.finished());
        verify(promptProcessor).processRemainingPrompts(jobWithAgent, testPrompts, testWorkflowData);
    }

    @Test
    void testProcessWorkflow_StatusTerminal() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithAgent)).thenReturn(jobWithAgent);
        // checkTimeout is only called if timeoutMillis is not null, so no stub needed here
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.error());
        lenient().when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.error());
        verify(promptProcessor, never()).processRemainingPrompts(any(), any(), any());
    }

    @Test
    void testProcessWorkflow_StatusCheckException() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithAgent)).thenReturn(jobWithAgent);
        // checkTimeout is only called if timeoutMillis is not null, so no stub needed here
        when(cliAgent.getAgentStatus("agent-id")).thenThrow(new RuntimeException("API error"));
        lenient().when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.error());
    }

    @Test
    void testProcessWorkflow_JustLaunched() {
        // Given
        when(timeoutManager.resetWorkflowStartTimeIfNeeded(any(Job.class))).thenReturn(testJob);
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(testJob, testPrompts, testWorkflowData);

        // Then
        verify(cliAgent, never()).getAgentStatus(anyString());
    }

    @Test
    void testProcessWorkflow_NoTimeout() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithAgent)).thenReturn(jobWithAgent);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());

        // When
        handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData);

        // Then
        verify(timeoutManager, never()).checkTimeout(any());
    }

    @Test
    void testProcessWorkflow_Exception() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id");
        when(timeoutManager.resetStaleWorkflowStartTime(any())).thenThrow(new RuntimeException("Error"));

        // When - Should not throw, exception is caught internally
        assertDoesNotThrow(() -> handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData));
    }

    @Test
    void testProcessWorkflow_TimeoutReached_AfterLaunch() {
        // Given
        Job jobWithStartTime = testJob.withWorkflowStartTime(LocalDateTime.now().minusSeconds(2))
            .withTimeoutMillis(1000L);
        when(timeoutManager.resetWorkflowStartTimeIfNeeded(any(Job.class))).thenReturn(jobWithStartTime);
        Job jobWithAgent = jobWithStartTime.withCursorAgentId("agent-id");
        // First findById returns job with agent (after launch)
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));
        when(timeoutManager.checkTimeout(jobWithAgent))
            .thenReturn(new TimeoutManager.TimeoutCheckResult(true, 1500L, 1000L));
        // Second findById returns job after fallback
        Job jobAfterFallback = jobWithAgent.withFallbackExecuted(false);
        when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent), Optional.of(jobAfterFallback));

        // When
        handler.processWorkflow(testJob, testPrompts, testWorkflowData);

        // Then
        verify(fallbackExecutor).executeFallback(eq(jobWithAgent), eq(testWorkflowData), eq(1500L), eq(1000L));
    }

    @Test
    void testProcessWorkflow_TimeoutNotReached() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id")
            .withTimeoutMillis(1000L)
            .withWorkflowStartTime(LocalDateTime.now().minusSeconds(1));
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithAgent)).thenReturn(jobWithAgent);
        when(timeoutManager.checkTimeout(jobWithAgent))
            .thenReturn(new TimeoutManager.TimeoutCheckResult(false, 500L, 1000L));
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());
        lenient().when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData);

        // Then
        verify(fallbackExecutor, never()).executeFallback(any(), any(), anyLong(), anyLong());
        verify(cliAgent).getAgentStatus("agent-id");
    }

    @Test
    void testProcessWorkflow_TimeoutWithNullWorkflowStartTime() {
        // Given
        Job jobWithAgent = testJob.withCursorAgentId("agent-id").withTimeoutMillis(1000L).withWorkflowStartTime(null);
        when(timeoutManager.resetStaleWorkflowStartTime(jobWithAgent)).thenReturn(jobWithAgent);
        // checkTimeout is called even if workflowStartTime is null, but it handles it internally
        when(timeoutManager.checkTimeout(jobWithAgent))
            .thenReturn(new TimeoutManager.TimeoutCheckResult(false, 0L, 1000L));
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());
        lenient().when(jobRepository.findById("job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testPrompts, testWorkflowData);

        // Then - timeout check is called, but fallback should not be executed
        verify(timeoutManager).checkTimeout(jobWithAgent);
        verify(fallbackExecutor, never()).executeFallback(any(), any(), anyLong(), anyLong());
    }
}

