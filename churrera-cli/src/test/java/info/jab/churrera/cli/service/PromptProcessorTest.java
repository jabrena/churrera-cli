package info.jab.churrera.cli.service;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.WorkflowData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PromptProcessor.
 */
@ExtendWith(MockitoExtension.class)
class PromptProcessorTest {

    @Mock
    private CLIAgent cliAgent;

    @Mock
    private WorkflowFileService workflowFileService;

    private PromptProcessor promptProcessor;
    private Job testJob;
    private Prompt testPrompt;
    private WorkflowData testWorkflowData;

    @BeforeEach
    void setUp() {
        promptProcessor = new PromptProcessor(cliAgent, workflowFileService);

        testJob = new Job("job-id", "/path/workflow.xml", "agent-id", "model", "repo", AgentState.creating(),
            LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        testPrompt = new Prompt("prompt-1", "job-id", "prompt1.pml", "UNKNOWN",
            LocalDateTime.now(), LocalDateTime.now());

        testWorkflowData = new WorkflowData(
            new PromptInfo("launch.pml", "pml"),
            "test-model",
            "test-repo",
            List.of(new PromptInfo("prompt1.pml", "pml")),
            null, null, null
        );
    }

    @Test
    void testProcessRemainingPrompts_NoPrompts() {
        // Given
        List<Prompt> emptyPrompts = List.of();

        // When
        promptProcessor.processRemainingPrompts(testJob, emptyPrompts, testWorkflowData);

        // Then
        verifyNoInteractions(cliAgent);
        verifyNoInteractions(workflowFileService);
    }

    @Test
    void testProcessRemainingPrompts_WithUnknownPrompt() {
        // Given
        List<Prompt> prompts = List.of(testPrompt);
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any())).thenReturn("follow-up-id");

        // When
        promptProcessor.processRemainingPrompts(testJob, prompts, testWorkflowData);

        // Then
        verify(workflowFileService).readPromptFile("/path/workflow.xml", "prompt1.pml");
        verify(cliAgent).followUpForPrompt("agent-id", "prompt content", "pml", null);
        verify(cliAgent).updatePromptInDatabase(testPrompt, "SENT");
    }

    @Test
    void testProcessRemainingPrompts_WithSentPrompt() {
        // Given
        Prompt sentPrompt = testPrompt.withStatus("SENT");
        List<Prompt> prompts = List.of(sentPrompt);
        // processRemainingPrompts only processes UNKNOWN prompts, not SENT ones

        // When
        promptProcessor.processRemainingPrompts(testJob, prompts, testWorkflowData);

        // Then
        verify(cliAgent, never()).getAgentStatus(anyString());
        verify(cliAgent, never()).updatePromptInDatabase(any(), anyString());
    }


    @Test
    void testProcessRemainingPrompts_Exception() {
        // Given
        List<Prompt> prompts = List.of(testPrompt);
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenThrow(new RuntimeException("File error"));

        // When
        promptProcessor.processRemainingPrompts(testJob, prompts, testWorkflowData);

        // Then
        verify(cliAgent).updatePromptInDatabase(testPrompt, "ERROR");
    }

    @Test
    void testProcessPrompt_UnknownStatus() {
        // Given
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any())).thenReturn("follow-up-id");

        // When
        promptProcessor.processPrompt(testJob, testPrompt, new PromptInfo("prompt1.pml", "pml"));

        // Then
        verify(cliAgent).followUpForPrompt("agent-id", "prompt content", "pml", null);
        verify(cliAgent).updatePromptInDatabase(testPrompt, "SENT");
    }

    @Test
    void testProcessPrompt_WithBindValue() {
        // Given
        PromptInfo promptWithBind = new PromptInfo("prompt1.pml", "pml", "bindExp");
        Job jobWithResult = testJob.withResult("bound-value");
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any())).thenReturn("follow-up-id");

        // When
        promptProcessor.processPrompt(jobWithResult, testPrompt, promptWithBind);

        // Then
        verify(cliAgent).followUpForPrompt("agent-id", "prompt content", "pml", "bound-value");
    }

    @Test
    void testProcessPrompt_NoBindValue_WhenPromptHasNoBindResultExp() {
        // Given
        Job jobWithResult = testJob.withResult("bound-value");
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenReturn("prompt content");
        when(cliAgent.followUpForPrompt(anyString(), anyString(), anyString(), any())).thenReturn("follow-up-id");

        // When
        promptProcessor.processPrompt(jobWithResult, testPrompt, new PromptInfo("prompt1.pml", "pml"));

        // Then
        verify(cliAgent).followUpForPrompt("agent-id", "prompt content", "pml", null);
    }

    @Test
    void testProcessPrompt_SentStatus_Terminal() {
        // Given
        Prompt sentPrompt = testPrompt.withStatus("SENT");
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.finished());

        // When
        promptProcessor.processPrompt(testJob, sentPrompt, new PromptInfo("prompt1.pml", "pml"));

        // Then
        verify(cliAgent).getAgentStatus("agent-id");
        verify(cliAgent).updatePromptInDatabase(sentPrompt, "COMPLETED");
    }

    @Test
    void testProcessPrompt_SentStatus_Failed() {
        // Given
        Prompt sentPrompt = testPrompt.withStatus("SENT");
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.error());

        // When
        promptProcessor.processPrompt(testJob, sentPrompt, new PromptInfo("prompt1.pml", "pml"));

        // Then
        verify(cliAgent).getAgentStatus("agent-id");
        verify(cliAgent).updatePromptInDatabase(sentPrompt, "FAILED");
    }

    @Test
    void testProcessPrompt_SentStatus_StillActive() {
        // Given
        Prompt sentPrompt = testPrompt.withStatus("SENT");
        when(cliAgent.getAgentStatus("agent-id")).thenReturn(AgentState.running());

        // When
        promptProcessor.processPrompt(testJob, sentPrompt, new PromptInfo("prompt1.pml", "pml"));

        // Then
        verify(cliAgent).getAgentStatus("agent-id");
        verify(cliAgent, never()).updatePromptInDatabase(any(), anyString());
    }

    @Test
    void testProcessPrompt_Exception_UpdateFails() {
        // Given
        when(workflowFileService.readPromptFile(anyString(), anyString())).thenThrow(new RuntimeException("File error"));
        doThrow(new RuntimeException("Update failed")).when(cliAgent).updatePromptInDatabase(any(), anyString());

        // When - Should not throw
        assertThatCode(() -> promptProcessor.processPrompt(testJob, testPrompt, new PromptInfo("prompt1.pml", "pml")))
            .doesNotThrowAnyException();
    }
}

