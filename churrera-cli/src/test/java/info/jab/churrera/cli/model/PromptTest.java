package info.jab.churrera.cli.model;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Prompt model")
class PromptTest {

    private static final String PROMPT_ID = "prompt-123";
    private static final String JOB_ID = "job-456";
    private static final String PML_FILE = "/path/to/prompt.pml";
    private static final String STATUS = "PENDING";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 10, 11, 8, 0);
    private static final LocalDateTime LAST_UPDATE = CREATED_AT.plusMinutes(5);

    @Test
    @DisplayName("should create a valid prompt")
    void shouldCreateValidPrompt() {
        Prompt prompt = promptFixture(CREATED_AT, LAST_UPDATE);

        assertThat(prompt.promptId()).isEqualTo(PROMPT_ID);
        assertThat(prompt.jobId()).isEqualTo(JOB_ID);
        assertThat(prompt.pmlFile()).isEqualTo(PML_FILE);
        assertThat(prompt.status()).isEqualTo(STATUS);
        assertThat(prompt.createdAt()).isEqualTo(CREATED_AT);
        assertThat(prompt.lastUpdate()).isEqualTo(LAST_UPDATE);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mandatoryPromptFieldProvider")
    @DisplayName("should enforce mandatory arguments")
    void shouldValidateMandatoryFields(String description, ThrowingCallable factory, String expectedMessage) {
        assertThatThrownBy(factory)
            .isInstanceOf(NullPointerException.class)
            .hasMessage(expectedMessage);
    }

    private static Stream<Arguments> mandatoryPromptFieldProvider() {
        return Stream.of(
            arguments("should reject null prompt id", (ThrowingCallable) () -> new Prompt(null, JOB_ID, PML_FILE, STATUS, CREATED_AT, LAST_UPDATE), "Prompt ID cannot be null"),
            arguments("should reject null job id", (ThrowingCallable) () -> new Prompt(PROMPT_ID, null, PML_FILE, STATUS, CREATED_AT, LAST_UPDATE), "Job ID cannot be null"),
            arguments("should reject null pml file", (ThrowingCallable) () -> new Prompt(PROMPT_ID, JOB_ID, null, STATUS, CREATED_AT, LAST_UPDATE), "PML file cannot be null"),
            arguments("should reject null status", (ThrowingCallable) () -> new Prompt(PROMPT_ID, JOB_ID, PML_FILE, null, CREATED_AT, LAST_UPDATE), "Status cannot be null"),
            arguments("should reject null createdAt", (ThrowingCallable) () -> new Prompt(PROMPT_ID, JOB_ID, PML_FILE, STATUS, null, LAST_UPDATE), "Created at cannot be null"),
            arguments("should reject null lastUpdate", (ThrowingCallable) () -> new Prompt(PROMPT_ID, JOB_ID, PML_FILE, STATUS, CREATED_AT, null), "Last update cannot be null")
        );
    }

    @Test
    @DisplayName("should update status and refresh timestamp")
    void shouldUpdateStatus() {
        Prompt original = promptFixture(CREATED_AT, LAST_UPDATE);
        String newStatus = "COMPLETED";

        Prompt updated = original.withStatus(newStatus);

        assertThat(updated.status()).isEqualTo(newStatus);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
        assertThat(updated.promptId()).isEqualTo(original.promptId());
    }

    @Test
    @DisplayName("should update PML file and refresh timestamp")
    void shouldUpdatePmlFile() {
        Prompt original = promptFixture(CREATED_AT, LAST_UPDATE);
        String newPmlFile = "/updated/prompt.pml";

        Prompt updated = original.withPmlFile(newPmlFile);

        assertThat(updated.pmlFile()).isEqualTo(newPmlFile);
        assertThat(updated.lastUpdate()).isAfter(original.lastUpdate());
        assertThat(updated.jobId()).isEqualTo(original.jobId());
    }

    @ParameterizedTest(name = "should preserve status {0}")
    @ValueSource(strings = {"PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"})
    @DisplayName("should handle diverse statuses")
    void shouldHandleDifferentStatuses(String status) {
        Prompt prompt = new Prompt(PROMPT_ID, JOB_ID, PML_FILE, status, CREATED_AT, LAST_UPDATE);

        assertThat(prompt.status()).isEqualTo(status);
    }

    @ParameterizedTest(name = "should support PML path {0}")
    @ValueSource(strings = {"/path/to/prompt.pml", "relative/prompt.pml", "./prompt.pml", "../parent/prompt.pml"})
    @DisplayName("should handle diverse PML file formats")
    void shouldHandleDifferentPmlFileFormats(String pmlPath) {
        Prompt prompt = new Prompt(PROMPT_ID, JOB_ID, pmlPath, STATUS, CREATED_AT, LAST_UPDATE);

        assertThat(prompt.pmlFile()).isEqualTo(pmlPath);
    }

    @Test
    @DisplayName("should implement equality and hashCode")
    void shouldHaveProperEquality() {
        Prompt prompt1 = promptFixture(CREATED_AT, LAST_UPDATE);
        Prompt prompt2 = promptFixture(CREATED_AT, LAST_UPDATE);
        Prompt prompt3 = new Prompt("other", JOB_ID, PML_FILE, STATUS, CREATED_AT, LAST_UPDATE);

        assertThat(prompt1)
            .isEqualTo(prompt2)
            .hasSameHashCodeAs(prompt2)
            .isNotEqualTo(prompt3);
    }

    @Test
    @DisplayName("should expose useful toString representation")
    void shouldHaveProperToString() {
        Prompt prompt = promptFixture(CREATED_AT, LAST_UPDATE);

        assertThat(prompt.toString())
            .contains(PROMPT_ID)
            .contains(JOB_ID)
            .contains(PML_FILE)
            .contains(STATUS);
    }

    private static Prompt promptFixture(LocalDateTime createdAt, LocalDateTime lastUpdate) {
        return new Prompt(PROMPT_ID, JOB_ID, PML_FILE, STATUS, createdAt, lastUpdate);
    }
}
