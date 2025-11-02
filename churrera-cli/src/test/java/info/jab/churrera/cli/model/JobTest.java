package info.jab.churrera.cli.model;

import info.jab.churrera.agent.AgentState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Job record.
 */
class JobTest {

    @Test
    void shouldCreateValidJob() {
        // Given
        String jobId = "test-job-id";
        String path = "/path/to/workflow.xml";
        String cursorAgentId = "agent-123";
        String model = "gpt-4";
        String repository = "test-repo";
        AgentState status = AgentState.PENDING;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When
        Job job = new Job(jobId, path, cursorAgentId, model, repository, status, createdAt, lastUpdate, null, null, null);

        // Then
        assertThat(job.jobId()).isEqualTo(jobId);
        assertThat(job.path()).isEqualTo(path);
        assertThat(job.cursorAgentId()).isEqualTo(cursorAgentId);
        assertThat(job.model()).isEqualTo(model);
        assertThat(job.repository()).isEqualTo(repository);
        assertThat(job.status()).isEqualTo(status);
        assertThat(job.createdAt()).isEqualTo(createdAt);
        assertThat(job.lastUpdate()).isEqualTo(lastUpdate);
        assertThat(job.parentJobId()).isNull();
        assertThat(job.result()).isNull();
    }

    @Test
    void shouldCreateJobWithNullCursorAgentId() {
        // Given
        String jobId = "test-job-id";
        String path = "/path/to/workflow.xml";
        String model = "gpt-4";
        String repository = "test-repo";
        AgentState status = AgentState.PENDING;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When
        Job job = new Job(jobId, path, null, model, repository, status, createdAt, lastUpdate, null, null, null);

        // Then
        assertThat(job.cursorAgentId()).isNull();
        assertThat(job.jobId()).isEqualTo(jobId);
    }

