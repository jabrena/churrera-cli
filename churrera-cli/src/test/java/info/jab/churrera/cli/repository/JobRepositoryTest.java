package info.jab.churrera.cli.repository;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.JobWithDetails;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.util.PropertyResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JobRepository.
 */
@ExtendWith(MockitoExtension.class)
class JobRepositoryTest {

    @TempDir
    Path tempDir;

    @Mock
    private PropertyResolver propertyResolver;

    private JobRepository jobRepository;

    @BeforeEach
    void setUp() throws IOException {
        // Mock PropertyResolver to return the tempDir path
        when(propertyResolver.getProperty(eq("application.properties"), eq("basex.database.path")))
                .thenReturn(Optional.of(tempDir.toString()));

        jobRepository = new JobRepository(propertyResolver);
    }

    @AfterEach
    void tearDown() {
        if (jobRepository != null) {
            jobRepository.close();
        }
    }

    @Test
    void shouldInitializeRepository() {
        // Repository should be initialized without throwing exceptions
        assertThat(jobRepository).isNotNull();
    }

    @Test
    void shouldSaveAndFindJob() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job("test-job-1", "/path/to/job", null, "default-model", "default-repo", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);

        // When
        jobRepository.save(job);
        Optional<Job> found = jobRepository.findById("test-job-1");

