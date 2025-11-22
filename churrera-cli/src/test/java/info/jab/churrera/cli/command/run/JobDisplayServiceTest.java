package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowType;
import org.basex.core.BaseXException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobDisplayServiceTest {

    @Mock
    private JobRepository jobRepository;

    private JobDisplayService jobDisplayService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);
        jobDisplayService = new JobDisplayService(jobRepository, fixedClock);
    }

    @Test
    void shouldRenderSequenceJobWithActiveTiming() throws Exception {
        String jobId = "1234567890";
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 11, 0);
        Job job = new Job(
            jobId,
            "/tmp/workflow.xml",
            null,
            "model",
            "repo",
            AgentState.RUNNING(),
            createdAt,
            createdAt,
            null,
            null,
            WorkflowType.SEQUENCE,
            null,
            null,
            null,
            null
        );
        List<Prompt> prompts = List.of(
            new Prompt("prompt-1", jobId, "prompt1.xml", "COMPLETED", createdAt, createdAt),
            new Prompt("prompt-2", jobId, "prompt2.xml", "SENT", createdAt, createdAt)
        );

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findPromptsByJobId(jobId)).thenReturn(prompts);

        String output = captureStdout(() -> jobDisplayService.displayFilteredJobsTable(jobId));

        assertThat(output)
            .contains("12345678")
            .contains("1/2")
            .contains("Started 1 hour ago");
    }

    @Test
    void shouldHandleParallelJobsAndPromptErrors() throws Exception {
        String parentId = "parent-job";
        String childId = "child-job";
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 10, 55);
        Job parentJob = new Job(
            parentId,
            "/tmp/workflow.xml",
            null,
            "model",
            "repo",
            AgentState.FINISHED(),
            createdAt,
            createdAt.plusMinutes(5),
            null,
            null,
            WorkflowType.PARALLEL,
            null,
            null,
            null,
            null
        );
        Job childJob = new Job(
            childId,
            "/tmp/child.xml",
            null,
            "model",
            "repo",
            AgentState.RUNNING(),
            createdAt,
            createdAt.plusMinutes(3),
            parentId,
            null,
            WorkflowType.SEQUENCE,
            null,
            null,
            null,
            null
        );

        when(jobRepository.findById(parentId)).thenReturn(Optional.of(parentJob));
        when(jobRepository.findAll()).thenReturn(List.of(parentJob, childJob));
        when(jobRepository.findPromptsByJobId(parentId))
            .thenReturn(List.of(new Prompt("prompt-1", parentId, "prompt1.xml", "COMPLETED", createdAt, createdAt)));
        doThrow(new BaseXException("boom")).when(jobRepository).findPromptsByJobId(childId);

        String output = captureStdout(() -> jobDisplayService.displayFilteredJobsTable(parentId));

        assertThat(output)
            .contains("parent-j")
            .contains("ERROR");
    }

    private String captureStdout(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream wrapper = new PrintStream(buffer);
        System.setOut(wrapper);
        try {
            action.run();
        } finally {
            System.setOut(originalOut);
        }
        return buffer.toString();
    }
}

