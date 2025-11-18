package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CompletionCheckResult class.
 */
class CompletionCheckResultTest {

    @Test
    void shouldCreateCompletedResultWithFinalStatus() {
        // Given
        AgentState finalStatus = AgentState.FINISHED();
        List<Job> childJobs = new ArrayList<>();

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, childJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getChildJobs()).isEmpty();
    }

    @Test
    void shouldCreateNotCompletedResultWithNullStatus() {
        // Given
        List<Job> childJobs = new ArrayList<>();

        // When
        CompletionCheckResult result = new CompletionCheckResult(false, null, childJobs);

        // Then
        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getFinalStatus()).isNull();
        assertThat(result.getChildJobs()).isEmpty();
    }

    @Test
    void shouldCreateCompletedResultWithChildJobs() {
        // Given
        AgentState finalStatus = AgentState.FINISHED();
        Job childJob1 = createTestJob("child-1", "parent-1");
        Job childJob2 = createTestJob("child-2", "parent-1");
        List<Job> childJobs = Arrays.asList(childJob1, childJob2);

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, childJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getChildJobs()).hasSize(2);
        assertThat(result.getChildJobs()).containsExactly(childJob1, childJob2);
    }

    @Test
    void shouldCreateNotCompletedResultWithChildJobs() {
        // Given
        Job childJob1 = createTestJob("child-1", "parent-1");
        Job childJob2 = createTestJob("child-2", "parent-1");
        List<Job> childJobs = Arrays.asList(childJob1, childJob2);

        // When
        CompletionCheckResult result = new CompletionCheckResult(false, null, childJobs);

        // Then
        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getFinalStatus()).isNull();
        assertThat(result.getChildJobs()).hasSize(2);
        assertThat(result.getChildJobs()).containsExactly(childJob1, childJob2);
    }

    @Test
    void shouldCreateCompletedResultWithErrorStatus() {
        // Given
        AgentState finalStatus = AgentState.ERROR();
        List<Job> childJobs = new ArrayList<>();

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, childJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getFinalStatus().isFailed()).isTrue();
    }

    @Test
    void shouldCreateCompletedResultWithExpiredStatus() {
        // Given
        AgentState finalStatus = AgentState.EXPIRED();
        List<Job> childJobs = new ArrayList<>();

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, childJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getFinalStatus().isFailed()).isTrue();
    }

    @Test
    void shouldCreateCompletedResultWithRunningStatus() {
        // Given
        AgentState finalStatus = AgentState.RUNNING();
        List<Job> childJobs = new ArrayList<>();

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, childJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getFinalStatus().isActive()).isTrue();
    }

    @Test
    void shouldCreateCompletedResultWithCreatingStatus() {
        // Given
        AgentState finalStatus = AgentState.CREATING();
        List<Job> childJobs = new ArrayList<>();

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, childJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getFinalStatus().isActive()).isTrue();
    }

    @Test
    void shouldCreateResultWithEmptyChildJobsList() {
        // Given
        AgentState finalStatus = AgentState.FINISHED();
        List<Job> emptyChildJobs = new ArrayList<>();

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, emptyChildJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getChildJobs()).isEmpty();
    }

    @Test
    void shouldCreateResultWithMultipleChildJobs() {
        // Given
        AgentState finalStatus = AgentState.FINISHED();
        Job childJob1 = createTestJob("child-1", "parent-1");
        Job childJob2 = createTestJob("child-2", "parent-1");
        Job childJob3 = createTestJob("child-3", "parent-1");
        List<Job> childJobs = Arrays.asList(childJob1, childJob2, childJob3);

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, childJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getChildJobs()).hasSize(3);
        assertThat(result.getChildJobs()).containsExactly(childJob1, childJob2, childJob3);
    }

    @Test
    void shouldCreateNotCompletedResultWithChildJobsForParallelWorkflow() {
        // Given
        Job childJob1 = createTestJob("child-1", "parent-1", AgentState.RUNNING());
        Job childJob2 = createTestJob("child-2", "parent-1", AgentState.RUNNING());
        List<Job> childJobs = Arrays.asList(childJob1, childJob2);

        // When
        CompletionCheckResult result = new CompletionCheckResult(false, null, childJobs);

        // Then
        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getFinalStatus()).isNull();
        assertThat(result.getChildJobs()).hasSize(2);
        assertThat(result.getChildJobs()).allMatch(job -> job.status().isActive());
    }

    @Test
    void shouldCreateCompletedResultWithSuccessfulChildJobs() {
        // Given
        AgentState finalStatus = AgentState.FINISHED();
        Job childJob1 = createTestJob("child-1", "parent-1", AgentState.FINISHED());
        Job childJob2 = createTestJob("child-2", "parent-1", AgentState.FINISHED());
        List<Job> childJobs = Arrays.asList(childJob1, childJob2);

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, childJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getFinalStatus().isSuccessful()).isTrue();
        assertThat(result.getChildJobs()).hasSize(2);
        assertThat(result.getChildJobs()).allMatch(job -> job.status().isSuccessful());
    }

    @Test
    void shouldCreateCompletedResultWithFailedChildJobs() {
        // Given
        AgentState finalStatus = AgentState.ERROR();
        Job childJob1 = createTestJob("child-1", "parent-1", AgentState.ERROR());
        Job childJob2 = createTestJob("child-2", "parent-1", AgentState.EXPIRED());
        List<Job> childJobs = Arrays.asList(childJob1, childJob2);

        // When
        CompletionCheckResult result = new CompletionCheckResult(true, finalStatus, childJobs);

        // Then
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(finalStatus);
        assertThat(result.getFinalStatus().isFailed()).isTrue();
        assertThat(result.getChildJobs()).hasSize(2);
        assertThat(result.getChildJobs()).allMatch(job -> job.status().isFailed());
    }

    /**
     * Creates a test job with default values.
     */
    private Job createTestJob(String jobId, String parentJobId) {
        return createTestJob(jobId, parentJobId, AgentState.CREATING());
    }

    /**
     * Creates a test job with specified status.
     */
    private Job createTestJob(String jobId, String parentJobId, AgentState status) {
        return new Job(
            jobId,
            "/path/workflow.xml",
            null,
            "model",
            "repo",
            status,
            LocalDateTime.now(),
            LocalDateTime.now(),
            parentJobId,
            null,
            WorkflowType.PARALLEL,
            null,
            null,
            null,
            null
        );
    }
}

