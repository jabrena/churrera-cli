package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleWorkflowCompletionCheckerTest {

    private final SimpleWorkflowCompletionChecker checker = new SimpleWorkflowCompletionChecker();

    @Test
    void shouldReportCompletionWhenJobIsTerminal() {
        Job terminalJob = createJob(AgentState.FINISHED());

        CompletionCheckResult result = checker.checkCompletion(terminalJob, terminalJob.jobId());

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.FINISHED());
        assertThat(result.getChildJobs()).isEmpty();
    }

    @Test
    void shouldReportNotCompletedWhenJobIsActive() {
        Job activeJob = createJob(AgentState.RUNNING());

        CompletionCheckResult result = checker.checkCompletion(activeJob, activeJob.jobId());

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getFinalStatus()).isNull();
        assertThat(result.getChildJobs()).isEmpty();
    }

    private Job createJob(AgentState state) {
        LocalDateTime now = LocalDateTime.now();
        return new Job(
            "job-" + state,
            "/tmp/workflow.xml",
            null,
            "model",
            "repo",
            state,
            now.minusMinutes(1),
            now,
            null,
            null,
            WorkflowType.SEQUENCE,
            null,
            null,
            null,
            null
        );
    }
}

