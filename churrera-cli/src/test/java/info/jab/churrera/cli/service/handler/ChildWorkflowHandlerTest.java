package info.jab.churrera.cli.service.handler;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChildWorkflowHandler.
 */
@ExtendWith(MockitoExtension.class)
class ChildWorkflowHandlerTest {

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

    private ChildWorkflowHandler handler;
    private Job testChildJob;
    private List<Prompt> testPrompts;
    private WorkflowData testParentWorkflowData;
    private ParallelWorkflowData testParallelData;
    private SequenceInfo testSequenceInfo;

    @BeforeEach
    void setUp() {
        handler = new ChildWorkflowHandler(jobRepository, cliAgent, agentLauncher, promptProcessor, timeoutManager, fallbackExecutor);

        testChildJob = new Job("child-job-id", "/path/workflow.xml", null, "model", "repo", AgentState.creating(),
            LocalDateTime.now(), LocalDateTime.now(), "parent-job-id", "bound-value", null, null, null, null, null);

        testPrompts = List.of(
            new Prompt("prompt-1", "child-job-id", "prompt1.pml", "UNKNOWN", LocalDateTime.now(), LocalDateTime.now())
        );

        testSequenceInfo = mock(SequenceInfo.class);
        lenient().when(testSequenceInfo.getPrompts()).thenReturn(List.of(
            new PromptInfo("launch.pml", "pml"),
            new PromptInfo("update.pml", "pml")
        ));
        lenient().when(testSequenceInfo.getModel()).thenReturn("test-model");
        lenient().when(testSequenceInfo.getRepository()).thenReturn("test-repo");

        testParallelData = mock(ParallelWorkflowData.class);
        lenient().when(testParallelData.getSequences()).thenReturn(List.of(testSequenceInfo));
        lenient().when(testParallelData.getTimeoutMillis()).thenReturn(1000L);
        lenient().when(testParallelData.getFallbackSrc()).thenReturn("fallback.pml");

        testParentWorkflowData = new WorkflowData(
            new PromptInfo("parent.pml", "pml"),
            "parent-model",
            "parent-repo",
            List.of(),
            testParallelData, null, null
        );
    }

    @Test
    void testProcessWorkflow_NotParallelWorkflow() {
        // Given
        WorkflowData nonParallelData = new WorkflowData(
            new PromptInfo("launch.pml", "pml"),
            "model",
            "repo",
            List.of(),
            null, null, null
        );

        // When
        handler.processWorkflow(testChildJob, nonParallelData, testPrompts);

        // Then
        verifyNoInteractions(agentLauncher);
    }

    @Test
    void testProcessWorkflow_NoSequences() {
        // Given
        when(testParallelData.getSequences()).thenReturn(List.of());

        // When
        handler.processWorkflow(testChildJob, testParentWorkflowData, testPrompts);

        // Then
        verifyNoInteractions(agentLauncher);
    }

    @Test
    void testProcessWorkflow_NoPromptsInSequence() {
        // Given
        when(testSequenceInfo.getPrompts()).thenReturn(List.of());

        // When
        handler.processWorkflow(testChildJob, testParentWorkflowData, testPrompts);

        // Then
        verifyNoInteractions(agentLauncher);
    }

    @Test
    void testProcessWorkflow_LaunchChildJob() throws Exception {
        // Given
        Job jobWithAgent = testChildJob.withCursorAgentId("agent-id");
        when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(testChildJob, testParentWorkflowData, testPrompts);

        // Then
        verify(agentLauncher).launchJobAgent(eq(testChildJob), any(WorkflowData.class));
        verify(jobRepository).findById("child-job-id");
    }

