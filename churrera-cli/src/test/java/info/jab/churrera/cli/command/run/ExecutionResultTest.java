package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionResultTest {

    @Test
    void shouldExposeFieldsWhenCreatedWithoutChildren() {
        // When
        ExecutionResult result = new ExecutionResult(AgentState.FINISHED(), true);

        // Then
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.FINISHED());
        assertThat(result.isInterrupted()).isTrue();
        assertThat(result.getChildJobs()).isEmpty();
    }

    @Test
    void shouldExposeFieldsWhenCreatedWithChildren() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job child = new Job(
            "child",
            "/tmp/workflow.xml",
            null,
            "model",
            "repo",
            AgentState.FINISHED(),
            now,
            now,
            "parent",
            null,
            WorkflowType.SEQUENCE,
            null,
            null,
            null,
            null
        );

        // When
        ExecutionResult result = new ExecutionResult(AgentState.ERROR(), false, List.of(child));

        // Then
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.ERROR());
        assertThat(result.isInterrupted()).isFalse();
        assertThat(result.getChildJobs()).containsExactly(child);
    }
}