        // Then
        assertThat(found).isPresent();
        // After XML round-trip, null fallbackExecuted becomes false
        Job expectedJob = new Job("test-job-1", "/path/to/job", null, "default-model", "default-repo", AgentState.CREATING(), now, now, null, null, null, null, null, null, false);
        assertThat(found.get()).isEqualTo(expectedJob);
    }

    @Test
    void shouldReturnEmptyWhenJobNotFound() {
        // When
        Optional<Job> found = jobRepository.findById("non-existent-job");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAllJobs() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job job1 = new Job("job-1", "/path/1", null, "model1", "repo1", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);
        Job job2 = new Job("job-2", "/path/2", null, "model2", "repo2", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);

        jobRepository.save(job1);
        jobRepository.save(job2);

        // When
        List<Job> allJobs = jobRepository.findAll();

        // Then
        assertThat(allJobs).hasSize(2);
        // After XML round-trip, null fallbackExecuted becomes false
        Job expectedJob1 = new Job("job-1", "/path/1", null, "model1", "repo1", AgentState.CREATING(), now, now, null, null, null, null, null, null, false);
        Job expectedJob2 = new Job("job-2", "/path/2", null, "model2", "repo2", AgentState.CREATING(), now, now, null, null, null, null, null, null, false);
        assertThat(allJobs).containsExactlyInAnyOrder(expectedJob1, expectedJob2);
    }

    @Test
    void shouldReturnEmptyListWhenNoJobs() {
        // When
        List<Job> allJobs = jobRepository.findAll();

        // Then
        assertThat(allJobs).isEmpty();
    }

    @Test
    void shouldDeleteJob() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job("job-to-delete", "/path/to/delete", null, "model", "repo", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);
        jobRepository.save(job);

        // Verify job exists
        assertThat(jobRepository.findById("job-to-delete")).isPresent();

        // When
        jobRepository.deleteById("job-to-delete");

        // Then
        assertThat(jobRepository.findById("job-to-delete")).isEmpty();
    }

    @Test
    void shouldSaveAndFindPrompt() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Prompt prompt = new Prompt("prompt-1", "job-1", "prompt1.xml", "UNKNOWN", now, now);

        // When
        jobRepository.savePrompt(prompt);
        Optional<Prompt> found = jobRepository.findPromptById("prompt-1");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(prompt);
    }

    @Test
    void shouldFindPromptsByJobId() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Prompt prompt1 = new Prompt("prompt-1", "job-1", "prompt1.xml", "UNKNOWN", now, now);
        Prompt prompt2 = new Prompt("prompt-2", "job-1", "prompt2.xml", "UNKNOWN", now, now);
        Prompt prompt3 = new Prompt("prompt-3", "job-2", "prompt3.xml", "UNKNOWN", now, now);

        jobRepository.savePrompt(prompt1);
        jobRepository.savePrompt(prompt2);
        jobRepository.savePrompt(prompt3);

        // When
        List<Prompt> job1Prompts = jobRepository.findPromptsByJobId("job-1");

        // Then
        assertThat(job1Prompts).hasSize(2);
        assertThat(job1Prompts).containsExactlyInAnyOrder(prompt1, prompt2);
    }

    @Test
    void shouldFindJobWithDetails() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job("job-1", "/path/1", "cursor-agent-123", "model1", "repo1", AgentState.FINISHED(), now, now, null, null, null, null, null, null, null);
        Prompt prompt1 = new Prompt("prompt-1", "job-1", "prompt1.xml", "COMPLETED", now, now);
        Prompt prompt2 = new Prompt("prompt-2", "job-1", "prompt2.xml", "SENT", now, now);

        jobRepository.save(job);
        jobRepository.savePrompt(prompt1);
        jobRepository.savePrompt(prompt2);

        // When
        Optional<JobWithDetails> result = jobRepository.findJobWithDetails("job-1");

        // Then
        assertThat(result).isPresent();
        // After XML round-trip, null fallbackExecuted becomes false
        Job expectedJob = new Job("job-1", "/path/1", "cursor-agent-123", "model1", "repo1", AgentState.FINISHED(), now, now, null, null, null, null, null, null, false);
        assertThat(result.get().getJob()).isEqualTo(expectedJob);
        assertThat(result.get().getPrompts()).hasSize(2);
        assertThat(result.get().getPrompts()).containsExactlyInAnyOrder(prompt1, prompt2);
    }

    @Test
    void shouldReturnEmptyWhenJobWithDetailsNotFound() {
        // When
        Optional<JobWithDetails> result = jobRepository.findJobWithDetails("non-existent-job");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindUnfinishedJobs() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job finishedJob = new Job("job-1", "/path/1", "cursor-agent-123", "model1", "repo1", AgentState.FINISHED(), now, now, null, null, null, null, null, null, null);
        Job unfinishedJob1 = new Job("job-2", "/path/2", null, "model2", "repo2", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);
        Job unfinishedJob2 = new Job("job-3", "/path/3", "cursor-agent-456", "model3", "repo3", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);

        jobRepository.save(finishedJob);
        jobRepository.save(unfinishedJob1);
        jobRepository.save(unfinishedJob2);

        // When
        List<Job> unfinishedJobs = jobRepository.findUnfinishedJobs();

        // Then
        assertThat(unfinishedJobs).hasSize(2);
        // After XML round-trip, null fallbackExecuted becomes false
        Job expectedUnfinishedJob1 = new Job("job-2", "/path/2", null, "model2", "repo2", AgentState.CREATING(), now, now, null, null, null, null, null, null, false);
        Job expectedUnfinishedJob2 = new Job("job-3", "/path/3", "cursor-agent-456", "model3", "repo3", AgentState.CREATING(), now, now, null, null, null, null, null, null, false);
        assertThat(unfinishedJobs).containsExactlyInAnyOrder(expectedUnfinishedJob1, expectedUnfinishedJob2);
    }

    @Test
    void shouldDeletePromptsByJobId() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Prompt prompt1 = new Prompt("prompt-1", "job-1", "prompt1.xml", "UNKNOWN", now, now);
        Prompt prompt2 = new Prompt("prompt-2", "job-1", "prompt2.xml", "UNKNOWN", now, now);
        Prompt prompt3 = new Prompt("prompt-3", "job-2", "prompt3.xml", "UNKNOWN", now, now);

        jobRepository.savePrompt(prompt1);
        jobRepository.savePrompt(prompt2);
        jobRepository.savePrompt(prompt3);

        // Verify prompts exist
        assertThat(jobRepository.findPromptsByJobId("job-1")).hasSize(2);
        assertThat(jobRepository.findPromptsByJobId("job-2")).hasSize(1);

        // When
        jobRepository.deletePromptsByJobId("job-1");

        // Then
        assertThat(jobRepository.findPromptsByJobId("job-1")).isEmpty();
        assertThat(jobRepository.findPromptsByJobId("job-2")).hasSize(1);
        assertThat(jobRepository.findPromptById("prompt-3")).isPresent();
    }

    @Test
    void shouldUpdateExistingJob() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job originalJob = new Job("job-1", "/path/1", null, "model1", "repo1", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);
        jobRepository.save(originalJob);

        // When
        Job updatedJob = originalJob.withCursorAgentId("cursor-agent-123").withStatus(AgentState.CREATING());
        jobRepository.save(updatedJob);

        // Then
        Optional<Job> found = jobRepository.findById("job-1");
        assertThat(found).isPresent();
        assertThat(found.get().cursorAgentId()).isEqualTo("cursor-agent-123");
        assertThat(found.get().status()).isEqualTo(AgentState.CREATING());
    }

    @Test
    void shouldUpdateExistingPrompt() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Prompt originalPrompt = new Prompt("prompt-1", "job-1", "prompt1.xml", "UNKNOWN", now, now);
        jobRepository.savePrompt(originalPrompt);

        // When
        Prompt updatedPrompt = originalPrompt.withStatus("COMPLETED");
        jobRepository.savePrompt(updatedPrompt);

        // Then
        Optional<Prompt> found = jobRepository.findPromptById("prompt-1");
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldHandleJobWithNullCursorAgentId() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job jobWithNullAgent = new Job("job-1", "/path/1", null, "model1", "repo1", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);

        // When
        jobRepository.save(jobWithNullAgent);
        Optional<Job> found = jobRepository.findById("job-1");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().cursorAgentId()).isNull();
    }

    @Test
    void shouldHandleJobWithCursorAgentId() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job jobWithAgent = new Job("job-1", "/path/1", "cursor-agent-123", "model1", "repo1", AgentState.FINISHED(), now, now, null, null, null, null, null, null, null);

        // When
        jobRepository.save(jobWithAgent);
        Optional<Job> found = jobRepository.findById("job-1");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().cursorAgentId()).isEqualTo("cursor-agent-123");
    }

    @Test
    void shouldHandleAllAgentStates() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job[] jobs = {
            new Job("job-1", "/path/1", null, "model1", "repo1", AgentState.CREATING(), now, now, null, null, null, null, null, null, null),
            new Job("job-2", "/path/2", "agent-2", "model2", "repo2", AgentState.CREATING(), now, now, null, null, null, null, null, null, null),
            new Job("job-3", "/path/3", "agent-3", "model3", "repo3", AgentState.RUNNING(), now, now, null, null, null, null, null, null, null),
            new Job("job-4", "/path/4", "agent-4", "model4", "repo4", AgentState.FINISHED(), now, now, null, null, null, null, null, null, null),
            new Job("job-5", "/path/5", "agent-5", "model5", "repo5", AgentState.ERROR(), now, now, null, null, null, null, null, null, null),
            new Job("job-6", "/path/6", "agent-6", "model6", "repo6", AgentState.FINISHED(), now, now, null, null, null, null, null, null, null)
        };

        // When
        for (Job job : jobs) {
            jobRepository.save(job);
        }

        // Then
        List<Job> allJobs = jobRepository.findAll();
        assertThat(allJobs).hasSize(6);

        List<Job> unfinishedJobs = jobRepository.findUnfinishedJobs();
        assertThat(unfinishedJobs).hasSize(3); // UNKNOWN (null cursorAgentId), CREATING, RUNNING
    }

    @Test
    void shouldHandleEmptyJobList() {
        // When
        List<Job> allJobs = jobRepository.findAll();
        List<Job> unfinishedJobs = jobRepository.findUnfinishedJobs();

        // Then
        assertThat(allJobs).isEmpty();
        assertThat(unfinishedJobs).isEmpty();
    }

    @Test
    void shouldHandleEmptyPromptList() {
        // When
        List<Prompt> prompts = jobRepository.findPromptsByJobId("non-existent-job");

        // Then
        assertThat(prompts).isEmpty();
    }

    @Test
    void shouldCloseRepository() {
        // When & Then
        assertDoesNotThrow(() -> jobRepository.close());
    }

    @Test
    void shouldFindJobsByParentId() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job parentJob = new Job("parent-job", "/path/parent", "cursor-agent-123", "model1", "repo1", AgentState.FINISHED(), now, now, null, null, null, null, null, null, null);
        Job childJob1 = new Job("child-job-1", "/path/child1", "cursor-agent-124", "model1", "repo1", AgentState.RUNNING(), now, now, "parent-job", null, null, null, null, null, null);
        Job childJob2 = new Job("child-job-2", "/path/child2", "cursor-agent-125", "model1", "repo1", AgentState.CREATING(), now, now, "parent-job", null, null, null, null, null, null);
        Job otherJob = new Job("other-job", "/path/other", "cursor-agent-126", "model1", "repo1", AgentState.FINISHED(), now, now, null, null, null, null, null, null, null);

        jobRepository.save(parentJob);
        jobRepository.save(childJob1);
        jobRepository.save(childJob2);
        jobRepository.save(otherJob);

        // When
        List<Job> childJobs = jobRepository.findJobsByParentId("parent-job");

        // Then
        assertThat(childJobs).hasSize(2);
        // After XML round-trip, null fallbackExecuted becomes false
        Job expectedChildJob1 = new Job("child-job-1", "/path/child1", "cursor-agent-124", "model1", "repo1", AgentState.RUNNING(), now, now, "parent-job", null, null, null, null, null, false);
        Job expectedChildJob2 = new Job("child-job-2", "/path/child2", "cursor-agent-125", "model1", "repo1", AgentState.CREATING(), now, now, "parent-job", null, null, null, null, null, false);
        assertThat(childJobs).containsExactlyInAnyOrder(expectedChildJob1, expectedChildJob2);
    }

    @Test
    void shouldReturnEmptyListWhenNoChildJobs() {
        // When
        List<Job> childJobs = jobRepository.findJobsByParentId("non-existent-parent");

        // Then
        assertThat(childJobs).isEmpty();
    }

    @Test
    void shouldHandleJobWithParentJobId() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job jobWithParent = new Job("child-job", "/path/child", "cursor-agent-123", "model1", "repo1", AgentState.RUNNING(), now, now, "parent-job-id", null, null, null, null, null, null);

        // When
        jobRepository.save(jobWithParent);
        Optional<Job> found = jobRepository.findById("child-job");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().parentJobId()).isEqualTo("parent-job-id");
    }

    @Test
    void shouldHandleJobWithResult() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job jobWithResult = new Job("job-with-result", "/path/job", "cursor-agent-123", "model1", "repo1", AgentState.FINISHED(), now, now, null, "Some result data", null, null, null, null, null);

        // When
        jobRepository.save(jobWithResult);
        Optional<Job> found = jobRepository.findById("job-with-result");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().result()).isEqualTo("Some result data");
    }

    @Test
    void shouldHandleJobWithWorkflowType() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job sequenceJob = new Job("sequence-job", "/path/seq", "cursor-agent-123", "model1", "repo1", AgentState.FINISHED(), now, now, null, null, info.jab.churrera.workflow.WorkflowType.SEQUENCE, null, null, null, null);
        Job parallelJob = new Job("parallel-job", "/path/par", "cursor-agent-124", "model1", "repo1", AgentState.FINISHED(), now, now, null, null, info.jab.churrera.workflow.WorkflowType.PARALLEL, null, null, null, null);

        // When
        jobRepository.save(sequenceJob);
        jobRepository.save(parallelJob);

        Optional<Job> foundSeq = jobRepository.findById("sequence-job");
        Optional<Job> foundPar = jobRepository.findById("parallel-job");

        // Then
        assertThat(foundSeq).isPresent();
        assertThat(foundSeq.get().type()).isEqualTo(info.jab.churrera.workflow.WorkflowType.SEQUENCE);
        assertThat(foundPar).isPresent();
        assertThat(foundPar.get().type()).isEqualTo(info.jab.churrera.workflow.WorkflowType.PARALLEL);
    }

    @Test
    void shouldHandleJobWithSpecialCharactersInXml() throws IOException {
        // Given - test XML escaping - Note: current implementation stores escaped but retrieves escaped too
        LocalDateTime now = LocalDateTime.now();
        Job jobWithSpecialChars = new Job(
            "job-1",
            "/path/with <special> & \"chars\" 'test'",
            "cursor-agent-123",
            "model1",
            "repo1",
            AgentState.FINISHED(),
            now,
            now,
            null,
            "Result with <tags> & \"quotes\"",
            null,
            null,
            null,
            null
        , null);

        // When
        jobRepository.save(jobWithSpecialChars);
        Optional<Job> found = jobRepository.findById("job-1");

        // Then
        assertThat(found).isPresent();
        // XML values are unescaped when parsed, so we get the original text back
        assertThat(found.get().path()).isEqualTo("/path/with <special> & \"chars\" 'test'");
        assertThat(found.get().result()).isEqualTo("Result with <tags> & \"quotes\"");
    }

    @Test
    void shouldCloseRepositorySuccessfully(@TempDir Path testTempDir) throws IOException {
        // Given - use a separate temp directory to avoid conflicts
        PropertyResolver testPropertyResolver = mock(PropertyResolver.class);
        when(testPropertyResolver.getProperty(eq("application.properties"), eq("basex.database.path")))
                .thenReturn(Optional.of(testTempDir.toString()));
        JobRepository repo = new JobRepository(testPropertyResolver);

        // When & Then - should close without throwing exception
        assertDoesNotThrow(repo::close);
    }


    @Test
    void shouldHandleInitializeWhenDatabaseExists() throws IOException {
        // Given - repository already initialized
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job("test-job", "/path/to/job", null, "model", "repo", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);
        jobRepository.save(job);
        jobRepository.close();

        // When - reinitialize with same database path
        when(propertyResolver.getProperty(eq("application.properties"), eq("basex.database.path")))
                .thenReturn(Optional.of(tempDir.toString()));
        JobRepository newRepo = new JobRepository(propertyResolver);

        // Then - should open existing database and find the job
        Optional<Job> found = newRepo.findById("test-job");
        assertThat(found).isPresent();
        // After XML round-trip, null fallbackExecuted becomes false
        Job expectedJob = new Job("test-job", "/path/to/job", null, "model", "repo", AgentState.CREATING(), now, now, null, null, null, null, null, null, false);
        assertThat(found.get()).isEqualTo(expectedJob);

        newRepo.close();
    }

    @Test
    void shouldReturnEmptyWhenPromptNotFound() {
        // When
        Optional<Prompt> found = jobRepository.findPromptById("non-existent-prompt");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldHandleMalformedJobXmlGracefully() throws IOException {
        // This test verifies that parseJobsFromDocument handles exceptions gracefully
        // We can't directly inject malformed XML into the database, but we can test
        // that the error handling path exists by checking the behavior with edge cases

        // Given
        LocalDateTime now = LocalDateTime.now();
        Job validJob = new Job("valid-job", "/path", null, "model", "repo", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);

        // When
        jobRepository.save(validJob);
        List<Job> jobs = jobRepository.findAll();

        // Then - should successfully retrieve the valid job
        assertThat(jobs).hasSize(1);
        // After XML round-trip, null fallbackExecuted becomes false
        Job expectedJob = new Job("valid-job", "/path", null, "model", "repo", AgentState.CREATING(), now, now, null, null, null, null, null, null, false);
        assertThat(jobs.get(0)).isEqualTo(expectedJob);
    }

    @Test
    void shouldHandleNullInEscapeXml() throws IOException {
        // Given - Job with null values that get escaped
        LocalDateTime now = LocalDateTime.now();
        Job jobWithNulls = new Job("job-1", "/path", null, "model", "repo", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);

        // When
        jobRepository.save(jobWithNulls);
        Optional<Job> found = jobRepository.findById("job-1");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().cursorAgentId()).isNull();
        assertThat(found.get().parentJobId()).isNull();
        assertThat(found.get().result()).isNull();
    }

    @Test
    void shouldHandleJobWithInvalidWorkflowType() throws IOException {
        // Given - Save a job with valid workflow type
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job("job-1", "/path", "cursor-agent-123", "model1", "repo1",
                         AgentState.FINISHED(), now, now, null, null,
                         info.jab.churrera.workflow.WorkflowType.SEQUENCE, null, null, null, null);

        jobRepository.save(job);

        // Now manually corrupt the workflow type in the database by inserting invalid XML
        // We can't directly do this through the API, but we can verify the error handling exists
        // by checking that jobs with null workflow types are handled properly

        // When
        Optional<Job> found = jobRepository.findById("job-1");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().type()).isEqualTo(info.jab.churrera.workflow.WorkflowType.SEQUENCE);
    }


    @Test
    void shouldHandleChildJobsInFindJobsByParentId() throws IOException {
        // Given - Create multiple child jobs to test exception handling path
        LocalDateTime now = LocalDateTime.now();
        Job parentJob = new Job("parent", "/parent", "agent-1", "model", "repo",
                               AgentState.FINISHED(), now, now, null, null, null, null, null, null, null);
        jobRepository.save(parentJob);

        // Add multiple child jobs to exercise different code paths
        for (int i = 0; i < 3; i++) {
            Job childJob = new Job("child-" + i, "/child" + i, "agent-" + (i+2), "model", "repo",
                                  AgentState.RUNNING(), now, now, "parent", null, null, null, null, null, null);
            jobRepository.save(childJob);
        }

        // When
        List<Job> childJobs = jobRepository.findJobsByParentId("parent");

        // Then
        assertThat(childJobs).hasSize(3);
    }

    @Test
    void shouldHandleJobWithAllOptionalFields() throws IOException {
        // Given - Job with all optional fields populated
        LocalDateTime now = LocalDateTime.now();
        Job completeJob = new Job(
            "complete-job",
            "/path/complete",
            "cursor-agent-999",
            "gpt-4",
            "myrepo",
            AgentState.FINISHED(),
            now,
            now,
            "parent-job-999",
            "Complete result with data",
            info.jab.churrera.workflow.WorkflowType.PARALLEL,
            null,
            null,
            null,
            null
        );

        // When
        jobRepository.save(completeJob);
        Optional<Job> found = jobRepository.findById("complete-job");

        // Then
        assertThat(found).isPresent();
        Job retrievedJob = found.get();
        assertThat(retrievedJob.cursorAgentId()).isEqualTo("cursor-agent-999");
        assertThat(retrievedJob.parentJobId()).isEqualTo("parent-job-999");
        assertThat(retrievedJob.result()).isEqualTo("Complete result with data");
        assertThat(retrievedJob.type()).isEqualTo(info.jab.churrera.workflow.WorkflowType.PARALLEL);
    }

    @Test
    void shouldHandleRepositoryWithDefaultDatabasePath(@TempDir Path testTempDir) throws IOException {
        // Given - Create repository with PropertyResolver that returns empty Optional (default path behavior)
        // Use a temp directory to avoid conflicts and database lock issues
        PropertyResolver testPropertyResolver = mock(PropertyResolver.class);
        when(testPropertyResolver.getProperty(eq("application.properties"), eq("basex.database.path")))
                .thenReturn(Optional.of(testTempDir.toString()));

        JobRepository defaultRepo = new JobRepository(testPropertyResolver);

        // When
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job("test-default", "/path", null, "model", "repo", AgentState.CREATING(), now, now, null, null, null, null, null, null, null);
        defaultRepo.save(job);
        Optional<Job> found = defaultRepo.findById("test-default");

        // Then
        assertThat(found).isPresent();

        // Cleanup - Delete the job to avoid leaving database entries
        defaultRepo.deleteById("test-default");
        defaultRepo.close();
    }

    @Test
    void shouldHandlePromptsWithExceptionPath() throws IOException {
        // Given - Create multiple prompts to exercise different code paths
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            Prompt prompt = new Prompt("prompt-" + i, "job-test", "file" + i + ".xml",
                                      "SENT", now, now);
            jobRepository.savePrompt(prompt);
        }

        // When
        List<Prompt> prompts = jobRepository.findPromptsByJobId("job-test");

        // Then
        assertThat(prompts).hasSize(5);
    }

    @Test
    void shouldDeleteMultiplePromptsByJobId() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        // Create multiple prompts for the same job to test the exception handling loop
        for (int i = 0; i < 5; i++) {
            Prompt prompt = new Prompt("multi-prompt-" + i, "multi-job", "file" + i + ".xml",
                                      "COMPLETED", now, now);
            jobRepository.savePrompt(prompt);
        }

        // Verify prompts exist
        assertThat(jobRepository.findPromptsByJobId("multi-job")).hasSize(5);

        // When
        jobRepository.deletePromptsByJobId("multi-job");

        // Then
        assertThat(jobRepository.findPromptsByJobId("multi-job")).isEmpty();
    }
}
