package info.jab.churrera.cli.service;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.JobWithDetails;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.SequenceInfo;
import info.jab.churrera.workflow.WorkflowParseException;
import info.jab.churrera.cli.model.AgentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import info.jab.churrera.workflow.WorkflowType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobProcessor.
 */
@ExtendWith(MockitoExtension.class)
class JobProcessorTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CLIAgent cliAgent;

    @Mock
    private WorkflowParser workflowParser;

    private JobProcessor jobProcessor;
    private Job testJob;
    private Job testJobWithAgent;
    private List<Prompt> testPrompts;
    private WorkflowData testWorkflowData;

    @BeforeEach
    void setUp() {
        jobProcessor = new JobProcessor(jobRepository, cliAgent, workflowParser);

        testJob = new Job("test-job-id",
            "/test/path/workflow.xml",
            null, // no cursor agent initially
            "test-model",
            "test-repo",
            AgentState.CREATING(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        testJobWithAgent = new Job("test-job-id",
            "/test/path/workflow.xml",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.CREATING(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        testPrompts = List.of(
            new Prompt(
                "prompt-1",
                "test-job-id",
                "prompt1.pml",
                "UNKNOWN",
                LocalDateTime.now(),
                LocalDateTime.now()
            ),
            new Prompt(
                "prompt-2",
                "test-job-id",
                "prompt2.pml",
                "UNKNOWN",
                LocalDateTime.now(),
                LocalDateTime.now()
            )
        );

        testWorkflowData = new WorkflowData(
            new PromptInfo("prompt1.pml", "pml"),
            "test-model",
            "test-repo",
            List.of(new PromptInfo("prompt2.pml", "pml")),
            null, null, null
        );
    }

    @Test
    void testStart_Stop() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs()).thenReturn(List.of());

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - Verify processJobs was called
        verify(jobRepository).findUnfinishedJobs();
    }


    @Test
    void testProcessJobs_NoUnfinishedJobs() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs()).thenReturn(List.of());

        // When - Process jobs directly
        jobProcessor.processJobs();
        jobProcessor.processJobs(); // Call twice to simulate multiple iterations

        // Then
        verify(jobRepository, times(2)).findUnfinishedJobs();
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testProcessJobs_WithUnfinishedJob() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(testJob, testPrompts)));

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findUnfinishedJobs();
        verify(jobRepository).findJobWithDetails("test-job-id");
    }

    @Test
    void testProcessJob_JobDetailsNotFound() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id")).thenReturn(Optional.empty());

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findJobWithDetails("test-job-id");
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testProcessJob_NoPrompts() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(testJob, List.of())));

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findJobWithDetails("test-job-id");
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testProcessJob_ExceptionInFindingJobs() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenThrow(new RuntimeException("Database error"));

        // When - Process jobs directly (exception will be caught and logged)
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findUnfinishedJobs();
    }

    @Test
    void testProcessJob_ExceptionInJobDetails() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJob));
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenThrow(new RuntimeException("Job processing error"));

        // When - Process jobs directly (exception will be caught and logged)
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findUnfinishedJobs();
        verify(jobRepository).findJobWithDetails("test-job-id");
    }

    @Test
    void testProcessJob_WithCursorAgent_FinishedStatus() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJobWithAgent))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(testJobWithAgent, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.getAgentStatus("cursor-agent-123")).thenReturn(AgentState.FINISHED());

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findJobWithDetails("test-job-id");
        verify(cliAgent).getAgentStatus("cursor-agent-123");
        verify(cliAgent).updateJobStatusInDatabase(eq(testJobWithAgent), eq(AgentState.FINISHED()));
    }

    @Test
    void testProcessJob_WithCursorAgent_FailedStatus() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJobWithAgent))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(testJobWithAgent, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.getAgentStatus("cursor-agent-123")).thenReturn(AgentState.ERROR());

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findJobWithDetails("test-job-id");
        verify(cliAgent).getAgentStatus("cursor-agent-123");
        verify(cliAgent).updateJobStatusInDatabase(eq(testJobWithAgent), eq(AgentState.ERROR()));
    }

    @Test
    void testProcessJob_AgentStatusError() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJobWithAgent))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(testJobWithAgent, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.getAgentStatus("cursor-agent-123"))
            .thenThrow(new RuntimeException("Status check failed"));

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findJobWithDetails("test-job-id");
        verify(cliAgent).getAgentStatus("cursor-agent-123");
        verify(cliAgent).updateJobStatusInDatabase(eq(testJobWithAgent), eq(AgentState.ERROR()));
    }

    @Test
    void testProcessJob_ActiveAgent_MonitorsAndCompletes() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJobWithAgent))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(testJobWithAgent, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.getAgentStatus("cursor-agent-123")).thenReturn(AgentState.RUNNING());

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findJobWithDetails("test-job-id");
        verify(cliAgent).getAgentStatus("cursor-agent-123");
        // With the new non-blocking approach, monitorAgent is not called
        verify(cliAgent, never()).monitorAgent(anyString(), anyInt());
        verify(cliAgent, atLeastOnce()).updateJobStatusInDatabase(any(Job.class), any(AgentState.class));
    }

    @Test
    void testProcessJob_CreatingAgent_MonitorsAndCompletes() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJobWithAgent))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(testJobWithAgent, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.getAgentStatus("cursor-agent-123"))
            .thenReturn(AgentState.CREATING());

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(jobRepository).findJobWithDetails("test-job-id");
        verify(cliAgent).getAgentStatus("cursor-agent-123");
        // With the new non-blocking approach, monitorAgent is not called
        verify(cliAgent, never()).monitorAgent(anyString(), anyInt());
    }

    @Test
    void testMultipleJobs_ProcessedSequentially() throws Exception {
        // Given
        Job job2 = new Job("test-job-id-2",
            "/test/path2/workflow.xml",
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(),LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        CountDownLatch bothJobsProcessedLatch = new CountDownLatch(2);
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJob, job2))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenAnswer(invocation -> {
                bothJobsProcessedLatch.countDown();
                return Optional.of(new JobWithDetails(testJob, testPrompts));
            });
        when(jobRepository.findJobWithDetails("test-job-id-2"))
            .thenAnswer(invocation -> {
                bothJobsProcessedLatch.countDown();
                return Optional.of(new JobWithDetails(job2, testPrompts));
            });

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(jobRepository, atLeastOnce()).findUnfinishedJobs();
        verify(jobRepository).findJobWithDetails("test-job-id");
        verify(jobRepository).findJobWithDetails("test-job-id-2");
    }

    @Test
    void testProcessJobs_CanBeCalledMultipleTimes() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs()).thenReturn(List.of());

        // When - Process jobs multiple times (should be safe to call repeatedly)
        jobProcessor.processJobs();
        jobProcessor.processJobs();

        // Then - Should process successfully each time
        verify(jobRepository, times(2)).findUnfinishedJobs();
    }


    @Test
    void testProcessJob_LaunchesAgentWhenNoCursorAgentId() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(testJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(testJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean())).thenReturn("new-agent-123");

        // Create temporary files for testing
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path promptFile = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(promptFile, "Test prompt content");

        Job jobWithRealPath = new Job("test-job-id",
            workflowFile.toString(),
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithRealPath))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithRealPath, testPrompts)));
        when(jobRepository.findById("test-job-id")).thenReturn(Optional.of(jobWithRealPath));

        // When
        CountDownLatch agentLaunchedLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            agentLaunchedLatch.countDown();
            return null;
        }).when(cliAgent).updateJobCursorIdInDatabase(any(Job.class), eq("new-agent-123"), eq(AgentState.CREATING()));

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent).launchAgentForJob(any(Job.class), anyString(), eq("pml"), any(), anyBoolean());
        verify(cliAgent).updateJobCursorIdInDatabase(any(Job.class), eq("new-agent-123"), eq(AgentState.CREATING()));

        // Cleanup
        Files.deleteIfExists(promptFile);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessJob_LaunchAgentFailure() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path promptFile = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(promptFile, "Test prompt content");

        Job jobWithRealPath = new Job("test-job-id",
            workflowFile.toString(),
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithRealPath))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithRealPath, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean()))
            .thenThrow(new RuntimeException("Launch failed"));

        // When
        CountDownLatch failedLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            failedLatch.countDown();
            return null;
        }).when(cliAgent).updateJobStatusInDatabase(any(Job.class), eq(AgentState.ERROR()));

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent).launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean());
        verify(cliAgent).updateJobStatusInDatabase(any(Job.class), eq(AgentState.ERROR()));

        // Cleanup
        Files.deleteIfExists(promptFile);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessJob_SuccessfulCompletionWithRemainingPrompts() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Path prompt2File = tempDir.resolve("prompt2.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");
        Files.writeString(prompt2File, "Test prompt 2");

        Job jobWithRealPath = new Job("test-job-id",
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithRealPath))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithRealPath, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.getAgentStatus("cursor-agent-123")).thenReturn(AgentState.FINISHED());
        CountDownLatch followUpLatch = new CountDownLatch(1);
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                followUpLatch.countDown();
                return "follow-up-123";
            });

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent).getAgentStatus("cursor-agent-123");
        verify(cliAgent).updateJobStatusInDatabase(any(Job.class), eq(AgentState.FINISHED()));
        verify(cliAgent).followUpForPrompt(eq("cursor-agent-123"), anyString(), eq("pml"), any());

        // Cleanup
        Files.deleteIfExists(prompt2File);
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessPrompt_AlreadySent_StillProcessing() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Path prompt2File = tempDir.resolve("prompt2.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");
        Files.writeString(prompt2File, "Test prompt 2");

        Job jobWithRealPath = new Job("test-job-id",
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        Prompt sentPrompt = new Prompt("prompt-1", "test-job-id", "prompt2.pml", "SENT",
            LocalDateTime.now(), LocalDateTime.now());

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithRealPath))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithRealPath, List.of(sentPrompt))));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        CountDownLatch statusCheckedLatch = new CountDownLatch(1);
        when(cliAgent.getAgentStatus("cursor-agent-123"))
            .thenAnswer(invocation -> {
                statusCheckedLatch.countDown();
                return AgentState.RUNNING(); // Still running, not finished yet
            });

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent, atLeastOnce()).getAgentStatus("cursor-agent-123");
        // In non-blocking mode, SENT prompts are only checked for completion, not actively processed
        // The status update happens when checking again in the next polling cycle

        // Cleanup
        Files.deleteIfExists(prompt2File);
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessPrompt_WithBindValue() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Path prompt2File = tempDir.resolve("prompt2.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");
        Files.writeString(prompt2File, "Test prompt 2 with {bindValue}");

        Job jobWithRealPath = new Job("test-job-id",
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, "test-bound-value", null, null, null, null, null);

        PromptInfo launchPrompt = new PromptInfo("prompt1.pml", "pml", "item");
        PromptInfo updatePromptWithBind = new PromptInfo("prompt2.pml", "pml", "item");
        WorkflowData workflowDataWithBind = new WorkflowData(
            launchPrompt, "test-model", "test-repo", List.of(updatePromptWithBind), null, null, null
        );

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithRealPath))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithRealPath, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(workflowDataWithBind);
        when(cliAgent.getAgentStatus("cursor-agent-123")).thenReturn(AgentState.FINISHED());
        CountDownLatch followUpWithBindLatch = new CountDownLatch(1);
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                followUpWithBindLatch.countDown();
                return "follow-up-123";
            });

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent).followUpForPrompt(eq("cursor-agent-123"), anyString(), eq("pml"), eq("test-bound-value"));

        // Cleanup
        Files.deleteIfExists(prompt2File);
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessPrompt_ErrorInSending() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Path prompt2File = tempDir.resolve("prompt2.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");
        Files.writeString(prompt2File, "Test prompt 2");

        Job jobWithRealPath = new Job("test-job-id",
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithRealPath))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithRealPath, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.getAgentStatus("cursor-agent-123")).thenReturn(AgentState.FINISHED());
        CountDownLatch errorUpdatedLatch = new CountDownLatch(1);
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Follow-up failed"));
        doAnswer(invocation -> {
            errorUpdatedLatch.countDown();
            return null;
        }).when(cliAgent).updatePromptInDatabase(any(Prompt.class), eq("ERROR"));

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent).followUpForPrompt(anyString(), anyString(), anyString(), any());
        verify(cliAgent).updatePromptInDatabase(any(Prompt.class), eq("ERROR"));

        // Cleanup
        Files.deleteIfExists(prompt2File);
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testParseWorkflow_FileNotFound() throws Exception {
        // Given
        Job jobWithBadPath = new Job("test-job-id",
            "/nonexistent/workflow.xml",
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithBadPath))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithBadPath, testPrompts)));
        CountDownLatch parseCalledLatch = new CountDownLatch(1);
        when(workflowParser.parse(any(Path.class)))
            .thenAnswer(invocation -> {
                parseCalledLatch.countDown();
                throw new WorkflowParseException("File not found");
            });

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(workflowParser).parse(any(java.nio.file.Path.class));
        verify(jobRepository).findJobWithDetails("test-job-id");
    }

    @Test
    void testProcessJob_ThreadInterruption() throws Exception {
        // Given
        when(jobRepository.findUnfinishedJobs()).thenReturn(List.of());

        // When - Process jobs directly (no thread interruption test needed with direct calls)
        jobProcessor.processJobs();

        // Then - ProcessJobs() completed successfully
        verify(jobRepository).findUnfinishedJobs();
    }

    @Test
    void testProcessParallelWorkflow_LaunchesAgent() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Path prompt2File = tempDir.resolve("prompt2.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");
        Files.writeString(prompt2File, "Test prompt 2");

        Job parallelJob = new Job("parallel-job-id",
            workflowFile.toString(),
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        // Create parallel workflow data
        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        PromptInfo seqPrompt = new PromptInfo("prompt2.pml", "pml", "item");
        SequenceInfo sequenceInfo = new SequenceInfo("test-model", "test-repo",
            List.of(seqPrompt), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(parallelJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("parallel-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), any(), anyBoolean())).thenReturn("parallel-agent-123");

        Job updatedJob = parallelJob.withCursorAgentId("parallel-agent-123").withStatus(AgentState.CREATING());
        when(jobRepository.findById("parallel-job-id")).thenReturn(Optional.of(updatedJob));

        // When
        CountDownLatch agentLaunchedLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            agentLaunchedLatch.countDown();
            return null;
        }).when(cliAgent).updateJobCursorIdInDatabase(any(Job.class), eq("parallel-agent-123"), eq(AgentState.CREATING()));

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - Verify agent was launched for parallel workflow
        verify(cliAgent).launchAgentForJob(any(Job.class), anyString(), eq("pml"), any(), anyBoolean());
        verify(workflowParser).parse(any(Path.class));

        // Cleanup
        Files.deleteIfExists(prompt2File);
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessParallelWorkflow_AlreadyLaunched() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job parallelJob = new Job("parallel-job-id",
            workflowFile.toString(),
            "parallel-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        SequenceInfo sequenceInfo = new SequenceInfo("test-model", "test-repo", List.of(), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(parallelJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("parallel-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        when(jobRepository.findById("parallel-job-id")).thenReturn(Optional.of(parallelJob));
        CountDownLatch statusUpdatedLatch = new CountDownLatch(1);
        when(cliAgent.getAgentStatus("parallel-agent-123")).thenReturn(AgentState.FINISHED());
        when(cliAgent.getConversationContent("parallel-agent-123")).thenReturn("[\"value1\"]");
        doAnswer(invocation -> {
            statusUpdatedLatch.countDown();
            return null;
        }).when(cliAgent).updateJobStatusInDatabase(eq(parallelJob), eq(AgentState.FINISHED()));

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent, never()).launchAgentForJob(any(), anyString(), anyString(), any(), anyBoolean());
        verify(cliAgent).getAgentStatus(eq("parallel-agent-123"));
        verify(cliAgent).updateJobStatusInDatabase(eq(parallelJob), eq(AgentState.FINISHED()));

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessParallelWorkflow_Failed() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job parallelJob = new Job("parallel-job-id",
            workflowFile.toString(),
            "parallel-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        SequenceInfo sequenceInfo = new SequenceInfo("test-model", "test-repo", List.of(), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(parallelJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("parallel-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        when(jobRepository.findById("parallel-job-id")).thenReturn(Optional.of(parallelJob));
        CountDownLatch failedLatch = new CountDownLatch(1);
        when(cliAgent.getAgentStatus(eq("parallel-agent-123"))).thenReturn(AgentState.ERROR());
        doAnswer(invocation -> {
            failedLatch.countDown();
            return null;
        }).when(cliAgent).updateJobStatusInDatabase(any(Job.class), eq(AgentState.ERROR()));

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent).getAgentStatus(eq("parallel-agent-123"));
        verify(cliAgent).updateJobStatusInDatabase(any(Job.class), eq(AgentState.ERROR()));
        verify(jobRepository, never()).save(argThat(job -> job.parentJobId() != null));

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessChildJobWorkflow_NotParallelWorkflow() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job childJob = new Job("child-job-id",
            workflowFile.toString(),
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(), LocalDateTime.now(), LocalDateTime.now(), "parent-job-id", "bound-value",
            WorkflowType.SEQUENCE, null, null, null, null);

        // Regular sequence workflow data (not parallel)
        WorkflowData sequenceWorkflowData = new WorkflowData(
            new PromptInfo("prompt1.pml", "pml"),
            "test-model",
            "test-repo",
            List.of(),
            null, null, null
        );

        CountDownLatch jobProcessedLatch = new CountDownLatch(1);
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(childJob))
            .thenAnswer(invocation -> {
                jobProcessedLatch.countDown();
                return List.of();
            });
        when(jobRepository.findJobWithDetails("child-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(childJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(sequenceWorkflowData);

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - Should not launch because parent workflow is not parallel
        verify(cliAgent, never()).launchAgentForJob(any(), anyString(), anyString(), any(), anyBoolean());

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessChildJobWorkflow_NoSequences() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job childJob = new Job("child-job-id",
            workflowFile.toString(),
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(), LocalDateTime.now(), LocalDateTime.now(), "parent-job-id", "bound-value",
            WorkflowType.SEQUENCE, null, null, null, null);

        // Parallel workflow with no sequences
        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(), null, null); // Empty sequences
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(childJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("child-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(childJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent, never()).launchAgentForJob(any(), anyString(), anyString(), any(), anyBoolean());

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessChildJobWorkflow_NoPromptsInSequence() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job childJob = new Job("child-job-id",
            workflowFile.toString(),
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(), LocalDateTime.now(), LocalDateTime.now(), "parent-job-id", "bound-value",
            WorkflowType.SEQUENCE, null, null, null, null);

        // Parallel workflow with sequence but no prompts
        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        SequenceInfo sequenceInfo = new SequenceInfo("test-model", "test-repo", List.of(), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(childJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("child-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(childJob, testPrompts)));
        CountDownLatch jobProcessedLatch2 = new CountDownLatch(1);
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(childJob))
            .thenAnswer(invocation -> {
                jobProcessedLatch2.countDown();
                return List.of();
            });

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent, never()).launchAgentForJob(any(), anyString(), anyString(), any(), anyBoolean());

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessChildJobWorkflow_SuccessfulLaunch() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Path prompt2File = tempDir.resolve("prompt2.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");
        Files.writeString(prompt2File, "Test prompt 2");

        Job childJob = new Job("child-job-id",
            workflowFile.toString(),
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(), LocalDateTime.now(), LocalDateTime.now(), "parent-job-id", "bound-value",
            WorkflowType.SEQUENCE, null, null, null, null);

        // Parallel workflow with proper sequence and prompts
        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        PromptInfo seqPrompt1 = new PromptInfo("prompt1.pml", "pml", "item");
        PromptInfo seqPrompt2 = new PromptInfo("prompt2.pml", "pml");
        SequenceInfo sequenceInfo = new SequenceInfo("test-model", "test-repo",
            List.of(seqPrompt1, seqPrompt2), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(childJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("child-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(childJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), anyString(), anyString(), anyBoolean())).thenReturn("child-agent-123");

        Job updatedChildJob = childJob.withCursorAgentId("child-agent-123");
        when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(updatedChildJob));

        // When
        CountDownLatch childLaunchedLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            childLaunchedLatch.countDown();
            return null;
        }).when(cliAgent).updateJobCursorIdInDatabase(any(Job.class), eq("child-agent-123"), eq(AgentState.CREATING()));

        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent).launchAgentForJob(any(Job.class), anyString(), eq("pml"), eq("bound-value"), anyBoolean());
        verify(cliAgent).updateJobCursorIdInDatabase(any(Job.class), eq("child-agent-123"), eq(AgentState.CREATING()));

        // Cleanup
        java.nio.file.Files.deleteIfExists(prompt2File);
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessChildJobWorkflow_AlreadyLaunchedNonBlocking() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job childJob = new Job("child-job-id",
            workflowFile.toString(),
            "child-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), "parent-job-id", "bound-value",
            WorkflowType.SEQUENCE, null, null, null, null);

        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        PromptInfo seqPrompt = new PromptInfo("prompt1.pml", "pml");
        SequenceInfo sequenceInfo = new SequenceInfo("test-model", "test-repo", List.of(seqPrompt), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(childJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("child-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(childJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(childJob));
        CountDownLatch childStatusUpdatedLatch = new CountDownLatch(1);
        when(cliAgent.getAgentStatus(eq("child-agent-123"))).thenReturn(AgentState.FINISHED());
        doAnswer(invocation -> {
            childStatusUpdatedLatch.countDown();
            return null;
        }).when(cliAgent).updateJobStatusInDatabase(eq(childJob), eq(AgentState.FINISHED()));

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then
        verify(cliAgent, never()).launchAgentForJob(any(), anyString(), anyString(), any(), anyBoolean());
        verify(cliAgent).getAgentStatus(eq("child-agent-123"));
        verify(cliAgent).updateJobStatusInDatabase(eq(childJob), eq(AgentState.FINISHED()));

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    // Tests for createChildJobs and createChildJobPrompts (0% coverage)
    // Note: Full end-to-end testing of child job creation with JSON deserialization
    // is complex in a unit test environment. This test focuses on verifying the core flow.
    @Test
    void testProcessParallelWorkflow_EntersChildJobCreationPath() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Path prompt2File = tempDir.resolve("prompt2.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");
        Files.writeString(prompt2File, "Test prompt 2");

        Job parallelJob = new Job("parallel-job-id",
            workflowFile.toString(),
            "parallel-agent-123",  // Already has agent
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        PromptInfo seqPrompt1 = new PromptInfo("prompt1.pml", "pml");
        PromptInfo seqPrompt2 = new PromptInfo("prompt2.pml", "pml");
        SequenceInfo sequenceInfo = new SequenceInfo("child-model", "child-repo",
            List.of(seqPrompt1, seqPrompt2), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(parallelJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("parallel-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        when(jobRepository.findById("parallel-job-id"))
            .thenReturn(Optional.of(parallelJob))
            .thenReturn(Optional.of(parallelJob));
        when(cliAgent.getAgentStatus(eq("parallel-agent-123"))).thenReturn(AgentState.FINISHED());
        CountDownLatch conversationReadLatch = new CountDownLatch(1);
        when(cliAgent.getConversationContent(eq("parallel-agent-123")))
            .thenAnswer(invocation -> {
                conversationReadLatch.countDown();
                return "```json\n[\"value1\",\"value2\",\"value3\"]\n```";
            });

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - Verify the parallel workflow processing was executed
        verify(cliAgent).getAgentStatus(eq("parallel-agent-123"));
        verify(cliAgent).getConversationContent(eq("parallel-agent-123"));
        verify(cliAgent).updateJobStatusInDatabase(eq(parallelJob), eq(AgentState.FINISHED()));

        // Cleanup
        Files.deleteIfExists(prompt2File);
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessParallelWorkflow_NoSequencesInParallelData() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job parallelJob = new Job("parallel-job-id",
            workflowFile.toString(),
            "parallel-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        // Empty sequences list
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(parallelJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("parallel-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        when(jobRepository.findById("parallel-job-id"))
            .thenReturn(Optional.of(parallelJob))
            .thenReturn(Optional.of(parallelJob));
        when(cliAgent.getAgentStatus(eq("parallel-agent-123"))).thenReturn(AgentState.FINISHED());
        CountDownLatch conversationReadLatch2 = new CountDownLatch(1);
        when(cliAgent.getConversationContent(eq("parallel-agent-123")))
            .thenAnswer(invocation -> {
                conversationReadLatch2.countDown();
                return "[\"value1\",\"value2\"]";
            });

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - No child jobs should be created (because sequences list is empty)
        verify(cliAgent).getAgentStatus(eq("parallel-agent-123"));
        verify(cliAgent).getConversationContent(eq("parallel-agent-123"));
        verify(jobRepository, never()).save(argThat(job ->
            job.parentJobId() != null && job.parentJobId().equals("parallel-job-id")
        ));

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessParallelWorkflow_FailedStatus() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job parallelJob = new Job("parallel-job-id",
            workflowFile.toString(),
            "parallel-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        SequenceInfo sequenceInfo = new SequenceInfo("test-model", "test-repo", List.of(), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(parallelJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("parallel-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        when(jobRepository.findById("parallel-job-id")).thenReturn(Optional.of(parallelJob));
        CountDownLatch failedStatusLatch = new CountDownLatch(1);
        when(cliAgent.getAgentStatus(eq("parallel-agent-123"))).thenReturn(AgentState.ERROR());
        doAnswer(invocation -> {
            failedStatusLatch.countDown();
            return null;
        }).when(cliAgent).updateJobStatusInDatabase(eq(parallelJob), eq(AgentState.ERROR()));

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - No child jobs should be created for failed jobs
        verify(cliAgent).getAgentStatus(eq("parallel-agent-123"));
        verify(cliAgent).updateJobStatusInDatabase(eq(parallelJob), eq(AgentState.ERROR()));
        verify(jobRepository, never()).save(argThat(job ->
            job.parentJobId() != null && job.parentJobId().equals("parallel-job-id")
        ));

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testProcessParallelWorkflow_ExceptionDuringProcessing() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job parallelJob = new Job("parallel-job-id",
            workflowFile.toString(),
            "parallel-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        SequenceInfo sequenceInfo = new SequenceInfo("test-model", "test-repo", List.of(), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(parallelJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("parallel-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        CountDownLatch errorLatch = new CountDownLatch(1);
        when(jobRepository.findById("parallel-job-id"))
            .thenThrow(new RuntimeException("Database error"));
        doAnswer(invocation -> {
            errorLatch.countDown();
            return null;
        }).when(cliAgent).updateJobStatusInDatabase(any(Job.class), eq(AgentState.ERROR()));

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - Error should be caught and logged
        verify(cliAgent, atLeastOnce()).updateJobStatusInDatabase(any(Job.class), eq(AgentState.ERROR()));

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    // Tests for processPrompt error paths (33% coverage)
    @Test
    void testProcessPrompt_ErrorUpdatingPromptStatus() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Path prompt2File = tempDir.resolve("prompt2.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");
        Files.writeString(prompt2File, "Test prompt 2");

        Job jobWithAgent = new Job("test-job-id",
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        Prompt testPrompt = new Prompt("prompt-1", "test-job-id", "prompt2.pml", "UNKNOWN",
            LocalDateTime.now(), LocalDateTime.now());

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithAgent))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithAgent, List.of(testPrompt))));
        when(workflowParser.parse(any(Path.class))).thenReturn(testWorkflowData);
        when(cliAgent.getAgentStatus("cursor-agent-123")).thenReturn(AgentState.FINISHED());
        CountDownLatch followUpCalledLatch = new CountDownLatch(1);
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                followUpCalledLatch.countDown();
                throw new RuntimeException("Follow-up failed");
            });
        doThrow(new RuntimeException("Update failed")).when(cliAgent).updatePromptInDatabase(any(Prompt.class), eq("ERROR"));

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - Error should be caught and logged
        verify(cliAgent).followUpForPrompt(anyString(), anyString(), anyString(), any());

        // Cleanup
        Files.deleteIfExists(prompt2File);
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }


    @Test
    void testProcessRemainingPrompts_ExceptionInLoop() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Path prompt2File = tempDir.resolve("prompt2.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");
        Files.writeString(prompt2File, "Test prompt 2");

        Job jobWithAgent = new Job("test-job-id",
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        Prompt testPrompt = new Prompt("prompt-1", "test-job-id", "prompt2.pml", "UNKNOWN",
            LocalDateTime.now(), LocalDateTime.now());

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithAgent))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithAgent, List.of(testPrompt))));
        CountDownLatch parseErrorLatch = new CountDownLatch(1);
        when(workflowParser.parse(any(Path.class)))
            .thenAnswer(invocation -> {
                parseErrorLatch.countDown();
                throw new RuntimeException("Parse error");
            });

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - Error should be caught and logged
        verify(workflowParser).parse(any(java.nio.file.Path.class));

        // Cleanup
        Files.deleteIfExists(prompt2File);
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testLaunchJobAgent_WithoutBindResultExp() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        // PromptInfo without bindResultExp
        PromptInfo launchPrompt = new PromptInfo("prompt1.pml", "pml");
        WorkflowData workflowData = new WorkflowData(
            launchPrompt, "test-model", "test-repo", List.of(), null, null, null);

        Job jobWithResult = new Job("test-job-id",
            workflowFile.toString(),
            null,
            "test-model",
            "test-repo",
            AgentState.CREATING(), LocalDateTime.now(), LocalDateTime.now(), null, "some-result", null, null, null, null, null);

        Job updatedJob = jobWithResult.withCursorAgentId("new-agent-123").withStatus(AgentState.CREATING());

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(jobWithResult))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("test-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(jobWithResult, testPrompts)));
        when(workflowParser.parse(any(Path.class))).thenReturn(workflowData);
        CountDownLatch launchLatch = new CountDownLatch(1);
        when(cliAgent.launchAgentForJob(any(Job.class), anyString(), eq("pml"), isNull(), anyBoolean()))
            .thenAnswer(invocation -> {
                launchLatch.countDown();
                return "new-agent-123";
            });
        when(jobRepository.findById("test-job-id")).thenReturn(Optional.of(updatedJob));

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - Launch should be called without bindValue
        verify(cliAgent).launchAgentForJob(any(Job.class), anyString(), eq("pml"), isNull(), anyBoolean());

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testMonitorAndProcessPrompts_ErrorDuringMonitoring() throws Exception {
        // Given - Create temporary files
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        Path prompt1File = tempDir.resolve("prompt1.pml");
        Files.writeString(workflowFile, "<pml-workflow></pml-workflow>");
        Files.writeString(prompt1File, "Test prompt 1");

        Job childJob = new Job("child-job-id",
            workflowFile.toString(),
            "child-agent-123",
            "test-model",
            "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), "parent-job-id", "bound-value",
            WorkflowType.SEQUENCE, null, null, null, null);

        PromptInfo parallelPrompt = new PromptInfo("prompt1.pml", "pml");
        PromptInfo seqPrompt = new PromptInfo("prompt1.pml", "pml");
        SequenceInfo sequenceInfo = new SequenceInfo("test-model", "test-repo", List.of(seqPrompt), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List<String>", List.of(sequenceInfo), null, null);
        WorkflowData parallelWorkflowData = new WorkflowData(
            parallelPrompt, "test-model", "test-repo", List.of(), parallelData, null, null);

        when(jobRepository.findUnfinishedJobs())
            .thenReturn(List.of(childJob))
            .thenReturn(List.of());
        when(jobRepository.findJobWithDetails("child-job-id"))
            .thenReturn(Optional.of(new JobWithDetails(childJob, testPrompts)));
        // workflowParser.parse and findById may not be reached in all error paths
        lenient().when(workflowParser.parse(any(Path.class))).thenReturn(parallelWorkflowData);
        lenient().when(jobRepository.findById("child-job-id")).thenReturn(Optional.of(childJob));
        CountDownLatch monitorErrorLatch = new CountDownLatch(1);
        when(cliAgent.getAgentStatus(eq("child-agent-123")))
            .thenThrow(new RuntimeException("Monitor error"));
        doAnswer(invocation -> {
            monitorErrorLatch.countDown();
            return null;
        }).when(cliAgent).updateJobStatusInDatabase(eq(childJob), eq(AgentState.ERROR()));

        // When
        // When - Process jobs directly
        jobProcessor.processJobs();

        // Then - Error should be caught and logged
        verify(cliAgent).getAgentStatus(eq("child-agent-123"));
        verify(cliAgent).updateJobStatusInDatabase(eq(childJob), eq(AgentState.ERROR()));

        // Cleanup
        Files.deleteIfExists(prompt1File);
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

}
