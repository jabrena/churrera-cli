package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPollingServiceTest {

    private static final String JOB_ID = "job-123";

    @Mock
    private JobProcessor jobProcessor;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobDisplayService jobDisplayService;
    @Mock
    private CompletionCheckerFactory completionCheckerFactory;
    @Mock
    private CompletionChecker completionChecker;

    private Job job;

    @BeforeEach
    void setUp() {
        job = createJob(AgentState.RUNNING());
        lenient().when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        lenient().when(completionCheckerFactory.create(any())).thenReturn(completionChecker);
    }

    @Test
    void shouldReturnResultWhenJobCompletesImmediately() {
        // Given
        CompletionCheckResult completion = new CompletionCheckResult(true, AgentState.FINISHED(), List.of(job));
        when(completionChecker.checkCompletion(job, JOB_ID)).thenReturn(completion);
        AtomicInteger sleeperCalls = new AtomicInteger();
        JobPollingService service = new JobPollingService(
            jobProcessor,
            jobRepository,
            jobDisplayService,
            completionCheckerFactory,
            1,
            millis -> sleeperCalls.incrementAndGet()
        );

        // When
        ExecutionResult result = service.executePollingLoop(JOB_ID);

        // Then
        assertThat(result.getFinalStatus()).isEqualTo(AgentState.FINISHED());
        assertThat(result.isInterrupted()).isFalse();
        assertThat(result.getChildJobs()).containsExactly(job);
        assertThat(sleeperCalls).hasValue(0);
        verify(jobProcessor).processJobs();
        verify(jobDisplayService).displayFilteredJobsTable(JOB_ID);
    }

    @Test
    void shouldReturnInterruptedResultWhenSleepIsInterrupted() {
        // Given
        CompletionCheckResult notCompleted = new CompletionCheckResult(false, null, List.of());
        when(completionChecker.checkCompletion(job, JOB_ID)).thenReturn(notCompleted);
        JobPollingService service = new JobPollingService(
            jobProcessor,
            jobRepository,
            jobDisplayService,
            completionCheckerFactory,
            1,
            millis -> { throw new InterruptedException("stop"); }
        );

        // When
        ExecutionResult result = service.executePollingLoop(JOB_ID);

        // Then
        assertThat(result.isInterrupted()).isTrue();
        assertThat(result.getFinalStatus()).isNull();
        assertThat(result.getChildJobs()).isEmpty();
        verify(jobProcessor).processJobs();
        verify(jobDisplayService).displayFilteredJobsTable(JOB_ID);
    }

    @Test
    void shouldPropagateRepositoryErrorsAsRuntimeException() {
        // Given
        when(jobRepository.findById(JOB_ID)).thenThrow(new RuntimeException("boom"));
        JobPollingService service = new JobPollingService(
            jobProcessor,
            jobRepository,
            jobDisplayService,
            completionCheckerFactory,
            1,
            millis -> {}
        );

        // When & Then
        assertThatThrownBy(() -> service.executePollingLoop(JOB_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Error retrieving job");

        verify(jobProcessor).processJobs();
        verify(jobDisplayService).displayFilteredJobsTable(JOB_ID);
    }

    @Test
    void shouldFallbackToSequenceCheckerWhenJobTypeIsNull() {
        Job jobWithoutType = createJob(AgentState.RUNNING(), null);
        Job completedJob = jobWithoutType.withStatus(AgentState.FINISHED());
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(jobWithoutType));
        when(completionCheckerFactory.create(WorkflowType.SEQUENCE)).thenReturn(completionChecker);
        when(completionChecker.checkCompletion(jobWithoutType, JOB_ID))
            .thenReturn(new CompletionCheckResult(true, AgentState.FINISHED(), List.of(completedJob)));

        JobPollingService service = new JobPollingService(
            jobProcessor,
            jobRepository,
            jobDisplayService,
            completionCheckerFactory,
            1,
            millis -> {}
        );

        ExecutionResult result = service.executePollingLoop(JOB_ID);

        assertThat(result.getFinalStatus()).isEqualTo(AgentState.FINISHED());
        assertThat(result.getChildJobs()).containsExactly(completedJob);
        verify(completionCheckerFactory).create(WorkflowType.SEQUENCE);
    }

    private Job createJob(AgentState state) {
        return createJob(state, WorkflowType.SEQUENCE);
    }

    private Job createJob(AgentState state, WorkflowType type) {
        LocalDateTime now = LocalDateTime.now();
        return new Job(
            JOB_ID,
            "/tmp/workflow.xml",
            null,
            "model",
            "repo",
            state,
            now.minusMinutes(1),
            now,
            null,
            null,
            type,
            null,
            null,
            null,
            null
        );
    }

}
