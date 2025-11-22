package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JobsCommand.
 */
@ExtendWith(MockitoExtension.class)
class JobsCommandTest {

    @Mock
    private JobRepository jobRepository;

    private JobsCommand jobsCommand;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        jobsCommand = new JobsCommand(jobRepository);
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void shouldDisplayNoJobsMessageWhenEmpty() throws Exception {
        // Given
        when(jobRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("No jobs found.");
    }

    @Test
    void shouldDisplayJobsWhenAvailable() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobs = Arrays.asList(
            new Job("job-1", "/path/1", null, "model1", "repo1", AgentState.CREATING(), now, now, null, null, null, null, null, null, null),
            new Job("job-2", "/path/2", null, "model2", "repo2", AgentState.CREATING(), now, now, null, null, null, null, null, null, null)
        );
        when(jobRepository.findAll()).thenReturn(jobs);
        when(jobRepository.findPromptsByJobId("job-1")).thenReturn(Arrays.asList(
            new Prompt("prompt-1", "job-1", "prompt1.xml", "UNKNOWN", now, now)
        ));
        when(jobRepository.findPromptsByJobId("job-2")).thenReturn(Arrays.asList(
            new Prompt("prompt-2", "job-2", "prompt2.xml", "COMPLETED", now, now)
        ));

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Job ID");
        assertThat(output).contains("Prompts");
        assertThat(output).contains("Status");
        assertThat(output).contains("Last update");
        assertThat(output).contains("job-1");
        assertThat(output).contains("job-2");
    }

    @Test
    void shouldHandleRepositoryException() throws Exception {
        // Given
        when(jobRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Error listing jobs: Database error");
    }

    @Test
    void shouldHandleQueryException() throws Exception {
        // Given
        when(jobRepository.findAll()).thenThrow(new RuntimeException("[basex:error] Query error"));

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Error listing jobs:");
    }

    @Test
    void shouldDisplayFinishedJobsWithCompletionTime() throws Exception {
        // Given
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);
        LocalDateTime updatedAt = LocalDateTime.now().minusMinutes(2);

        List<Job> jobs = Arrays.asList(
            new Job("job-finished", "/path/finished", "agent-123", "model1", "repo1",
                AgentState.FINISHED(), createdAt, updatedAt, null, null,
                WorkflowType.SEQUENCE, null, null, null, null)
        );

        when(jobRepository.findAll()).thenReturn(jobs);
        when(jobRepository.findPromptsByJobId("job-finished")).thenReturn(Arrays.asList(
            new Prompt("prompt-1", "job-finished", "prompt1.xml", "COMPLETED", createdAt, updatedAt),
            new Prompt("prompt-2", "job-finished", "prompt2.xml", "COMPLETED", createdAt, updatedAt)
        ));

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("job-fini");
        assertThat(output).contains("FINISHED");
        assertThat(output).contains("min");
        assertThat(output).contains("2/2");
    }

    @Test
    void shouldDisplayJobWithParentJobId() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobs = Arrays.asList(
            new Job("child-job-1", "/path/child", null, "model1", "repo1",
                AgentState.RUNNING(), now, now, "parent-job-123", "item1",
                WorkflowType.SEQUENCE, null, null, null, null)
        );

