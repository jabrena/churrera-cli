package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowType;
import org.basex.core.BaseXException;
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
        parentJob = createJob(parentJobId, null, AgentState.RUNNING());
    }

    @Test
    void shouldReportNotCompletedWhenParentIsStillActive() throws Exception {
        when(jobRepository.findAll()).thenReturn(List.of(parentJob));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getFinalStatus()).isNull();
        assertThat(result.getChildJobs()).isEmpty();
    }

    @Test
    void shouldReportCompletionWhenParentFinishedAndNoChildrenExist() throws Exception {
        parentJob = createJob(parentJobId, null, AgentState.FINISHED());
        when(jobRepository.findAll()).thenReturn(List.of(parentJob));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.FINISHED());
        assertThat(result.getChildJobs()).isEmpty();
    }

    @Test
    void shouldWaitWhenAnyChildIsStillActive() throws Exception {
        parentJob = createJob(parentJobId, null, AgentState.FINISHED());
        Job childRunning = createJob("child-1", parentJobId, AgentState.RUNNING());
        Job childFinished = createJob("child-2", parentJobId, AgentState.FINISHED());
        when(jobRepository.findAll()).thenReturn(List.of(parentJob, childRunning, childFinished));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getFinalStatus()).isNull();
        assertThat(result.getChildJobs()).containsExactlyInAnyOrder(childRunning, childFinished);
    }

    @Test
    void shouldReturnChildFailureStatusWhenAnyChildFails() throws Exception {
        parentJob = createJob(parentJobId, null, AgentState.FINISHED());
        Job childOk = createJob("child-1", parentJobId, AgentState.FINISHED());
        Job childFailed = createJob("child-2", parentJobId, AgentState.ERROR());
        when(jobRepository.findAll()).thenReturn(List.of(parentJob, childOk, childFailed));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.ERROR());
        assertThat(result.getChildJobs()).containsExactlyInAnyOrder(childOk, childFailed);
    }

    @Test
    void shouldUseParentStatusWhenAllChildrenSuccessful() throws Exception {
        parentJob = createJob(parentJobId, null, AgentState.FINISHED());
        Job childOk1 = createJob("child-1", parentJobId, AgentState.FINISHED());
        Job childOk2 = createJob("child-2", parentJobId, AgentState.FINISHED());
        when(jobRepository.findAll()).thenReturn(List.of(parentJob, childOk1, childOk2));

        CompletionCheckResult result = checker.checkCompletion(parentJob, parentJobId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.FINISHED());
        assertThat(result.getChildJobs()).containsExactlyInAnyOrder(childOk1, childOk2);
    }

    @Test
    void shouldPropagateRepositoryErrors() throws Exception {
        parentJob = createJob(parentJobId, null, AgentState.FINISHED());
        when(jobRepository.findAll()).thenThrow(new BaseXException("boom"));

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

