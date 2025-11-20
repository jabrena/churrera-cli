package info.jab.churrera.cli.model;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.AgentStatus;
import info.jab.cursor.client.model.Source;
import info.jab.cursor.client.model.Target;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentState")
class AgentStateTest {

    @ParameterizedTest(name = "AgentStatus {0} should map to AgentState")
    @EnumSource(AgentStatus.class)
    @DisplayName("should create AgentState from AgentStatus enum")
    void shouldCreateStateFromAgentStatus(AgentStatus status) {
        // When
        AgentState state = AgentState.of(status);

        // Then
        assertThat(state.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("should default to CREATING when AgentStatus is null")
    void shouldDefaultToCreatingForNullStatus() {
        assertThat(AgentState.of((AgentStatus) null)).isEqualTo(AgentState.CREATING());
    }

    @ParameterizedTest(name = "AgentResponse with status {0} should map correctly")
    @EnumSource(AgentStatus.class)
    @DisplayName("should create AgentState from AgentResponse")
    void shouldCreateStateFromAgentResponse(AgentStatus status) {
        // Given
        AgentResponse response = agentResponse(status);

        // When
        AgentState state = AgentState.of(response);

        // Then
        assertThat(state.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("should default to CREATING when AgentResponse is null")
    void shouldDefaultToCreatingWhenAgentResponseIsNull() {
        assertThat(AgentState.of((AgentResponse) null)).isEqualTo(AgentState.CREATING());
    }

    @Test
    @DisplayName("should default to CREATING when AgentResponse status is null")
    void shouldDefaultToCreatingWhenAgentResponseStatusIsNull() {
        AgentResponse response = agentResponse(null);

        assertThat(AgentState.of(response)).isEqualTo(AgentState.CREATING());
    }

    @ParameterizedTest(name = "\"{0}\" should be parsed as {1}")
    @MethodSource("statusStringProvider")
    @DisplayName("should parse textual statuses case-insensitively")
    void shouldParseStatusStrings(String input, AgentStatus expectedStatus) {
        // When
        AgentState state = AgentState.of(input);

        // Then
        assertThat(state.getStatus()).isEqualTo(expectedStatus);
    }

    private static Stream<Arguments> statusStringProvider() {
        return Stream.of(
            Arguments.of("CREATING", AgentStatus.CREATING),
            Arguments.of("creating", AgentStatus.CREATING),
            Arguments.of(" Creating ", AgentStatus.CREATING),
            Arguments.of("RUNNING", AgentStatus.RUNNING),
            Arguments.of("running", AgentStatus.RUNNING),
            Arguments.of("  running  ", AgentStatus.RUNNING),
            Arguments.of("FINISHED", AgentStatus.FINISHED),
            Arguments.of("finished", AgentStatus.FINISHED),
            Arguments.of("FINISHED\t", AgentStatus.FINISHED),
            Arguments.of("ERROR", AgentStatus.ERROR),
            Arguments.of("error", AgentStatus.ERROR),
            Arguments.of(" error ", AgentStatus.ERROR),
            Arguments.of("EXPIRED", AgentStatus.EXPIRED),
            Arguments.of("expired", AgentStatus.EXPIRED),
            Arguments.of("\tExpired\n", AgentStatus.EXPIRED)
        );
    }

    @ParameterizedTest(name = "\"{0}\" should default to CREATING")
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "unknown", "RUNNING!", "CREATING_EXTRA"})
    @DisplayName("should default to CREATING for invalid textual statuses")
    void shouldDefaultToCreatingForInvalidStatusStrings(String invalidValue) {
        assertThat(AgentState.of(invalidValue)).isEqualTo(AgentState.CREATING());
    }

    @ParameterizedTest(name = "{0} -> terminal={1}, successful={2}, failed={3}")
    @MethodSource("stateExpectationProvider")
    @DisplayName("should expose derived flags consistently")
    void shouldExposeDerivedFlags(AgentStatus status, boolean terminal, boolean successful, boolean failed) {
        // When
        AgentState state = AgentState.of(status);

        // Then
        assertThat(state.isTerminal()).isEqualTo(terminal);
        assertThat(state.isActive()).isEqualTo(!terminal);
        assertThat(state.isSuccessful()).isEqualTo(successful);
        assertThat(state.isFailed()).isEqualTo(failed);
    }

    private static Stream<Arguments> stateExpectationProvider() {
        return Stream.of(
            Arguments.of(AgentStatus.CREATING, false, false, false),
            Arguments.of(AgentStatus.RUNNING, false, false, false),
            Arguments.of(AgentStatus.FINISHED, true, true, false),
            Arguments.of(AgentStatus.ERROR, true, false, true),
            Arguments.of(AgentStatus.EXPIRED, true, false, true)
        );
    }

    @Test
    @DisplayName("should implement equality and hashCode based on status")
    void shouldImplementEqualityContract() {
        // Given
        AgentState creating = AgentState.CREATING();
        AgentState sameCreating = AgentState.of(AgentStatus.CREATING);
        AgentState running = AgentState.RUNNING();

        // Then
        assertThat(creating)
            .isEqualTo(sameCreating)
            .hasSameHashCodeAs(sameCreating)
            .isNotEqualTo(running);
    }

    @ParameterizedTest(name = "{0} should render its enum name")
    @EnumSource(AgentStatus.class)
    @DisplayName("should render enum name inside toString")
    void shouldRenderEnumName(AgentStatus status) {
        assertThat(AgentState.of(status)).hasToString(status.name());
    }

    private static AgentResponse agentResponse(AgentStatus status) {
        return new AgentResponse(
            "test-id",
            "Test Agent",
            status,
            new Source(URI.create("https://github.com/test/repo"), "main"),
            new Target("cursor/test", URI.create("https://cursor.com/agents?id=test"), false, false, false),
            OffsetDateTime.now()
        );
    }
}
