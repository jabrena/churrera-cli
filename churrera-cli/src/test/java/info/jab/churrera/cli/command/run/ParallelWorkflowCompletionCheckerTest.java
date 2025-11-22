package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParallelWorkflowCompletionCheckerTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private ParallelWorkflowCompletionChecker checker;

    private Job parentJob;
    private final String parentJobId = "parent-job";
    private LocalDateTime createdAt;

    @BeforeEach
    void setUp() {
        createdAt = LocalDateTime.now().minusMinutes(5);
        parentJob = createJob(parentJobId, null, AgentState.running());
    }

    @Test
    void shouldReportNotCompletedWhenParentIsStillActive() {
        when(jobRepository.findAll()).thenReturn(List.of(parentJob));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getFinalStatus()).isNull();
        assertThat(result.getChildJobs()).isEmpty();
    }

    @Test
    void shouldReportCompletionWhenParentFinishedAndNoChildrenExist() {
        parentJob = createJob(parentJobId, null, AgentState.finished());
        when(jobRepository.findAll()).thenReturn(List.of(parentJob));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.finished());
        assertThat(result.getChildJobs()).isEmpty();
    }

    @Test
    void shouldWaitWhenAnyChildIsStillActive() {
        parentJob = createJob(parentJobId, null, AgentState.finished());
        Job childRunning = createJob("child-1", parentJobId, AgentState.running());
        Job childFinished = createJob("child-2", parentJobId, AgentState.finished());
        when(jobRepository.findAll()).thenReturn(List.of(parentJob, childRunning, childFinished));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getFinalStatus()).isNull();
        assertThat(result.getChildJobs()).containsExactlyInAnyOrder(childRunning, childFinished);
    }

    @Test
    void shouldReturnChildFailureStatusWhenAnyChildFails() {
        parentJob = createJob(parentJobId, null, AgentState.finished());
        Job childOk = createJob("child-1", parentJobId, AgentState.finished());
        Job childFailed = createJob("child-2", parentJobId, AgentState.error());
        when(jobRepository.findAll()).thenReturn(List.of(parentJob, childOk, childFailed));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.error());
        assertThat(result.getChildJobs()).containsExactlyInAnyOrder(childOk, childFailed);
    }

    @Test
    void shouldUseParentStatusWhenAllChildrenSuccessful() {
        parentJob = createJob(parentJobId, null, AgentState.finished());
        Job childOk1 = createJob("child-1", parentJobId, AgentState.finished());
        Job childOk2 = createJob("child-2", parentJobId, AgentState.finished());
        when(jobRepository.findAll()).thenReturn(List.of(parentJob, childOk1, childOk2));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.finished());
        assertThat(result.getChildJobs()).containsExactlyInAnyOrder(childOk1, childOk2);
    }

    @Test
    void shouldPropagateRepositoryErrors() {
        parentJob = createJob(parentJobId, null, AgentState.finished());
        when(jobRepository.findAll()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> checker.checkCompletion(parentJob, parentJobId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Error retrieving all jobs");
    }

    private Job createJob(String jobId, String parentId, AgentState state) {
        return new Job(
            jobId,
            "/tmp/workflow.xml",
            null,
            "model",
            "repo",
            state,
            createdAt,
            createdAt.plusMinutes(1),
            parentId,
            null,
            WorkflowType.PARALLEL,
            null,
            null,
            null,
            null
        );
    }
}
