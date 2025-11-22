package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobDeletionServiceTest {

    private static final String JOB_ID = "job-id";

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CLIAgent cliAgent;

    @InjectMocks
    private JobDeletionService jobDeletionService;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
    }

    @Test
    void shouldDeleteWhenDeleteOnCompletionFlagIsTrue() {
        JobDeletionService serviceSpy = spy(jobDeletionService);
        Job job = createJob(JOB_ID, null, AgentState.FINISHED(), null);

        doNothing().when(serviceSpy).deleteJobAndChildren(JOB_ID, "--delete-on-completion");

        serviceSpy.handleDeletion(JOB_ID, job, List.of(), true, false);

        verify(serviceSpy).deleteJobAndChildren(JOB_ID, "--delete-on-completion");
    }

    @Test
    void shouldDeleteWhenSuccessFlagAndAllJobsSuccessful() {
        JobDeletionService serviceSpy = spy(jobDeletionService);
        Job parentJob = createJob(JOB_ID, null, AgentState.FINISHED(), null);
        List<Job> childJobs = List.of(createJob("child-1", JOB_ID, AgentState.FINISHED(), null));

        doNothing().when(serviceSpy).deleteJobAndChildren(JOB_ID, "--delete-on-success-completion");
        when(serviceSpy.isJobAndChildrenSuccessful(JOB_ID, childJobs)).thenReturn(true);

        serviceSpy.handleDeletion(JOB_ID, parentJob, childJobs, false, true);

        verify(serviceSpy).deleteJobAndChildren(JOB_ID, "--delete-on-success-completion");
    }

    @Test
    void shouldNotDeleteWhenSuccessFlagButJobsNotSuccessful() {
        JobDeletionService serviceSpy = spy(jobDeletionService);
        Job parentJob = createJob(JOB_ID, null, AgentState.FINISHED(), null);
        List<Job> childJobs = List.of(createJob("child-1", JOB_ID, AgentState.ERROR(), null));

        when(serviceSpy.isJobAndChildrenSuccessful(JOB_ID, childJobs)).thenReturn(false);

        serviceSpy.handleDeletion(JOB_ID, parentJob, childJobs, false, true);

        verify(serviceSpy, never()).deleteJobAndChildren(any(), any());
    }

    @Test
    void shouldReturnTrueWhenParentAndChildrenSuccessful() {
        Job parentJob = createJob(JOB_ID, null, AgentState.FINISHED(), null);
        Job childJob = createJob("child", JOB_ID, AgentState.FINISHED(), null);

        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(parentJob));

        boolean result = jobDeletionService.isJobAndChildrenSuccessful(JOB_ID, List.of(childJob));

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenParentNotSuccessful() {
        Job parentJob = createJob(JOB_ID, null, AgentState.ERROR(), null);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(parentJob));

        boolean result = jobDeletionService.isJobAndChildrenSuccessful(JOB_ID, List.of());

        assertThat(result).isFalse();
    }

    @Test
    void shouldDeleteJobAndChildrenInOrder() {
        JobDeletionService serviceSpy = spy(jobDeletionService);
        Job parentJob = createJob(JOB_ID, null, AgentState.FINISHED(), null);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(parentJob));

        doNothing().when(serviceSpy).deleteChildJobsRecursively(JOB_ID);
        doNothing().when(serviceSpy).deleteJob(parentJob);

        serviceSpy.deleteJobAndChildren(JOB_ID, "--delete-on-success-completion");

        verify(serviceSpy).deleteChildJobsRecursively(JOB_ID);
        verify(serviceSpy).deleteJob(parentJob);
    }

    @Test
    void shouldDeleteChildJobsRecursivelyDepthFirst() {
        JobDeletionService serviceSpy = spy(jobDeletionService);
        Job child1 = createJob("child-1", JOB_ID, AgentState.FINISHED(), null);
        Job child2 = createJob("child-2", JOB_ID, AgentState.FINISHED(), null);
        Job grandChild = createJob("grand-child", "child-1", AgentState.FINISHED(), null);

        when(jobRepository.findJobsByParentId(JOB_ID)).thenReturn(List.of(child1, child2));
        when(jobRepository.findJobsByParentId("child-1")).thenReturn(List.of(grandChild));
        when(jobRepository.findJobsByParentId("child-2")).thenReturn(List.of());
        when(jobRepository.findJobsByParentId("grand-child")).thenReturn(List.of());

        doNothing().when(serviceSpy).deleteJob(any());

        serviceSpy.deleteChildJobsRecursively(JOB_ID);

        verify(jobRepository, times(4)).findJobsByParentId(any());
        verify(serviceSpy).deleteJob(grandChild);
        verify(serviceSpy).deleteJob(child1);
        verify(serviceSpy).deleteJob(child2);
    }

    @Test
    void shouldDeleteCursorAgentAndDatabaseEntries() {
        Job job = createJob(JOB_ID, null, AgentState.FINISHED(), "cursor-agent-123");

        jobDeletionService.deleteJob(job);

        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(jobRepository).deletePromptsByJobId(JOB_ID);
        verify(jobRepository).deleteById(JOB_ID);
    }

    @Test
    void shouldContinueDeletionWhenCursorAgentRemovalFails() {
        Job job = createJob(JOB_ID, null, AgentState.FINISHED(), "cursor-agent-123");
        doThrow(new RuntimeException("API error")).when(cliAgent).deleteAgent("cursor-agent-123");

        jobDeletionService.deleteJob(job);

        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(jobRepository).deletePromptsByJobId(JOB_ID);
        verify(jobRepository).deleteById(JOB_ID);
    }

    @Test
    void shouldContinueWhenExceptionOccursDuringDeleteJobAndChildren() {
        JobDeletionService serviceSpy = spy(jobDeletionService);
        doThrow(new RuntimeException("boom")).when(serviceSpy).deleteChildJobsRecursively(JOB_ID);

        serviceSpy.deleteJobAndChildren(JOB_ID, "--delete-on-completion");

        verify(serviceSpy).deleteChildJobsRecursively(JOB_ID);
    }

    private Job createJob(String jobId, String parentJobId, AgentState state, String cursorAgentId) {
        return new Job(
            jobId,
            "/tmp/workflow.xml",
            cursorAgentId,
            "model",
            "repo",
            state,
            now.minusMinutes(5),
            now,
            parentJobId,
            null,
            WorkflowType.SEQUENCE,
            null,
            null,
            null,
            null
        );
    }
}
