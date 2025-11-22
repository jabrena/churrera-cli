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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentLauncher.
 */
@ExtendWith(MockitoExtension.class)
class AgentLauncherTest {

    @Mock
    private CLIAgent cliAgent;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private WorkflowFileService workflowFileService;

    @Mock
    private TimeoutManager timeoutManager;

    @TempDir
    Path tempDir;

    private AgentLauncher agentLauncher;
    private Job testJob;
    private WorkflowData testWorkflowData;

    @BeforeEach
    void setUp() throws IOException {
        agentLauncher = new AgentLauncher(cliAgent, jobRepository, workflowFileService);

        testJob = new Job("job-id", "/path/workflow.xml", null, "model", "repo", AgentState.creating(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        Path workflowPath = tempDir.resolve("workflow.xml");
        Files.createFile(workflowPath);
        Path promptPath = tempDir.resolve("prompt1.pml");
        Files.writeString(promptPath, "test prompt content");

        testWorkflowData = new WorkflowData(
            new PromptInfo("prompt1.pml", "pml"),
            "test-model",
            "test-repo",
            List.of(),
            null, null, null
        );
    }

    @Test
    void testLaunchJobAgent_Success_SequenceWorkflow() {
        // Given
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn("agent-id-123");

        // When
        agentLauncher.launchJobAgent(testJob, testWorkflowData);

        // Then
        verify(cliAgent).launchAgentForJob(eq(testJob), eq("prompt content"), eq("pml"), isNull(), eq(true));
        verify(cliAgent).updateJobCursorIdInDatabase(any(Job.class), eq("agent-id-123"), eq(AgentState.creating()));
        verifyNoInteractions(jobRepository); // No timeout, so no save
    }

    @Test
    void testLaunchJobAgent_Success_ParallelWorkflow() {
        // Given
        ParallelWorkflowData parallelData = mock(ParallelWorkflowData.class);
        WorkflowData parallelWorkflowData = new WorkflowData(
            new PromptInfo("prompt1.pml", "pml"),
            "test-model",
            "test-repo",
            List.of(),
            parallelData, null, null
        );
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn("agent-id-123");

        // When
        agentLauncher.launchJobAgent(testJob, parallelWorkflowData);

        // Then
        verify(cliAgent).launchAgentForJob(eq(testJob), eq("prompt content"), eq("pml"), isNull(), eq(false));
    }

    @Test
    void testLaunchJobAgent_WithBindValue() {
        // Given
        PromptInfo promptWithBind = new PromptInfo("prompt1.pml", "pml", "bindExp");
        WorkflowData workflowWithBind = new WorkflowData(
            promptWithBind,
            "test-model",
            "test-repo",
            List.of(),
            null, null, null
        );
        Job jobWithResult = testJob.withResult("bound-value");
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn("agent-id-123");

        // When
        agentLauncher.launchJobAgent(jobWithResult, workflowWithBind);

        // Then
        verify(cliAgent).launchAgentForJob(jobWithResult, "prompt content", "pml", "bound-value", true);
    }

    @Test
    void testLaunchJobAgent_WithTimeout() throws IOException {
        // Given
        Job jobWithTimeout = testJob.withTimeoutMillis(1000L);
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn("agent-id-123");

        // When
        agentLauncher.launchJobAgent(jobWithTimeout, testWorkflowData);

        // Then
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testLaunchJobAgent_NoBindValue_WhenPromptHasNoBindResultExp() {
        // Given
        Job jobWithResult = testJob.withResult("bound-value");
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn("agent-id-123");

        // When
        agentLauncher.launchJobAgent(jobWithResult, testWorkflowData);

        // Then
        verify(cliAgent).launchAgentForJob(eq(jobWithResult), eq("prompt content"), eq("pml"), isNull(), eq(true));
    }

    @Test
    void testLaunchJobAgent_NoBindValue_WhenJobHasNoResult() {
        // Given
        PromptInfo promptWithBind = new PromptInfo("prompt1.pml", "pml", "bindExp");
        WorkflowData workflowWithBind = new WorkflowData(
            promptWithBind,
            "test-model",
            "test-repo",
            List.of(),
            null, null, null
        );
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenReturn("agent-id-123");

        // When
        agentLauncher.launchJobAgent(testJob, workflowWithBind);

        // Then
        verify(cliAgent).launchAgentForJob(eq(testJob), eq("prompt content"), eq("pml"), isNull(), eq(true));
    }

    @Test
    void testLaunchJobAgent_Exception() {
        // Given
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenThrow(new RuntimeException("File read error"));

        // When
        agentLauncher.launchJobAgent(testJob, testWorkflowData);

        // Then
        verify(cliAgent).updateJobStatusInDatabase(testJob, AgentState.error());
    }

    @Test
    void testLaunchJobAgent_Exception_UpdateStatusFails() {
        // Given
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenThrow(new RuntimeException("File read error"));
        doThrow(new RuntimeException("Update failed")).when(cliAgent).updateJobStatusInDatabase(any(), any());

        // When - Should not throw, just log error
        assertDoesNotThrow(() -> agentLauncher.launchJobAgent(testJob, testWorkflowData));
    }
}