        when(jobRepository.findAll()).thenReturn(jobs);
        when(jobRepository.findPromptsByJobId("child-job-1")).thenReturn(Arrays.asList(
            new Prompt("prompt-1", "child-job-1", "prompt1.xml", "SENT", now, now)
        ));

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("child-jo");
        assertThat(output).contains("parent-j");
    }

    @Test
    void shouldDisplayJobsWithDifferentTimeFormats() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoMinutesAgo = now.minusMinutes(2);
        LocalDateTime thirtySecondsAgo = now.minusSeconds(30);

        List<Job> jobs = Arrays.asList(
            new Job("job-1h", "/path/1", null, "model1", "repo1",
                AgentState.RUNNING(), oneHourAgo, oneHourAgo, null, null,
                WorkflowType.SEQUENCE, null, null, null, null),
            new Job("job-2m", "/path/2", null, "model2", "repo2",
                AgentState.RUNNING(), twoMinutesAgo, twoMinutesAgo, null, null,
                WorkflowType.SEQUENCE, null, null, null, null),
            new Job("job-30s", "/path/3", null, "model3", "repo3",
                AgentState.RUNNING(), thirtySecondsAgo, thirtySecondsAgo, null, null,
                WorkflowType.SEQUENCE, null, null, null, null)
        );

        when(jobRepository.findAll()).thenReturn(jobs);
        when(jobRepository.findPromptsByJobId("job-1h")).thenReturn(Collections.emptyList());
        when(jobRepository.findPromptsByJobId("job-2m")).thenReturn(Collections.emptyList());
        when(jobRepository.findPromptsByJobId("job-30s")).thenReturn(Collections.emptyList());

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("job-1h");
        assertThat(output).contains("job-2m");
        assertThat(output).contains("job-30s");
    }

    @Test
    void shouldDisplayParallelWorkflowType() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobs = Arrays.asList(
            new Job("parallel-job", "/path/parallel", null, "model1", "repo1",
                AgentState.RUNNING(), now, now, null, null,
                WorkflowType.PARALLEL, null, null, null, null)
        );

        when(jobRepository.findAll()).thenReturn(jobs);
        when(jobRepository.findPromptsByJobId("parallel-job")).thenReturn(Collections.emptyList());

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("parallel");
        assertThat(output).contains("PARALLEL");
    }

    @Test
    void shouldHandleErrorRetrievingJobDetails() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobs = Arrays.asList(
            new Job("error-job", "/path/error", null, "model1", "repo1",
                AgentState.RUNNING(), now, now, null, null, null, null, null, null, null)
        );

        when(jobRepository.findAll()).thenReturn(jobs);
        when(jobRepository.findPromptsByJobId("error-job")).thenThrow(new RuntimeException("Database error"));

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("error-jo");
        assertThat(output).contains("ERROR");
    }

    @Test
    void shouldDisplayJobsWithMixedPromptStatuses() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobs = Arrays.asList(
            new Job("job-mixed", "/path/mixed", "agent-123", "model1", "repo1",
                AgentState.RUNNING(), now, now, null, null,
                WorkflowType.SEQUENCE, null, null, null, null)
        );

        when(jobRepository.findAll()).thenReturn(jobs);
        when(jobRepository.findPromptsByJobId("job-mixed")).thenReturn(Arrays.asList(
            new Prompt("prompt-1", "job-mixed", "prompt1.xml", "COMPLETED", now, now),
            new Prompt("prompt-2", "job-mixed", "prompt2.xml", "SENT", now, now),
            new Prompt("prompt-3", "job-mixed", "prompt3.xml", "PENDING", now, now)
        ));

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("job-mixe");
        assertThat(output).contains("2/3"); // 2 completed or sent out of 3 total
    }

    @Test
    void shouldHandleJobWithNullWorkflowType() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobs = Arrays.asList(
            new Job("job-null-type", "/path/null", null, "model1", "repo1",
                AgentState.RUNNING(), now, now, null, null, null, null, null, null, null)
        );

        when(jobRepository.findAll()).thenReturn(jobs);
        when(jobRepository.findPromptsByJobId("job-null-type")).thenReturn(Collections.emptyList());

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("job-null");
    }

    @Test
    void shouldDisplayJobsWithFailedStatus() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobs = Arrays.asList(
            new Job("failed-job", "/path/failed", "agent-123", "model1", "repo1",
                AgentState.ERROR(), now, now, null, null,
                WorkflowType.SEQUENCE, null, null, null, null)
        );

        when(jobRepository.findAll()).thenReturn(jobs);
        when(jobRepository.findPromptsByJobId("failed-job")).thenReturn(Arrays.asList(
            new Prompt("prompt-1", "failed-job", "prompt1.xml", "COMPLETED", now, now)
        ));

        // When
        jobsCommand.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("failed-j");
        assertThat(output).contains("ERROR");
    }
}