    @Test
    void shouldThrowExceptionWhenJobIdIsNull() {
        // Given
        String path = "/path/to/workflow.xml";
        String model = "gpt-4";
        String repository = "test-repo";
        AgentState status = AgentState.PENDING;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Job(null, path, null, model, repository, status, createdAt, lastUpdate, null, null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Job ID cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenPathIsNull() {
        // Given
        String jobId = "test-job-id";
        String model = "gpt-4";
        String repository = "test-repo";
        AgentState status = AgentState.PENDING;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Job(jobId, null, null, model, repository, status, createdAt, lastUpdate, null, null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Path cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenModelIsNull() {
        // Given
        String jobId = "test-job-id";
        String path = "/path/to/workflow.xml";
        String repository = "test-repo";
        AgentState status = AgentState.PENDING;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Job(jobId, path, null, null, repository, status, createdAt, lastUpdate, null, null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Model cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenRepositoryIsNull() {
        // Given
        String jobId = "test-job-id";
        String path = "/path/to/workflow.xml";
        String model = "gpt-4";
        AgentState status = AgentState.PENDING;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Job(jobId, path, null, model, null, status, createdAt, lastUpdate, null, null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Repository cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenStatusIsNull() {
        // Given
        String jobId = "test-job-id";
        String path = "/path/to/workflow.xml";
        String model = "gpt-4";
        String repository = "test-repo";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Job(jobId, path, null, model, repository, null, createdAt, lastUpdate, null, null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Status cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenCreatedAtIsNull() {
        // Given
        String jobId = "test-job-id";
        String path = "/path/to/workflow.xml";
        String model = "gpt-4";
        String repository = "test-repo";
        AgentState status = AgentState.PENDING;
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Job(jobId, path, null, model, repository, status, null, lastUpdate, null, null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Created at cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenLastUpdateIsNull() {
        // Given
        String jobId = "test-job-id";
        String path = "/path/to/workflow.xml";
        String model = "gpt-4";
        String repository = "test-repo";
        AgentState status = AgentState.PENDING;
        LocalDateTime createdAt = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Job(jobId, path, null, model, repository, status, createdAt, null, null, null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Last update cannot be null");
    }

    @Test
    void shouldCreateJobWithUpdatedPath() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        Job originalJob = new Job(
            "test-job-id",
            "/original/path.xml",
            "agent-123",
            "gpt-4",
            "test-repo",
            AgentState.PENDING,
            createdAt,
            LocalDateTime.now(),
            null,
            null,
            null
        );
        String newPath = "/new/path.xml";

        // When
        Job updatedJob = originalJob.withPath(newPath);

        // Then
        assertThat(updatedJob.path()).isEqualTo(newPath);
        assertThat(updatedJob.jobId()).isEqualTo(originalJob.jobId());
        assertThat(updatedJob.cursorAgentId()).isEqualTo(originalJob.cursorAgentId());
        assertThat(updatedJob.model()).isEqualTo(originalJob.model());
        assertThat(updatedJob.repository()).isEqualTo(originalJob.repository());
        assertThat(updatedJob.status()).isEqualTo(originalJob.status());
        assertThat(updatedJob.createdAt()).isEqualTo(originalJob.createdAt());
        assertThat(updatedJob.lastUpdate()).isAfter(originalJob.lastUpdate());
    }

    @Test
    void shouldCreateJobWithUpdatedCursorAgentId() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        Job originalJob = new Job(
            "test-job-id",
            "/path/to/workflow.xml",
            null,
            "gpt-4",
            "test-repo",
            AgentState.PENDING,
            createdAt,
            LocalDateTime.now(),
            null,
            null,
            null
        );
        String newAgentId = "agent-456";

        // When
        Job updatedJob = originalJob.withCursorAgentId(newAgentId);

        // Then
        assertThat(updatedJob.cursorAgentId()).isEqualTo(newAgentId);
        assertThat(updatedJob.jobId()).isEqualTo(originalJob.jobId());
        assertThat(updatedJob.path()).isEqualTo(originalJob.path());
        assertThat(updatedJob.model()).isEqualTo(originalJob.model());
        assertThat(updatedJob.repository()).isEqualTo(originalJob.repository());
        assertThat(updatedJob.status()).isEqualTo(originalJob.status());
        assertThat(updatedJob.createdAt()).isEqualTo(originalJob.createdAt());
    }

    @Test
    void shouldCreateJobWithUpdatedStatus() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        Job originalJob = new Job(
            "test-job-id",
            "/path/to/workflow.xml",
            "agent-123",
            "gpt-4",
            "test-repo",
            AgentState.PENDING,
            createdAt,
            LocalDateTime.now(),
            null,
            null,
            null
        );
        AgentState newStatus = AgentState.RUNNING;

        // When
        Job updatedJob = originalJob.withStatus(newStatus);

        // Then
        assertThat(updatedJob.status()).isEqualTo(newStatus);
        assertThat(updatedJob.jobId()).isEqualTo(originalJob.jobId());
        assertThat(updatedJob.path()).isEqualTo(originalJob.path());
        assertThat(updatedJob.cursorAgentId()).isEqualTo(originalJob.cursorAgentId());
        assertThat(updatedJob.model()).isEqualTo(originalJob.model());
        assertThat(updatedJob.repository()).isEqualTo(originalJob.repository());
        assertThat(updatedJob.createdAt()).isEqualTo(originalJob.createdAt());
    }

    @Test
    void shouldCreateJobWithUpdatedModel() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        Job originalJob = new Job(
            "test-job-id",
            "/path/to/workflow.xml",
            "agent-123",
            "gpt-4",
            "test-repo",
            AgentState.PENDING,
            createdAt,
            LocalDateTime.now(),
            null,
            null,
            null
        );
        String newModel = "gpt-4-turbo";

        // When
        Job updatedJob = originalJob.withModel(newModel);

        // Then
        assertThat(updatedJob.model()).isEqualTo(newModel);
        assertThat(updatedJob.jobId()).isEqualTo(originalJob.jobId());
        assertThat(updatedJob.path()).isEqualTo(originalJob.path());
        assertThat(updatedJob.cursorAgentId()).isEqualTo(originalJob.cursorAgentId());
        assertThat(updatedJob.repository()).isEqualTo(originalJob.repository());
        assertThat(updatedJob.status()).isEqualTo(originalJob.status());
        assertThat(updatedJob.createdAt()).isEqualTo(originalJob.createdAt());
    }

    @Test
    void shouldCreateJobWithUpdatedRepository() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        Job originalJob = new Job(
            "test-job-id",
            "/path/to/workflow.xml",
            "agent-123",
            "gpt-4",
            "test-repo",
            AgentState.PENDING,
            createdAt,
            LocalDateTime.now(),
            null,
            null,
            null
        );
        String newRepository = "new-repo";

        // When
        Job updatedJob = originalJob.withRepository(newRepository);

        // Then
        assertThat(updatedJob.repository()).isEqualTo(newRepository);
        assertThat(updatedJob.jobId()).isEqualTo(originalJob.jobId());
        assertThat(updatedJob.path()).isEqualTo(originalJob.path());
        assertThat(updatedJob.cursorAgentId()).isEqualTo(originalJob.cursorAgentId());
        assertThat(updatedJob.model()).isEqualTo(originalJob.model());
        assertThat(updatedJob.status()).isEqualTo(originalJob.status());
        assertThat(updatedJob.createdAt()).isEqualTo(originalJob.createdAt());
    }

    @Test
    void shouldHaveProperEquality() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        Job job1 = new Job("id", "/path", "agent", "model", "repo", AgentState.PENDING, timestamp, timestamp, null, null, null);
        Job job2 = new Job("id", "/path", "agent", "model", "repo", AgentState.PENDING, timestamp, timestamp, null, null, null);
        Job job3 = new Job("id2", "/path", "agent", "model", "repo", AgentState.PENDING, timestamp, timestamp, null, null, null);

        // When & Then
        assertThat(job1).isEqualTo(job2);
        assertThat(job1).isNotEqualTo(job3);
        assertThat(job1.hashCode()).isEqualTo(job2.hashCode());
    }

    @Test
    void shouldHaveProperToString() {
        // Given
        LocalDateTime timestamp = LocalDateTime.of(2024, 10, 11, 14, 30);
        Job job = new Job(
            "test-id",
            "/path/to/file.xml",
            "agent-123",
            "gpt-4",
            "test-repo",
            AgentState.PENDING,
            timestamp,
            timestamp,
            null,
            null,
            null
        );

        // When
        String toString = job.toString();

        // Then
        assertThat(toString).contains("test-id");
        assertThat(toString).contains("/path/to/file.xml");
        assertThat(toString).contains("agent-123");
        assertThat(toString).contains("gpt-4");
        assertThat(toString).contains("test-repo");
        assertThat(toString).contains("PENDING");
    }

    @Test
    void shouldTestAllAgentStates() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();

        // When & Then - Test with different states
        for (AgentState state : AgentState.values()) {
            Job job = new Job("id", "/path", "agent", "model", "repo", state, timestamp, timestamp, null, null, null);
            assertThat(job.status()).isEqualTo(state);
        }
    }
}