    @Test
    void testProcessWorkflow_TimeoutReached_ExecuteFallback() throws Exception {
        // Given
        Job jobWithAgent = testChildJob.withCursorAgentId("agent-id").withWorkflowStartTime(LocalDateTime.now().minusSeconds(2));
        when(timeoutManager.getElapsedMillis(jobWithAgent)).thenReturn(2000L);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());
        Job jobAfterFallback = jobWithAgent.withFallbackExecuted(false);
        when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(jobAfterFallback));

        // When
        handler.processWorkflow(jobWithAgent, testParentWorkflowData, testPrompts);

        // Then
        verify(fallbackExecutor).executeFallback(any(Job.class), any(WorkflowData.class), eq(2000L), eq(1000L));
    }

    @Test
    void testProcessWorkflow_TimeoutReached_TerminalStatus() throws Exception {
        // Given
        Job terminalJob = testChildJob.withCursorAgentId("agent-id").withWorkflowStartTime(LocalDateTime.now().minusSeconds(2))
            .withStatus(AgentState.finished());
        lenient().when(timeoutManager.getElapsedMillis(terminalJob)).thenReturn(2000L);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.finished());

        // When
        handler.processWorkflow(terminalJob, testParentWorkflowData, testPrompts);

        // Then
        verify(fallbackExecutor, never()).executeFallback(any(), any(), anyLong(), anyLong());
        verify(cliAgent).getAgentStatus("agent-id"); // Still checks status
    }

    @Test
    void testProcessWorkflow_TimeoutReached_FallbackAlreadyExecuted() throws Exception {
        // Given
        Job jobWithFallback = testChildJob.withCursorAgentId("agent-id").withWorkflowStartTime(LocalDateTime.now().minusSeconds(2))
            .withFallbackExecuted(true);
        lenient().when(timeoutManager.getElapsedMillis(jobWithFallback)).thenReturn(2000L);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());

        // When
        handler.processWorkflow(jobWithFallback, testParentWorkflowData, testPrompts);

        // Then
        verify(fallbackExecutor, never()).executeFallback(any(), any(), anyLong(), anyLong());
        verify(cliAgent).getAgentStatus("agent-id"); // Still checks status
    }

    @Test
    void testProcessWorkflow_StatusActive() throws Exception {
        // Given
        Job jobWithAgent = testChildJob.withCursorAgentId("agent-id");
        lenient().when(timeoutManager.getElapsedMillis(jobWithAgent)).thenReturn(100L);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());
        lenient().when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testParentWorkflowData, testPrompts);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.running());
        verify(promptProcessor, never()).processRemainingPrompts(any(), any(), any());
    }

    @Test
    void testProcessWorkflow_StatusSuccessful() throws Exception {
        // Given
        Job jobWithAgent = testChildJob.withCursorAgentId("agent-id");
        lenient().when(timeoutManager.getElapsedMillis(jobWithAgent)).thenReturn(100L);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.finished());
        lenient().when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testParentWorkflowData, testPrompts);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.finished());
        verify(promptProcessor).processRemainingPrompts(eq(jobWithAgent), eq(testPrompts), any(WorkflowData.class));
    }

    @Test
    void testProcessWorkflow_StatusTerminal() throws Exception {
        // Given
        Job jobWithAgent = testChildJob.withCursorAgentId("agent-id");
        lenient().when(timeoutManager.getElapsedMillis(jobWithAgent)).thenReturn(100L);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.error());
        lenient().when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(jobWithAgent, testParentWorkflowData, testPrompts);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.error());
        verify(promptProcessor, never()).processRemainingPrompts(any(), any(), any());
    }

    @Test
    void testProcessWorkflow_StatusCheckException() throws Exception {
        // Given
        Job jobWithAgent = testChildJob.withCursorAgentId("agent-id");
        lenient().when(timeoutManager.getElapsedMillis(jobWithAgent)).thenReturn(100L);
        when(cliAgent.getAgentStatus("agent-id")).thenThrow(new RuntimeException("API error"));

        // When
        handler.processWorkflow(jobWithAgent, testParentWorkflowData, testPrompts);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobWithAgent, AgentState.error());
    }

    @Test
    void testProcessWorkflow_InheritTimeoutFromParent() throws Exception {
        // Given
        Job jobNoTimeout = testChildJob.withTimeoutMillis(null).withWorkflowStartTime(LocalDateTime.now());
        Job jobWithAgent = jobNoTimeout.withCursorAgentId("agent-id");
        when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(jobWithAgent));
        when(timeoutManager.getElapsedMillis(jobWithAgent)).thenReturn(100L);
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());

        // When
        handler.processWorkflow(jobWithAgent, testParentWorkflowData, testPrompts);

        // Then
        verify(timeoutManager).getElapsedMillis(jobWithAgent);
    }

    @Test
    void testProcessWorkflow_JustLaunched() throws Exception {
        // Given
        Job jobWithAgent = testChildJob.withCursorAgentId("agent-id");
        when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(jobWithAgent));

        // When
        handler.processWorkflow(testChildJob, testParentWorkflowData, testPrompts);

        // Then
        verify(cliAgent, never()).getAgentStatus(anyString());
    }

    @Test
    void testProcessWorkflow_Exception() {
        // Given
        // Use a mock that will throw when isParallelWorkflow is called
        WorkflowData mockWorkflowData = mock(WorkflowData.class);
        when(mockWorkflowData.isParallelWorkflow()).thenThrow(new RuntimeException("Error"));

        // When - Should not throw, exception is caught internally
        assertDoesNotThrow(() -> handler.processWorkflow(testChildJob, mockWorkflowData, testPrompts));
    }
}

