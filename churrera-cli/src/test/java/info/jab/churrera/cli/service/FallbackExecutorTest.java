package info.jab.churrera.cli.service;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.WorkflowData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FallbackExecutor.
 */
@ExtendWith(MockitoExtension.class)
class FallbackExecutorTest {

    @Mock
    private CLIAgent cliAgent;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private WorkflowFileService workflowFileService;

    @Mock
    private AgentLauncher agentLauncher;

    private FallbackExecutor fallbackExecutor;
    private Job testJob;
    private WorkflowData testWorkflowData;

    @BeforeEach
    void setUp() {
        fallbackExecutor = new FallbackExecutor(cliAgent, jobRepository, workflowFileService);

        testJob = new Job("job-id", "/path/workflow.xml", "agent-id", "model", "repo", AgentState.creating(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, 1000L, null, "fallback.pml", null);

        testWorkflowData = new WorkflowData(
            new PromptInfo("launch.pml", "pml"),
            "test-model",
            "test-repo",
            List.of(),
            null, null, null
        );
    }

    @Test
    void testExecuteFallback_TerminalStatus() {
        // Given
        Job terminalJob = testJob.withStatus(AgentState.finished());

        // When
        fallbackExecutor.executeFallback(terminalJob, testWorkflowData, 1000L, 1000L);

        // Then
        verifyNoInteractions(cliAgent);
        verifyNoInteractions(workflowFileService);
    }

    @Test
    void testExecuteFallback_AlreadyExecuted() {
        // Given
        Job jobWithFallbackExecuted = testJob.withFallbackExecuted(true);

        // When
        fallbackExecutor.executeFallback(jobWithFallbackExecuted, testWorkflowData, 1000L, 1000L);

        // Then
        verifyNoInteractions(cliAgent);
        verifyNoInteractions(workflowFileService);
    }

    @Test
    void testExecuteFallback_NoFallbackSrc() {
        // Given
        Job jobNoFallback = testJob.withFallbackSrc(null);

        // When
        fallbackExecutor.executeFallback(jobNoFallback, testWorkflowData, 1000L, 1000L);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobNoFallback, AgentState.error());
        verifyNoInteractions(workflowFileService);
    }

    @Test
    void testExecuteFallback_EmptyFallbackSrc() {
        // Given
        Job jobEmptyFallback = testJob.withFallbackSrc("");

        // When
        fallbackExecutor.executeFallback(jobEmptyFallback, testWorkflowData, 1000L, 1000L);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(jobEmptyFallback, AgentState.error());
        verifyNoInteractions(workflowFileService);
    }

