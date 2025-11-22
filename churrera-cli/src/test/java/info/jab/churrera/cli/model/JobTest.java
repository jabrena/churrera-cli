package info.jab.churrera.cli.model;

import info.jab.churrera.workflow.WorkflowType;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Job model")
class JobTest {

    private static final String JOB_ID = "test-job-id";
    private static final String PATH = "/path/to/workflow.xml";
    private static final String CURSOR_AGENT_ID = "agent-123";
    private static final String MODEL = "gpt-4";
    private static final String REPOSITORY = "test-repo";
    private static final AgentState STATUS = AgentState.CREATING();
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 10, 11, 8, 0);
    private static final LocalDateTime LAST_UPDATE = CREATED_AT.plusMinutes(5);

    @Test
    @DisplayName("should create a valid job with minimal data")
    void shouldCreateValidJob() {
        // When
        Job job = jobFixture(CREATED_AT, LAST_UPDATE);

        // Then
        assertThat(job.jobId()).isEqualTo(JOB_ID);
        assertThat(job.path()).isEqualTo(PATH);
        assertThat(job.cursorAgentId()).isEqualTo(CURSOR_AGENT_ID);
        assertThat(job.model()).isEqualTo(MODEL);
        assertThat(job.repository()).isEqualTo(REPOSITORY);
        assertThat(job.status()).isEqualTo(STATUS);
        assertThat(job.createdAt()).isEqualTo(CREATED_AT);
        assertThat(job.lastUpdate()).isEqualTo(LAST_UPDATE);
        assertThat(job.parentJobId()).isNull();
        assertThat(job.result()).isNull();
        assertThat(job.type()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mandatoryFieldProvider")
    @DisplayName("should enforce mandatory constructor arguments")
    void shouldValidateMandatoryConstructorArguments(String description, ThrowingCallable factory, String expectedMessage) {
        assertThatThrownBy(factory)
            .isInstanceOf(NullPointerException.class)
            .hasMessage(expectedMessage);
    }

    private static Stream<Arguments> mandatoryFieldProvider() {
        return Stream.of(
            arguments("should reject null job id", (ThrowingCallable) () -> new Job(null, PATH, CURSOR_AGENT_ID, MODEL, REPOSITORY, STATUS, CREATED_AT, LAST_UPDATE, null, null, null, null, null, null, null), "Job ID cannot be null"),
            arguments("should reject null path", (ThrowingCallable) () -> new Job(JOB_ID, null, CURSOR_AGENT_ID, MODEL, REPOSITORY, STATUS, CREATED_AT, LAST_UPDATE, null, null, null, null, null, null, null), "Path cannot be null"),
            arguments("should reject null model", (ThrowingCallable) () -> new Job(JOB_ID, PATH, CURSOR_AGENT_ID, null, REPOSITORY, STATUS, CREATED_AT, LAST_UPDATE, null, null, null, null, null, null, null), "Model cannot be null"),
            arguments("should reject null repository", (ThrowingCallable) () -> new Job(JOB_ID, PATH, CURSOR_AGENT_ID, MODEL, null, STATUS, CREATED_AT, LAST_UPDATE, null, null, null, null, null, null, null), "Repository cannot be null"),
            arguments("should reject null status", (ThrowingCallable) () -> new Job(JOB_ID, PATH, CURSOR_AGENT_ID, MODEL, REPOSITORY, null, CREATED_AT, LAST_UPDATE, null, null, null, null, null, null, null), "Status cannot be null"),
            arguments("should reject null createdAt", (ThrowingCallable) () -> new Job(JOB_ID, PATH, CURSOR_AGENT_ID, MODEL, REPOSITORY, STATUS, null, LAST_UPDATE, null, null, null, null, null, null, null), "Created at cannot be null"),
            arguments("should reject null lastUpdate", (ThrowingCallable) () -> new Job(JOB_ID, PATH, CURSOR_AGENT_ID, MODEL, REPOSITORY, STATUS, CREATED_AT, null, null, null, null, null, null, null, null), "Last update cannot be null")
        );
    }

    @Test
    @DisplayName("should allow null cursor agent id")
    void shouldAllowNullCursorAgentId() {
        Job job = new Job(JOB_ID, PATH, null, MODEL, REPOSITORY, STATUS, CREATED_AT, LAST_UPDATE, null, null, null, null, null, null, null);

        assertThat(job.cursorAgentId()).isNull();
        assertThat(job.jobId()).isEqualTo(JOB_ID);
    }

    @Test
    @DisplayName("should update path and refresh timestamp")
    void shouldUpdatePath() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        String newPath = "/new/path.xml";

        Job updated = original.withPath(newPath);

        assertThat(updated.path()).isEqualTo(newPath);
        assertThat(updated.jobId()).isEqualTo(original.jobId());
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update cursor agent id and refresh timestamp")
    void shouldUpdateCursorAgentId() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        String newCursorId = "agent-456";

        Job updated = original.withCursorAgentId(newCursorId);

        assertThat(updated.cursorAgentId()).isEqualTo(newCursorId);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update status and refresh timestamp")
    void shouldUpdateStatus() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        AgentState running = AgentState.RUNNING();

        Job updated = original.withStatus(running);

        assertThat(updated.status()).isEqualTo(running);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update model and refresh timestamp")
    void shouldUpdateModel() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        String newModel = "gpt-4o";

        Job updated = original.withModel(newModel);

        assertThat(updated.model()).isEqualTo(newModel);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update repository and refresh timestamp")
    void shouldUpdateRepository() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        String newRepository = "another-repo";

        Job updated = original.withRepository(newRepository);

        assertThat(updated.repository()).isEqualTo(newRepository);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update parent job id and refresh timestamp")
    void shouldUpdateParentJobId() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        String parentJobId = "parent-123";

        Job updated = original.withParentJobId(parentJobId);

        assertThat(updated.parentJobId()).isEqualTo(parentJobId);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update result and refresh timestamp")
    void shouldUpdateResult() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        String result = "SUCCESS";

        Job updated = original.withResult(result);

        assertThat(updated.result()).isEqualTo(result);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update workflow type and refresh timestamp")
    void shouldUpdateType() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);

        Job updated = original.withType(WorkflowType.SEQUENCE);

        assertThat(updated.type()).isEqualTo(WorkflowType.SEQUENCE);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update timeout and refresh timestamp")
    void shouldUpdateTimeout() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        long timeoutMillis = 5_000L;

        Job updated = original.withTimeoutMillis(timeoutMillis);

        assertThat(updated.timeoutMillis()).isEqualTo(timeoutMillis);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update workflow start time and refresh timestamp")
    void shouldUpdateWorkflowStartTime() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        LocalDateTime startTime = CREATED_AT.plusMinutes(30);

        Job updated = original.withWorkflowStartTime(startTime);

        assertThat(updated.workflowStartTime()).isEqualTo(startTime);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update fallback source and refresh timestamp")
    void shouldUpdateFallbackSrc() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);
        String fallbackSrc = "fallback.pml";

        Job updated = original.withFallbackSrc(fallbackSrc);

        assertThat(updated.fallbackSrc()).isEqualTo(fallbackSrc);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should update fallback executed flag and refresh timestamp")
    void shouldUpdateFallbackExecuted() {
        Job original = jobFixture(CREATED_AT, LAST_UPDATE);

        Job updated = original.withFallbackExecuted(Boolean.TRUE);

        assertThat(updated.fallbackExecuted()).isTrue();
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
    }

    @Test
    @DisplayName("should implement equality and hashCode")
    void shouldHaveProperEquality() {
        Job job1 = jobFixture(CREATED_AT, LAST_UPDATE);
        Job job2 = jobFixture(CREATED_AT, LAST_UPDATE);
        Job job3 = new Job("another-id", PATH, CURSOR_AGENT_ID, MODEL, REPOSITORY, STATUS, CREATED_AT, LAST_UPDATE, null, null, null, null, null, null, null);

        assertThat(job1)
            .isEqualTo(job2)
            .hasSameHashCodeAs(job2)
            .isNotEqualTo(job3);
    }

    @Test
    @DisplayName("should expose useful toString representation")
    void shouldHaveProperToString() {
        Job job = jobFixture(CREATED_AT, LAST_UPDATE);

        assertThat(job.toString())
            .contains(JOB_ID)
            .contains(PATH)
            .contains(MODEL)
            .contains(REPOSITORY)
            .contains(STATUS.toString());
    }

    @ParameterizedTest(name = "should create job with {0} state")
    @MethodSource("agentStateProvider")
    @DisplayName("should accept all AgentState factory methods")
    void shouldSupportAllAgentStates(AgentState state) {
        Job job = new Job(JOB_ID, PATH, CURSOR_AGENT_ID, MODEL, REPOSITORY, state, CREATED_AT, LAST_UPDATE, null, null, null, null, null, null, null);

        assertThat(job.status()).isEqualTo(state);
    }

    private static Stream<AgentState> agentStateProvider() {
        return Stream.of(
            AgentState.CREATING(),
            AgentState.RUNNING(),
            AgentState.FINISHED(),
            AgentState.ERROR(),
            AgentState.EXPIRED()
        );
    }

    private static Job jobFixture(LocalDateTime createdAt, LocalDateTime lastUpdate) {
        return new Job(JOB_ID, PATH, CURSOR_AGENT_ID, MODEL, REPOSITORY, STATUS, createdAt, lastUpdate, null, null, null, null, null, null, null);
    }
}
