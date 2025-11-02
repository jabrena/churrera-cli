package info.jab.churrera.cli.commands;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowValidator;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.WorkflowParseException;
import info.jab.churrera.agent.AgentState;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NewJobRunCommand.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewJobRunCommandTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private WorkflowValidator workflowValidator;

    @Mock
    private WorkflowParser workflowParser;

    private Path tempDir;
    private Path testWorkflowFile;
    private String testJobPath;
    private NewJobRunCommand newJobRunCommand;

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
                    <prompt src="prompt1.pml" type="pml"/>
                    <prompt src="prompt2.pml" type="pml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile, workflowContent.getBytes());

        // Setup default mocks - successful validation and parsing
        // Use lenient() since not all tests will use these stubbings
        lenient().when(workflowValidator.validate(any(File.class)))
            .thenReturn(new WorkflowValidator.ValidationResult(true, List.of()));

        WorkflowData defaultWorkflowData = createDefaultWorkflowData();
        lenient().doReturn(defaultWorkflowData).when(workflowParser).parse(any(File.class));

        // Reset mocks before each test
        reset(jobRepository);

        newJobRunCommand = new NewJobRunCommand(jobRepository, testJobPath,
                                               workflowValidator, workflowParser);
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
        updatePrompts.add(new PromptInfo("prompt2.pml", "pml"));
        return new WorkflowData(
            new PromptInfo("prompt1.pml", "pml"),
            "test-model",
            "test-repo",
            updatePrompts
        );
    }

    /**
     * Creates a WorkflowData with default model/repository for mocking.
     */
    private WorkflowData createWorkflowDataWithDefaults() {
        List<PromptInfo> updatePrompts = new ArrayList<>();
        return new WorkflowData(
            new PromptInfo("prompt1.pml", "pml"),
            null, // null model to trigger defaults
            null, // null repository to trigger defaults
            updatePrompts
        );
    }

    @Test
    void testRun_WorkflowFileDoesNotExist() throws BaseXException, QueryException, WorkflowParseException, IOException {
        // Given
        String nonExistentPath = "/non/existent/path.xml";
        NewJobRunCommand commandWithNonExistentFile =
            new NewJobRunCommand(jobRepository, nonExistentPath,
                               workflowValidator, workflowParser);

        // When
        commandWithNonExistentFile.run();

        // Then
        verify(jobRepository, never()).save(any(Job.class));
        verify(jobRepository, never()).savePrompt(any(Prompt.class));
        verify(workflowValidator, never()).validate(any(File.class));
        verify(workflowParser, never()).parse(any(File.class));
    }

    @Test
    void testRun_WorkflowValidationFails() throws Exception {
        // Given
        WorkflowValidator.ValidationResult validationResult =
            new WorkflowValidator.ValidationResult(false,
                List.of("Validation error 1", "Validation error 2"));
        when(workflowValidator.validate(any(File.class))).thenReturn(validationResult);

        // When
        newJobRunCommand.run();

        // Then
        verify(workflowValidator).validate(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
        verify(jobRepository, never()).savePrompt(any(Prompt.class));
        verify(workflowParser, never()).parse(any(File.class));
    }

    @Test
    void testRun_WorkflowParseException() throws Exception {
        // Given
        when(workflowParser.parse(any(File.class)))
            .thenThrow(new WorkflowParseException("Parse error"));

        // When
        newJobRunCommand.run();

        // Then
        verify(workflowValidator).validate(any(File.class));
        verify(workflowParser).parse(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
        verify(jobRepository, never()).savePrompt(any(Prompt.class));
    }

    @Test
    void testRun_SuccessfulJobCreation_WithCorrectValues() throws Exception {
        // When
        newJobRunCommand.run();

        // Then - Combined test: verify job creation AND correct values AND prompts
        verify(jobRepository).save(argThat(job -> {
            assertEquals(testJobPath, job.path());
            assertEquals("test-model", job.model());
            assertEquals("test-repo", job.repository());
            assertEquals(AgentState.UNKNOWN, job.status());
            assertNull(job.cursorAgentId());
            assertNotNull(job.jobId());
            assertNotNull(job.createdAt());
            assertNotNull(job.lastUpdate());
            return true;
        }));

        verify(jobRepository, times(2)).savePrompt(argThat(prompt -> {
            assertNotNull(prompt.promptId());
            assertEquals("UNKNOWN", prompt.status());
            assertNotNull(prompt.createdAt());
            assertNotNull(prompt.lastUpdate());
            return true;
        }));
    }

    @Test
    void testRun_JobCreatedWithDefaultValues() throws Exception {
        // Given
        doReturn(createWorkflowDataWithDefaults())
            .when(workflowParser).parse(any(File.class));

        // When
        newJobRunCommand.run();

        // Then
        verify(jobRepository).save(argThat(job -> {
            assertEquals(testJobPath, job.path());
            assertEquals("default-model", job.model());
            assertEquals("default-repository", job.repository());
            assertEquals(AgentState.UNKNOWN, job.status());
            return true;
        }));
        verify(jobRepository, atLeastOnce()).savePrompt(any(Prompt.class));
    }

    @Test
    void testRun_JobRepositoryExceptions() throws Exception {
        // Test BaseXException
        doThrow(new BaseXException("Database error"))
            .when(jobRepository).save(any(Job.class));
        assertDoesNotThrow(() -> newJobRunCommand.run());
        verify(jobRepository).save(any(Job.class));
        reset(jobRepository);
        doReturn(createDefaultWorkflowData()).when(workflowParser).parse(any(File.class));

        // Test QueryException
        doThrow(new QueryException("Query error"))
            .when(jobRepository).save(any(Job.class));
        assertDoesNotThrow(() -> newJobRunCommand.run());
        verify(jobRepository).save(any(Job.class));
        reset(jobRepository);
        doReturn(createDefaultWorkflowData()).when(workflowParser).parse(any(File.class));

        // Test IOException
        doThrow(new IOException("IO error"))
            .when(jobRepository).save(any(Job.class));
        assertDoesNotThrow(() -> newJobRunCommand.run());
        verify(jobRepository).save(any(Job.class));
    }
}