    @Test
    void testExecuteFallback_WithAgentId_AsFollowUp() throws IOException {
        // Given
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("fallback content");
        when(workflowFileService.inferTypeFromExtension("fallback.pml")).thenReturn("pml");
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any())).thenReturn("follow-up-id");

        // When
        fallbackExecutor.executeFallback(testJob, testWorkflowData, 1000L, 1000L);

        // Then
        verify(cliAgent).followUpForPrompt("agent-id", "fallback content", "pml", null);
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testExecuteFallback_NoAgentId_Launch() {
        // Given
        Job jobNoAgent = testJob.withCursorAgentId(null);
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("fallback content");
        when(workflowFileService.inferTypeFromExtension("fallback.pml")).thenReturn("pml");
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn("new-agent-id");

        // When
        fallbackExecutor.executeFallback(jobNoAgent, testWorkflowData, 1000L, 1000L);

        // Then
        verify(cliAgent).launchAgentForJob(eq(jobNoAgent), eq("fallback content"), eq("pml"), isNull(), eq(true));
        verify(cliAgent).updateJobCursorIdInDatabase(any(Job.class), eq("new-agent-id"), eq(AgentState.creating()));
    }

    @Test
    void testExecuteFallback_WithBindValue() {
        // Given
        Job jobWithResult = testJob.withResult("bound-value");
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("fallback content");
        when(workflowFileService.inferTypeFromExtension("fallback.pml")).thenReturn("pml");
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any())).thenReturn("follow-up-id");

        // When
        fallbackExecutor.executeFallback(jobWithResult, testWorkflowData, 1000L, 1000L);

        // Then
        verify(cliAgent).followUpForPrompt("agent-id", "fallback content", "pml", "bound-value");
    }

    @Test
    void testExecuteFallback_WithTimeout() throws IOException {
        // Given
        Job jobNoAgent = testJob.withCursorAgentId(null).withTimeoutMillis(1000L);
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("fallback content");
        when(workflowFileService.inferTypeFromExtension("fallback.pml")).thenReturn("pml");
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn("new-agent-id");

        // When
        fallbackExecutor.executeFallback(jobNoAgent, testWorkflowData, 1000L, 1000L);

        // Then
        verify(jobRepository, atLeast(2)).save(any(Job.class)); // Once for workflowStartTime, once for fallbackExecuted
    }

    @Test
    void testExecuteFallback_Exception() {
        // Given
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenThrow(new RuntimeException("File error"));

        // When
        fallbackExecutor.executeFallback(testJob, testWorkflowData, 1000L, 1000L);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(testJob, AgentState.error());
    }

    @Test
    void testExecuteFallbackForParallelChildren_AlreadyExecuted() {
        // Given
        Job parentJob = testJob.withFallbackExecuted(true);
        ParallelWorkflowData parallelData = mock(ParallelWorkflowData.class);

        // When
        fallbackExecutor.executeFallbackForParallelChildren(parentJob, parallelData);

        // Then
        verifyNoInteractions(cliAgent);
        verifyNoInteractions(workflowFileService);
    }

    @Test
    void testExecuteFallbackForParallelChildren_NoFallbackSrc() {
        // Given
        ParallelWorkflowData parallelData = mock(ParallelWorkflowData.class);
        when(parallelData.getFallbackSrc()).thenReturn(null);

        // When
        fallbackExecutor.executeFallbackForParallelChildren(testJob, parallelData);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(testJob, AgentState.error());
        verifyNoInteractions(workflowFileService);
    }

    @Test
    void testExecuteFallbackForParallelChildren_WithUnfinishedChildren() throws IOException {
        // Given
        ParallelWorkflowData parallelData = mock(ParallelWorkflowData.class);
        when(parallelData.getFallbackSrc()).thenReturn("fallback.pml");
        
        Job child1 = new Job("child-1", "/path", "child-agent-1", "model", "repo", AgentState.creating(),
            LocalDateTime.now(), LocalDateTime.now(), "job-id", null, null, null, null, null, null);
        Job child2 = new Job("child-2", "/path", null, "model", "repo", AgentState.creating(),
            LocalDateTime.now(), LocalDateTime.now(), "job-id", null, null, null, null, null, null);
        
        when(jobRepository.findAll()).thenReturn(List.of(testJob, child1, child2));
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("fallback content");
        when(workflowFileService.inferTypeFromExtension("fallback.pml")).thenReturn("pml");
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any())).thenReturn("follow-up-id");
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn("new-agent-id");

        // When
        fallbackExecutor.executeFallbackForParallelChildren(testJob, parallelData);

        // Then
        verify(cliAgent).followUpForPrompt("child-agent-1", "fallback content", "pml", null);
        verify(cliAgent).launchAgentForJob(eq(child2), eq("fallback content"), eq("pml"), isNull(), eq(true));
        verify(jobRepository, atLeast(3)).save(any(Job.class)); // 2 children + parent
    }

    @Test
    void testExecuteFallbackForParallelChildren_ChildAlreadyExecuted() throws IOException {
        // Given
        ParallelWorkflowData parallelData = mock(ParallelWorkflowData.class);
        when(parallelData.getFallbackSrc()).thenReturn("fallback.pml");
        
        Job child1 = new Job("child-1", "/path", "child-agent-1", "model", "repo", AgentState.creating(),
            LocalDateTime.now(), LocalDateTime.now(), "job-id", null, null, null, null, null, true);
        
        when(jobRepository.findAll()).thenReturn(List.of(testJob, child1));
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("fallback content");
        when(workflowFileService.inferTypeFromExtension("fallback.pml")).thenReturn("pml");

        // When
        fallbackExecutor.executeFallbackForParallelChildren(testJob, parallelData);

        // Then
        verify(cliAgent, never()).followUpForPrompt(anyString(), anyString(), anyString(), any());
        verify(jobRepository).save(any(Job.class)); // Only parent
    }

    @Test
    void testExecuteFallbackForParallelChildren_NoUnfinishedChildren() throws IOException {
        // Given
        ParallelWorkflowData parallelData = mock(ParallelWorkflowData.class);
        when(parallelData.getFallbackSrc()).thenReturn("fallback.pml");
        
        Job finishedChild = new Job("child-1", "/path", "child-agent-1", "model", "repo", AgentState.finished(),
            LocalDateTime.now(), LocalDateTime.now(), "job-id", null, null, null, null, null, null);
        
        when(jobRepository.findAll()).thenReturn(List.of(testJob, finishedChild));
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("fallback content");
        when(workflowFileService.inferTypeFromExtension("fallback.pml")).thenReturn("pml");

        // When
        fallbackExecutor.executeFallbackForParallelChildren(testJob, parallelData);

        // Then
        verify(cliAgent, never()).followUpForPrompt(anyString(), anyString(), anyString(), any());
        verify(cliAgent, never()).launchAgentForJob(any(), anyString(), anyString(), any(), anyBoolean());
        verify(jobRepository).save(any(Job.class)); // Only parent
    }

    @Test
    void testExecuteFallbackForParallelChildren_Exception() {
        // Given
        ParallelWorkflowData parallelData = mock(ParallelWorkflowData.class);
        when(parallelData.getFallbackSrc()).thenReturn("fallback.pml");
        when(jobRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        // When - Should not throw
        assertDoesNotThrow(() -> fallbackExecutor.executeFallbackForParallelChildren(testJob, parallelData));
    }
}

