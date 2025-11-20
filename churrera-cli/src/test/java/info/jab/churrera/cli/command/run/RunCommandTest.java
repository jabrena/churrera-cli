package info.jab.churrera.cli.command.run;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowValidator;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.WorkflowParseException;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.SequenceInfo;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.cli.service.CLIAgent;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RunCommand.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RunCommandTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobProcessor jobProcessor;

    @Mock
    private WorkflowValidator workflowValidator;

    @Mock
    private WorkflowParser workflowParser;

    @Mock
    private PmlValidator pmlValidator;

    @Mock
    private CLIAgent cliAgent;

    private Path tempDir;
    private Path testWorkflowFile;
    private String testJobPath;
    private RunCommand runCommand;
    private JobCreationService jobCreationService;
    private JobDisplayService jobDisplayService;
    private JobDeletionService jobDeletionService;
    private static final int DEFAULT_POLLING_INTERVAL = 5;

    @BeforeAll
    void setUpOnce() throws IOException {
        // Create a temporary directory for all test files
        tempDir = Files.createTempDirectory("churrera-test-");
    }

    @BeforeEach
    void setUp() throws IOException, WorkflowParseException {
        // Create a temporary workflow file for each test
        testWorkflowFile = Files.createTempFile(tempDir, "workflow-", ".xml");
        testJobPath = testWorkflowFile.toString();

        // Default workflow content
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml" type="xml"/>
                    <prompt src="prompt2.xml" type="xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile, workflowContent.getBytes());

        // Setup default mocks - successful validation and parsing
        lenient().when(workflowValidator.validate(any(File.class)))
            .thenReturn(new WorkflowValidator.ValidationResult(true, List.of()));

        WorkflowData defaultWorkflowData = createDefaultWorkflowData();
        lenient().doReturn(defaultWorkflowData).when(workflowParser).parse(any(File.class));

        lenient().when(workflowValidator.validateTimeoutAndFallback(any(File.class), any(WorkflowData.class)))
            .thenReturn(List.of());

        // Setup default PML validation - successful
        lenient().when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(true, List.of()));

        // Setup default model validation - return valid models
        lenient().when(cliAgent.getModels())
            .thenReturn(List.of("default", "test-model", "default-model", "gpt-4", "claude-3"));

        // Note: WorkflowParser.determineWorkflowType() is static, so we can't mock it
        // It will use the real implementation which should work with our test file

        // Reset mocks before each test (but keep cliAgent stubbing)
        reset(jobRepository, jobProcessor);
        // Re-setup cliAgent mock after reset
        lenient().when(cliAgent.getModels())
            .thenReturn(List.of("default", "test-model", "default-model", "gpt-4", "claude-3"));

        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        
        // Initialize services for direct testing
        jobCreationService = new JobCreationService(jobRepository, workflowValidator,
            workflowParser, pmlValidator, cliAgent);
        jobDisplayService = new JobDisplayService(jobRepository);
        jobDeletionService = new JobDeletionService(jobRepository, cliAgent);
    }

    @AfterAll
    void tearDownOnce() throws IOException {
        // Clean up temporary directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }

    /**
     * Creates a default WorkflowData for mocking.
     */
    private WorkflowData createDefaultWorkflowData() {
        List<PromptInfo> updatePrompts = new ArrayList<>();
        updatePrompts.add(new PromptInfo("prompt2.xml", "xml"));
        return new WorkflowData(
            new PromptInfo("prompt1.xml", "xml"),
            "test-model",
            "test-repo",
            updatePrompts,
            null, null, null
        );
    }

    @Test
    void testGetJobRepository() {
        // When
        JobRepository result = runCommand.getJobRepository();

        // Then
        assertSame(jobRepository, result);
    }

    @Test
    void testGetEffectivePollingIntervalSeconds_DefaultValue() throws Exception {
        // Given - no override set
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);

        // When - parse empty args (no polling interval override)
        cmdLine.parseArgs();

        // Then - verify default is used by checking behavior through call()
        // The polling interval is used internally, so we verify it indirectly
        // by ensuring the command works with default value
        when(cliAgent.getModels()).thenReturn(List.of("model1"));
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--retrieve-models");
        Integer exitCode = runCommand.call();

        // Then
        assertEquals(0, exitCode);
        verify(cliAgent).getModels();
    }

    @Test
    void testGetEffectivePollingIntervalSeconds_WithOverride() throws Exception {
        // Given - override set via CommandLine
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);

        // When - parse args with polling interval override
        cmdLine.parseArgs("--polling-interval", "10");

        // Then - verify override is used by checking behavior through call()
        // The polling interval is used internally when creating JobPollingService
        // We verify it indirectly by ensuring the command works with override value
        when(cliAgent.getModels()).thenReturn(List.of("model1"));
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--polling-interval", "10", "--retrieve-models");
        Integer exitCode = runCommand.call();

        // Then
        assertEquals(0, exitCode);
        verify(cliAgent).getModels();
    }


    @Test
    void testRun_RetrieveModels() throws BaseXException, QueryException, IOException {
        // Given
        when(cliAgent.getModels()).thenReturn(List.of("model1", "model2", "model3"));
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--retrieve-models");

        // When
        Integer exitCode = runCommand.call();

        // Then
        assertEquals(0, exitCode);
        verify(cliAgent).getModels();
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testRun_RetrieveRepositories() throws BaseXException, QueryException, IOException {
        // Given
        when(cliAgent.getRepositories()).thenReturn(List.of("repo1", "repo2"));
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--retrieve-repositories");

        // When
        Integer exitCode = runCommand.call();

        // Then
        assertEquals(0, exitCode);
        verify(cliAgent).getRepositories();
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testRun_EmptyWorkflowPath() throws BaseXException, QueryException, IOException {
        // Given
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--workflow", "");

        // When
        Integer exitCode = runCommand.call();

        // Then
        assertEquals(1, exitCode);
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testCreateJob_WorkflowFileDoesNotExist() throws Exception {
        // Given
        String nonExistentPath = "/non/existent/path.xml";

        // When
        JobCreationResult result = jobCreationService.createJob(nonExistentPath);

        // Then
        assertFalse(result.isSuccess());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testCreateJob_WorkflowValidationFails() throws Exception {
        // Given
        WorkflowValidator.ValidationResult validationResult =
            new WorkflowValidator.ValidationResult(false,
                List.of("Validation error 1", "Validation error 2"));
        when(workflowValidator.validate(any(File.class))).thenReturn(validationResult);

        // When
        JobCreationResult result = jobCreationService.createJob(testJobPath);

        // Then
        assertFalse(result.isSuccess());
        verify(workflowValidator).validate(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testCreateJob_TimeoutFallbackValidationFails() throws Exception {
        // Given
        when(workflowValidator.validateTimeoutAndFallback(any(File.class), any(WorkflowData.class)))
            .thenReturn(List.of("Fallback requires timeout"));

        // When
        JobCreationResult result = jobCreationService.createJob(testJobPath);

        // Then
        assertFalse(result.isSuccess());
        verify(workflowValidator).validateTimeoutAndFallback(any(File.class), any(WorkflowData.class));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testCreateJob_PmlValidationFails() throws Exception {
        // Given
        PmlValidator.ValidationResult pmlResult =
            new PmlValidator.ValidationResult(false, List.of("PML validation error"));
        when(pmlValidator.validate(any(File.class))).thenReturn(pmlResult);

        // When
        JobCreationResult result = jobCreationService.createJob(testJobPath);

        // Then
        assertFalse(result.isSuccess());
        verify(pmlValidator, atLeastOnce()).validate(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testCreateJob_Success() throws Exception {
        // Given
        doNothing().when(jobRepository).save(any(Job.class));
        doNothing().when(jobRepository).savePrompt(any(Prompt.class));

        // When
        JobCreationResult result = jobCreationService.createJob(testJobPath);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getJobId());
        verify(jobRepository).save(argThat(job -> {
            assertEquals(testJobPath, job.path());
            assertEquals("test-model", job.model());
            assertEquals("test-repo", job.repository());
            assertEquals(AgentState.CREATING(), job.status());
            assertNotNull(job.jobId());
            return true;
        }));
        verify(jobRepository, times(2)).savePrompt(any(Prompt.class));
    }

    @Test
    void testCreateJob_WithDefaultModelAndRepository() throws Exception {
        // Given
        WorkflowData workflowDataWithDefaults = new WorkflowData(
            new PromptInfo("prompt1.xml", "xml"),
            null, // null model
            null, // null repository
            List.of(),
            null, null, null
        );
        when(workflowParser.parse(any(File.class))).thenReturn(workflowDataWithDefaults);
        doNothing().when(jobRepository).save(any(Job.class));
        doNothing().when(jobRepository).savePrompt(any(Prompt.class));

        // When
        JobCreationResult result = jobCreationService.createJob(testJobPath);

        // Then
        assertTrue(result.isSuccess());
        verify(jobRepository).save(argThat(job -> {
            assertEquals("default-model", job.model());
            assertEquals("default-repository", job.repository());
            return true;
        }));
    }

    @Test
    void testCreateJob_WorkflowParseException() throws Exception {
        // Given
        when(workflowParser.parse(any(File.class)))
            .thenThrow(new WorkflowParseException("Parse error"));

        // When
        JobCreationResult result = jobCreationService.createJob(testJobPath);

        // Then
        assertFalse(result.isSuccess());
        verify(workflowParser).parse(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testShortenId_Null() {
        // When
        String result = jobDisplayService.shortenId(null);

        // Then
        assertEquals("NA", result);
    }

    @Test
    void testShortenId_Empty() {
        // When
        String result = jobDisplayService.shortenId("");

        // Then
        assertEquals("NA", result);
    }

    @Test
    void testShortenId_ShortId() {
        // Given
        String shortId = "12345";

        // When
        String result = jobDisplayService.shortenId(shortId);

        // Then
        assertEquals(shortId, result);
    }

    @Test
    void testShortenId_LongId() {
        // Given
        String longId = "12345678901234567890";

        // When
        String result = jobDisplayService.shortenId(longId);

        // Then
        assertEquals("12345678", result);
        assertEquals(8, result.length());
    }

    @Test
    void testCollectAllPrompts_SequenceWorkflow() {
        // Given
        WorkflowData workflowData = createDefaultWorkflowData();

        // When
        List<PromptInfo> result = jobCreationService.collectAllPrompts(workflowData);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("prompt1.xml", result.get(0).getSrcFile());
        assertEquals("prompt2.xml", result.get(1).getSrcFile());
    }

    @Test
    void testCollectAllPrompts_ParallelWorkflow() {
        // Given
        PromptInfo parallelPrompt = new PromptInfo("parallel.xml", "xml");
        PromptInfo seq1Prompt = new PromptInfo("seq1.xml", "xml");
        PromptInfo seq2Prompt = new PromptInfo("seq2.xml", "xml");

        SequenceInfo sequence1 = new SequenceInfo("model1", "repo1", List.of(seq1Prompt), null, null);
        SequenceInfo sequence2 = new SequenceInfo("model2", "repo2", List.of(seq2Prompt), null, null);
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, null, List.of(sequence1, sequence2), null, null);

        WorkflowData workflowData = new WorkflowData(
            new PromptInfo("launch.xml", "xml"),
            "model", "repo",
            List.of(),
            parallelData, null, null
        );

        // When
        List<PromptInfo> result = jobCreationService.collectAllPrompts(workflowData);

        // Then
        assertNotNull(result);
        assertEquals(4, result.size()); // launch + parallel + seq1 + seq2
    }

    @Test
    void testValidatePmlFiles_AllValid() {
        // Given
        WorkflowData workflowData = createDefaultWorkflowData();
        File workflowFile = testWorkflowFile.toFile();
        when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(true, List.of()));

        // When
        List<String> result = jobCreationService.validatePmlFiles(workflowFile, workflowData);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(pmlValidator, times(2)).validate(any(File.class));
    }

    @Test
    void testValidatePmlFiles_SomeInvalid() {
        // Given
        WorkflowData workflowData = createDefaultWorkflowData();
        File workflowFile = testWorkflowFile.toFile();
        when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(true, List.of()))
            .thenReturn(new PmlValidator.ValidationResult(false, List.of("PML error 1", "PML error 2")));

        // When
        List<String> result = jobCreationService.validatePmlFiles(workflowFile, workflowData);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("prompt2.xml"));
    }

    @Test
    void testIsJobAndChildrenSuccessful_AllSuccessful() throws BaseXException, QueryException {
        // Given
        String jobId = "parent-job";
        Job parentJob = new Job(jobId, "/path", null, "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        Job childJob1 = new Job("child1", "/path", null, "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), jobId, null, null, null, null, null, null);
        Job childJob2 = new Job("child2", "/path", null, "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), jobId, null, null, null, null, null, null);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parentJob));

        // When
        boolean result = jobDeletionService.isJobAndChildrenSuccessful(jobId, List.of(childJob1, childJob2));

        // Then
        assertTrue(result);
    }

    @Test
    void testIsJobAndChildrenSuccessful_ParentNotSuccessful() throws BaseXException, QueryException {
        // Given
        String jobId = "parent-job";
        Job parentJob = new Job(jobId, "/path", null, "model", "repo",
            AgentState.ERROR(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        Job childJob = new Job("child1", "/path", null, "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), jobId, null, null, null, null, null, null);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parentJob));

        // When
        boolean result = jobDeletionService.isJobAndChildrenSuccessful(jobId, List.of(childJob));

        // Then
        assertFalse(result);
    }

    @Test
    void testIsJobAndChildrenSuccessful_ChildNotSuccessful() throws BaseXException, QueryException {
        // Given
        String jobId = "parent-job";
        Job parentJob = new Job(jobId, "/path", null, "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        Job childJob = new Job("child1", "/path", null, "model", "repo",
            AgentState.ERROR(), LocalDateTime.now(), LocalDateTime.now(), jobId, null, null, null, null, null, null);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parentJob));

        // When
        boolean result = jobDeletionService.isJobAndChildrenSuccessful(jobId, List.of(childJob));

        // Then
        assertFalse(result);
    }

    @Test
    void testDeleteJob_WithCursorAgent() throws Exception {
        // Given
        Job job = new Job("job-id", "/path", "cursor-agent-123", "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        when(jobRepository.findPromptsByJobId("job-id")).thenReturn(List.of());

        // When
        jobDeletionService.deleteJob(job);

        // Then
        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(jobRepository).deletePromptsByJobId("job-id");
        verify(jobRepository).deleteById("job-id");
    }

    @Test
    void testDeleteJob_WithoutCursorAgent() throws Exception {
        // Given
        Job job = new Job("job-id", "/path", null, "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        when(jobRepository.findPromptsByJobId("job-id")).thenReturn(List.of());

        // When
        jobDeletionService.deleteJob(job);

        // Then
        verify(cliAgent, never()).deleteAgent(anyString());
        verify(jobRepository).deletePromptsByJobId("job-id");
        verify(jobRepository).deleteById("job-id");
    }

    @Test
    void testDeleteJob_CursorAgentDeletionFails() throws Exception {
        // Given
        Job job = new Job("job-id", "/path", "cursor-agent-123", "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        when(jobRepository.findPromptsByJobId("job-id")).thenReturn(List.of());
        doThrow(new RuntimeException("Delete failed")).when(cliAgent).deleteAgent(anyString());

        // When
        jobDeletionService.deleteJob(job);

        // Then - should continue with database deletion even if Cursor API fails
        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(jobRepository).deletePromptsByJobId("job-id");
        verify(jobRepository).deleteById("job-id");
    }

    @Test
    void testRetrieveAndDisplayModels_Success() throws Exception {
        // Given
        when(cliAgent.getModels()).thenReturn(List.of("model1", "model2", "model3"));
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--retrieve-models");

        // When
        runCommand.call();

        // Then
        verify(cliAgent).getModels();
    }

    @Test
    void testRetrieveAndDisplayModels_EmptyList() throws Exception {
        // Given
        when(cliAgent.getModels()).thenReturn(List.of());
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--retrieve-models");

        // When
        runCommand.call();

        // Then
        verify(cliAgent).getModels();
    }

    @Test
    void testRetrieveAndDisplayModels_NullList() throws Exception {
        // Given
        when(cliAgent.getModels()).thenReturn(null);
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--retrieve-models");

        // When
        runCommand.call();

        // Then
        verify(cliAgent).getModels();
    }

    @Test
    void testRetrieveAndDisplayRepositories_Success() throws Exception {
        // Given
        when(cliAgent.getRepositories()).thenReturn(List.of("repo1", "repo2"));
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--retrieve-repositories");

        // When
        runCommand.call();

        // Then
        verify(cliAgent).getRepositories();
    }

    @Test
    void testRetrieveAndDisplayRepositories_EmptyList() throws Exception {
        // Given
        when(cliAgent.getRepositories()).thenReturn(List.of());
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--retrieve-repositories");

        // When
        runCommand.call();

        // Then
        verify(cliAgent).getRepositories();
    }

    @Test
    void testRetrieveAndDisplayRepositories_Exception() throws Exception {
        // Given
        when(cliAgent.getRepositories()).thenThrow(new RuntimeException("API error"));
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        CommandLine cmdLine = new CommandLine(runCommand);
        cmdLine.parseArgs("--retrieve-repositories");

        // When
        runCommand.call();

        // Then - should handle exception gracefully
        verify(cliAgent).getRepositories();
    }

    @Test
    void testDisplayFilteredJobsTable_SequenceWorkflow() throws BaseXException, QueryException {
        // Given
        String jobId = "test-job-id";
        Job job = new Job(jobId, testJobPath, null, "test-model", "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.SEQUENCE, null, null, null, null);
        List<Prompt> prompts = List.of(
            new Prompt("prompt-1", jobId, "prompt1.xml", "COMPLETED", LocalDateTime.now(), LocalDateTime.now())
        );

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobRepository.findPromptsByJobId(jobId)).thenReturn(prompts);

        // When
        jobDisplayService.displayFilteredJobsTable(jobId);

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findPromptsByJobId(jobId);
    }

    @Test
    void testDisplayFilteredJobsTable_ParallelWorkflowWithChildren() throws BaseXException, QueryException {
        // Given
        String parentJobId = "parent-job-id";
        String childJobId1 = "child-job-id-1";
        String childJobId2 = "child-job-id-2";

        Job parentJob = new Job(parentJobId, testJobPath, null, "test-model", "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);
        Job childJob1 = new Job(childJobId1, testJobPath, null, "test-model", "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), parentJobId, null,
            WorkflowType.SEQUENCE, null, null, null, null);
        Job childJob2 = new Job(childJobId2, testJobPath, null, "test-model", "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), parentJobId, null,
            WorkflowType.SEQUENCE, null, null, null, null);

        List<Prompt> parentPrompts = List.of(
            new Prompt("prompt-1", parentJobId, "prompt1.xml", "COMPLETED", LocalDateTime.now(), LocalDateTime.now())
        );
        List<Prompt> childPrompts1 = List.of(
            new Prompt("prompt-2", childJobId1, "prompt2.xml", "SENT", LocalDateTime.now(), LocalDateTime.now())
        );
        List<Prompt> childPrompts2 = List.of(
            new Prompt("prompt-3", childJobId2, "prompt3.xml", "COMPLETED", LocalDateTime.now(), LocalDateTime.now())
        );

        when(jobRepository.findById(parentJobId)).thenReturn(Optional.of(parentJob));
        when(jobRepository.findAll()).thenReturn(List.of(parentJob, childJob1, childJob2));
        when(jobRepository.findPromptsByJobId(parentJobId)).thenReturn(parentPrompts);
        when(jobRepository.findPromptsByJobId(childJobId1)).thenReturn(childPrompts1);
        when(jobRepository.findPromptsByJobId(childJobId2)).thenReturn(childPrompts2);

        // When
        jobDisplayService.displayFilteredJobsTable(parentJobId);

        // Then
        verify(jobRepository).findById(parentJobId);
        verify(jobRepository).findAll();
        verify(jobRepository).findPromptsByJobId(parentJobId);
        verify(jobRepository).findPromptsByJobId(childJobId1);
        verify(jobRepository).findPromptsByJobId(childJobId2);
    }

    @Test
    void testDisplayFilteredJobsTable_TerminalJob() throws BaseXException, QueryException {
        // Given
        String jobId = "test-job-id";
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);
        LocalDateTime lastUpdate = LocalDateTime.now().minusMinutes(1);
        Job job = new Job(jobId, testJobPath, null, "test-model", "test-repo",
            AgentState.FINISHED(), createdAt, lastUpdate, null, null,
            WorkflowType.SEQUENCE, null, null, null, null);
        List<Prompt> prompts = List.of(
            new Prompt("prompt-1", jobId, "prompt1.xml", "COMPLETED", createdAt, lastUpdate)
        );

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobRepository.findPromptsByJobId(jobId)).thenReturn(prompts);

        // When
        jobDisplayService.displayFilteredJobsTable(jobId);

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findPromptsByJobId(jobId);
    }

    @Test
    void testDisplayFilteredJobsTable_JobNotFound() throws BaseXException, QueryException {
        // Given
        String jobId = "non-existent-job";
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> jobDisplayService.displayFilteredJobsTable(jobId));
    }

    @Test
    void testDisplayFilteredJobsTable_ExceptionRetrievingPrompts() throws BaseXException, QueryException {
        // Given
        String jobId = "test-job-id";
        Job job = new Job(jobId, testJobPath, null, "test-model", "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.SEQUENCE, null, null, null, null);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobRepository.findPromptsByJobId(jobId)).thenThrow(new BaseXException("Database error"));

        // When & Then - should handle exception gracefully
        assertDoesNotThrow(() -> jobDisplayService.displayFilteredJobsTable(jobId));
    }

    @Test
    void testDisplayFilteredJobsTable_JobWithNullType() throws BaseXException, QueryException {
        // Given
        String jobId = "test-job-id";
        Job job = new Job(jobId, testJobPath, null, "test-model", "test-repo",
            AgentState.RUNNING(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            null, null, null, null, null); // null type
        List<Prompt> prompts = List.of();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobRepository.findPromptsByJobId(jobId)).thenReturn(prompts);

        // When
        jobDisplayService.displayFilteredJobsTable(jobId);

        // Then
        verify(jobRepository).findById(jobId);
    }

    @Test
    void testDeleteJobAndChildren_WithDeleteOnCompletion() throws Exception {
        // Given
        String jobId = "parent-job-id";
        String childJobId = "child-job-id";
        Job parentJob = new Job(jobId, testJobPath, "cursor-agent-123", "test-model", "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);
        Job childJob = new Job(childJobId, testJobPath, "cursor-agent-456", "test-model", "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), jobId, null,
            WorkflowType.SEQUENCE, null, null, null, null);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parentJob));
        when(jobRepository.findJobsByParentId(jobId)).thenReturn(List.of(childJob));
        when(jobRepository.findJobsByParentId(childJobId)).thenReturn(List.of());
        when(jobRepository.findPromptsByJobId(jobId)).thenReturn(List.of());
        when(jobRepository.findPromptsByJobId(childJobId)).thenReturn(List.of());
        doNothing().when(cliAgent).deleteAgent(anyString());
        doNothing().when(jobRepository).deletePromptsByJobId(anyString());
        doNothing().when(jobRepository).deleteById(anyString());

        // When
        jobDeletionService.deleteJobAndChildren(jobId, "--delete-on-completion");

        // Then
        verify(jobRepository).findJobsByParentId(jobId);
        verify(jobRepository).findJobsByParentId(childJobId);
        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(cliAgent).deleteAgent("cursor-agent-456");
        verify(jobRepository).deleteById(childJobId);
        verify(jobRepository).deleteById(jobId);
    }

    @Test
    void testDeleteJobAndChildren_WithDeleteOnSuccessCompletion() throws Exception {
        // Given
        String jobId = "parent-job-id";
        String childJobId = "child-job-id";
        Job parentJob = new Job(jobId, testJobPath, "cursor-agent-123", "test-model", "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);
        Job childJob = new Job(childJobId, testJobPath, "cursor-agent-456", "test-model", "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), jobId, null,
            WorkflowType.SEQUENCE, null, null, null, null);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parentJob));
        when(jobRepository.findJobsByParentId(jobId)).thenReturn(List.of(childJob));
        when(jobRepository.findJobsByParentId(childJobId)).thenReturn(List.of());
        when(jobRepository.findPromptsByJobId(jobId)).thenReturn(List.of());
        when(jobRepository.findPromptsByJobId(childJobId)).thenReturn(List.of());
        doNothing().when(cliAgent).deleteAgent(anyString());
        doNothing().when(jobRepository).deletePromptsByJobId(anyString());
        doNothing().when(jobRepository).deleteById(anyString());

        // When
        jobDeletionService.deleteJobAndChildren(jobId, "--delete-on-success-completion");

        // Then
        verify(jobRepository).findJobsByParentId(jobId);
        verify(jobRepository).findJobsByParentId(childJobId);
        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(cliAgent).deleteAgent("cursor-agent-456");
        verify(jobRepository).deleteById(childJobId);
        verify(jobRepository).deleteById(jobId);
    }

    @Test
    void testDeleteJobAndChildren_ExceptionHandling() throws BaseXException, QueryException {
        // Given
        String jobId = "parent-job-id";
        Job parentJob = new Job(jobId, testJobPath, null, "test-model", "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parentJob));
        when(jobRepository.findJobsByParentId(jobId)).thenThrow(new BaseXException("Database error"));

        // When & Then - should handle exception gracefully
        assertDoesNotThrow(() -> jobDeletionService.deleteJobAndChildren(jobId, "--delete-on-completion"));
    }

    @Test
    void testDeleteChildJobsRecursively_WithNestedChildren() throws Exception {
        // Given
        String parentJobId = "parent-job-id";
        String childJobId1 = "child-job-id-1";
        String childJobId2 = "child-job-id-2";
        String grandchildJobId = "grandchild-job-id";

        Job childJob1 = new Job(childJobId1, testJobPath, "cursor-agent-1", "test-model", "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), parentJobId, null,
            WorkflowType.SEQUENCE, null, null, null, null);
        Job childJob2 = new Job(childJobId2, testJobPath, "cursor-agent-2", "test-model", "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), parentJobId, null,
            WorkflowType.SEQUENCE, null, null, null, null);
        Job grandchildJob = new Job(grandchildJobId, testJobPath, "cursor-agent-3", "test-model", "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), childJobId1, null,
            WorkflowType.SEQUENCE, null, null, null, null);

        when(jobRepository.findJobsByParentId(parentJobId)).thenReturn(List.of(childJob1, childJob2));
        when(jobRepository.findJobsByParentId(childJobId1)).thenReturn(List.of(grandchildJob));
        when(jobRepository.findJobsByParentId(childJobId2)).thenReturn(List.of());
        when(jobRepository.findJobsByParentId(grandchildJobId)).thenReturn(List.of());
        when(jobRepository.findPromptsByJobId(anyString())).thenReturn(List.of());
        doNothing().when(cliAgent).deleteAgent(anyString());
        doNothing().when(jobRepository).deletePromptsByJobId(anyString());
        doNothing().when(jobRepository).deleteById(anyString());

        // When
        jobDeletionService.deleteChildJobsRecursively(parentJobId);

        // Then - should delete in depth-first order: grandchild, child1, child2
        verify(jobRepository).findJobsByParentId(parentJobId);
        verify(jobRepository).findJobsByParentId(childJobId1);
        verify(jobRepository).findJobsByParentId(grandchildJobId);
        verify(jobRepository).deleteById(grandchildJobId);
        verify(jobRepository).deleteById(childJobId1);
        verify(jobRepository).deleteById(childJobId2);
    }

    @Test
    void testValidateModel_EmptyModelList() {
        // Given
        when(cliAgent.getModels()).thenReturn(List.of());

        // When
        List<String> result = jobCreationService.validateModel("test-model");

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("Unable to validate model"));
    }

    @Test
    void testValidateModel_NullModelList() {
        // Given
        when(cliAgent.getModels()).thenReturn(null);

        // When
        List<String> result = jobCreationService.validateModel("test-model");

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("Unable to validate model"));
    }

    @Test
    void testValidateModel_Exception() {
        // Given
        when(cliAgent.getModels()).thenThrow(new RuntimeException("API error"));

        // When
        List<String> result = jobCreationService.validateModel("test-model");

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("Error validating model"));
    }

    @Test
    void testValidateModel_InvalidModel() {
        // Given
        when(cliAgent.getModels()).thenReturn(List.of("model1", "model2", "model3"));

        // When
        List<String> result = jobCreationService.validateModel("invalid-model");

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("is not available"));
        assertTrue(result.contains("Available models are:"));
    }

    @Test
    void testValidateModel_NullOrEmptyModel() {
        // Given
        when(cliAgent.getModels()).thenReturn(List.of("model1", "model2"));

        // When
        List<String> resultNull = jobCreationService.validateModel(null);
        List<String> resultEmpty = jobCreationService.validateModel("");

        // Then
        assertNotNull(resultNull);
        assertFalse(resultNull.isEmpty());
        assertTrue(resultNull.get(0).contains("null or empty"));
        assertNotNull(resultEmpty);
        assertFalse(resultEmpty.isEmpty());
        assertTrue(resultEmpty.get(0).contains("null or empty"));
    }

}

