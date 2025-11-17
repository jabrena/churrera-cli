package info.jab.churrera.cli.command;

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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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

        // Note: WorkflowParser.determineWorkflowType() is static, so we can't mock it
        // It will use the real implementation which should work with our test file

        // Reset mocks before each test
        reset(jobRepository, jobProcessor, cliAgent);

        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
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

        // When
        Method method = RunCommand.class.getDeclaredMethod("getEffectivePollingIntervalSeconds");
        method.setAccessible(true);
        int result = (Integer) method.invoke(runCommand);

        // Then
        assertEquals(DEFAULT_POLLING_INTERVAL, result);
    }

    @Test
    void testGetEffectivePollingIntervalSeconds_WithOverride() throws Exception {
        // Given - override set via reflection
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        
        // Set pollingIntervalOverride via reflection
        java.lang.reflect.Field field = RunCommand.class.getDeclaredField("pollingIntervalOverride");
        field.setAccessible(true);
        field.set(runCommand, 10);

        // When
        Method method = RunCommand.class.getDeclaredMethod("getEffectivePollingIntervalSeconds");
        method.setAccessible(true);
        int result = (Integer) method.invoke(runCommand);

        // Then
        assertEquals(10, result);
    }

    @Test
    void testRun_RetrieveModels() throws BaseXException, QueryException, IOException {
        // Given
        when(cliAgent.getModels()).thenReturn(List.of("model1", "model2", "model3"));
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        
        // Set retrieveModels via reflection
        try {
            java.lang.reflect.Field field = RunCommand.class.getDeclaredField("retrieveModels");
            field.setAccessible(true);
            field.set(runCommand, true);
        } catch (Exception e) {
            fail("Failed to set retrieveModels field");
        }

        // When
        runCommand.run();

        // Then
        verify(cliAgent).getModels();
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testRun_RetrieveRepositories() throws BaseXException, QueryException, IOException {
        // Given
        when(cliAgent.getRepositories()).thenReturn(List.of("repo1", "repo2"));
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        
        // Set retrieveRepositories via reflection
        try {
            java.lang.reflect.Field field = RunCommand.class.getDeclaredField("retrieveRepositories");
            field.setAccessible(true);
            field.set(runCommand, true);
        } catch (Exception e) {
            fail("Failed to set retrieveRepositories field");
        }

        // When
        runCommand.run();

        // Then
        verify(cliAgent).getRepositories();
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testRun_EmptyWorkflowPath() throws BaseXException, QueryException, IOException {
        // Given
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);
        
        // Set workflowPath to empty string via reflection
        try {
            java.lang.reflect.Field field = RunCommand.class.getDeclaredField("workflowPath");
            field.setAccessible(true);
            field.set(runCommand, "");
        } catch (Exception e) {
            fail("Failed to set workflowPath field");
        }

        // When
        runCommand.run();

        // Then
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testCreateJob_WorkflowFileDoesNotExist() throws Exception {
        // Given
        String nonExistentPath = "/non/existent/path.xml";
        runCommand = new RunCommand(jobRepository, jobProcessor, workflowValidator,
            workflowParser, pmlValidator, DEFAULT_POLLING_INTERVAL, cliAgent);

        // When
        Method method = RunCommand.class.getDeclaredMethod("createJob", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, nonExistentPath);

        // Then
        assertNull(result);
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
        Method method = RunCommand.class.getDeclaredMethod("createJob", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, testJobPath);

        // Then
        assertNull(result);
        verify(workflowValidator).validate(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testCreateJob_TimeoutFallbackValidationFails() throws Exception {
        // Given
        when(workflowValidator.validateTimeoutAndFallback(any(File.class), any(WorkflowData.class)))
            .thenReturn(List.of("Fallback requires timeout"));

        // When
        Method method = RunCommand.class.getDeclaredMethod("createJob", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, testJobPath);

        // Then
        assertNull(result);
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
        Method method = RunCommand.class.getDeclaredMethod("createJob", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, testJobPath);

        // Then
        assertNull(result);
        verify(pmlValidator, atLeastOnce()).validate(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testCreateJob_Success() throws Exception {
        // Given
        doNothing().when(jobRepository).save(any(Job.class));
        doNothing().when(jobRepository).savePrompt(any(Prompt.class));

        // When
        Method method = RunCommand.class.getDeclaredMethod("createJob", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, testJobPath);

        // Then
        assertNotNull(result);
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
        Method method = RunCommand.class.getDeclaredMethod("createJob", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, testJobPath);

        // Then
        assertNotNull(result);
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
        Method method = RunCommand.class.getDeclaredMethod("createJob", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, testJobPath);

        // Then
        assertNull(result);
        verify(workflowParser).parse(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testShortenId_Null() throws Exception {
        // When
        Method method = RunCommand.class.getDeclaredMethod("shortenId", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, (String) null);

        // Then
        assertEquals("NA", result);
    }

    @Test
    void testShortenId_Empty() throws Exception {
        // When
        Method method = RunCommand.class.getDeclaredMethod("shortenId", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, "");

        // Then
        assertEquals("NA", result);
    }

    @Test
    void testShortenId_ShortId() throws Exception {
        // Given
        String shortId = "12345";

        // When
        Method method = RunCommand.class.getDeclaredMethod("shortenId", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, shortId);

        // Then
        assertEquals(shortId, result);
    }

    @Test
    void testShortenId_LongId() throws Exception {
        // Given
        String longId = "12345678901234567890";

        // When
        Method method = RunCommand.class.getDeclaredMethod("shortenId", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(runCommand, longId);

        // Then
        assertEquals("12345678", result);
        assertEquals(8, result.length());
    }

    @Test
    void testCollectAllPrompts_SequenceWorkflow() throws Exception {
        // Given
        WorkflowData workflowData = createDefaultWorkflowData();

        // When
        Method method = RunCommand.class.getDeclaredMethod("collectAllPrompts", WorkflowData.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<PromptInfo> result = (List<PromptInfo>) method.invoke(runCommand, workflowData);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("prompt1.xml", result.get(0).getSrcFile());
        assertEquals("prompt2.xml", result.get(1).getSrcFile());
    }

    @Test
    void testCollectAllPrompts_ParallelWorkflow() throws Exception {
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
        Method method = RunCommand.class.getDeclaredMethod("collectAllPrompts", WorkflowData.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<PromptInfo> result = (List<PromptInfo>) method.invoke(runCommand, workflowData);

        // Then
        assertNotNull(result);
        assertEquals(4, result.size()); // launch + parallel + seq1 + seq2
    }

    @Test
    void testValidatePmlFiles_AllValid() throws Exception {
        // Given
        WorkflowData workflowData = createDefaultWorkflowData();
        File workflowFile = testWorkflowFile.toFile();
        when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(true, List.of()));

        // When
        Method method = RunCommand.class.getDeclaredMethod("validatePmlFiles", File.class, WorkflowData.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(runCommand, workflowFile, workflowData);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(pmlValidator, times(2)).validate(any(File.class));
    }

    @Test
    void testValidatePmlFiles_SomeInvalid() throws Exception {
        // Given
        WorkflowData workflowData = createDefaultWorkflowData();
        File workflowFile = testWorkflowFile.toFile();
        when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(true, List.of()))
            .thenReturn(new PmlValidator.ValidationResult(false, List.of("PML error 1", "PML error 2")));

        // When
        Method method = RunCommand.class.getDeclaredMethod("validatePmlFiles", File.class, WorkflowData.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(runCommand, workflowFile, workflowData);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("prompt2.xml"));
    }

    @Test
    void testIsJobAndChildrenSuccessful_AllSuccessful() throws Exception {
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
        Method method = RunCommand.class.getDeclaredMethod("isJobAndChildrenSuccessful", String.class, List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(runCommand, jobId, List.of(childJob1, childJob2));

        // Then
        assertTrue(result);
    }

    @Test
    void testIsJobAndChildrenSuccessful_ParentNotSuccessful() throws Exception {
        // Given
        String jobId = "parent-job";
        Job parentJob = new Job(jobId, "/path", null, "model", "repo",
            AgentState.ERROR(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        Job childJob = new Job("child1", "/path", null, "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), jobId, null, null, null, null, null, null);
        
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parentJob));

        // When
        Method method = RunCommand.class.getDeclaredMethod("isJobAndChildrenSuccessful", String.class, List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(runCommand, jobId, List.of(childJob));

        // Then
        assertFalse(result);
    }

    @Test
    void testIsJobAndChildrenSuccessful_ChildNotSuccessful() throws Exception {
        // Given
        String jobId = "parent-job";
        Job parentJob = new Job(jobId, "/path", null, "model", "repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        Job childJob = new Job("child1", "/path", null, "model", "repo",
            AgentState.ERROR(), LocalDateTime.now(), LocalDateTime.now(), jobId, null, null, null, null, null, null);
        
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parentJob));

        // When
        Method method = RunCommand.class.getDeclaredMethod("isJobAndChildrenSuccessful", String.class, List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(runCommand, jobId, List.of(childJob));

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
        Method method = RunCommand.class.getDeclaredMethod("deleteJob", Job.class);
        method.setAccessible(true);
        method.invoke(runCommand, job);

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
        Method method = RunCommand.class.getDeclaredMethod("deleteJob", Job.class);
        method.setAccessible(true);
        method.invoke(runCommand, job);

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
        Method method = RunCommand.class.getDeclaredMethod("deleteJob", Job.class);
        method.setAccessible(true);
        method.invoke(runCommand, job);

        // Then - should continue with database deletion even if Cursor API fails
        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(jobRepository).deletePromptsByJobId("job-id");
        verify(jobRepository).deleteById("job-id");
    }

    @Test
    void testRetrieveAndDisplayModels_Success() throws Exception {
        // Given
        when(cliAgent.getModels()).thenReturn(List.of("model1", "model2", "model3"));

        // When
        Method method = RunCommand.class.getDeclaredMethod("retrieveAndDisplayModels");
        method.setAccessible(true);
        method.invoke(runCommand);

        // Then
        verify(cliAgent).getModels();
    }

    @Test
    void testRetrieveAndDisplayModels_EmptyList() throws Exception {
        // Given
        when(cliAgent.getModels()).thenReturn(List.of());

        // When
        Method method = RunCommand.class.getDeclaredMethod("retrieveAndDisplayModels");
        method.setAccessible(true);
        method.invoke(runCommand);

        // Then
        verify(cliAgent).getModels();
    }

    @Test
    void testRetrieveAndDisplayModels_NullList() throws Exception {
        // Given
        when(cliAgent.getModels()).thenReturn(null);

        // When
        Method method = RunCommand.class.getDeclaredMethod("retrieveAndDisplayModels");
        method.setAccessible(true);
        method.invoke(runCommand);

        // Then
        verify(cliAgent).getModels();
    }

    @Test
    void testRetrieveAndDisplayRepositories_Success() throws Exception {
        // Given
        when(cliAgent.getRepositories()).thenReturn(List.of("repo1", "repo2"));

        // When
        Method method = RunCommand.class.getDeclaredMethod("retrieveAndDisplayRepositories");
        method.setAccessible(true);
        method.invoke(runCommand);

        // Then
        verify(cliAgent).getRepositories();
    }

    @Test
    void testRetrieveAndDisplayRepositories_EmptyList() throws Exception {
        // Given
        when(cliAgent.getRepositories()).thenReturn(List.of());

        // When
        Method method = RunCommand.class.getDeclaredMethod("retrieveAndDisplayRepositories");
        method.setAccessible(true);
        method.invoke(runCommand);

        // Then
        verify(cliAgent).getRepositories();
    }

    @Test
    void testRetrieveAndDisplayRepositories_Exception() throws Exception {
        // Given
        when(cliAgent.getRepositories()).thenThrow(new RuntimeException("API error"));

        // When
        Method method = RunCommand.class.getDeclaredMethod("retrieveAndDisplayRepositories");
        method.setAccessible(true);
        method.invoke(runCommand);

        // Then - should handle exception gracefully
        verify(cliAgent).getRepositories();
    }
}

