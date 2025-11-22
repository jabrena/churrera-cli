package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.WorkflowParseException;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowValidator;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private PmlValidator pmlValidator;

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

        // Setup default PML validation - successful
        lenient().when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(true, List.of()));

        // Reset mocks before each test
        reset(jobRepository);

        newJobRunCommand = new NewJobRunCommand(jobRepository, testJobPath,
                                               workflowValidator, workflowParser, pmlValidator);
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
            updatePrompts,
            null, null, null
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
            updatePrompts,
            null, null, null
        );
    }

    @Test
    void testRun_WorkflowFileDoesNotExist() throws Exception, WorkflowParseException, IOException {
        // Given
        String nonExistentPath = "/non/existent/path.xml";
        NewJobRunCommand commandWithNonExistentFile =
            new NewJobRunCommand(jobRepository, nonExistentPath,
                               workflowValidator, workflowParser, pmlValidator);

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
            assertEquals(AgentState.CREATING(), job.status());
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
            assertEquals(AgentState.CREATING(), job.status());
            return true;
        }));
        verify(jobRepository, atLeastOnce()).savePrompt(any(Prompt.class));
    }

    @Test
    void testRun_JobRepositoryExceptions() throws Exception {
        // Test BaseXException equivalent
        doThrow(new RuntimeException("Database error"))
            .when(jobRepository).save(any(Job.class));
        assertThatThrownBy(() -> newJobRunCommand.run())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database error");
        verify(jobRepository).save(any(Job.class));
        reset(jobRepository);
        doReturn(createDefaultWorkflowData()).when(workflowParser).parse(any(File.class));

        // Test QueryException equivalent
        doThrow(new RuntimeException("Query error"))
            .when(jobRepository).save(any(Job.class));
        assertThatThrownBy(() -> newJobRunCommand.run())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Query error");
        verify(jobRepository).save(any(Job.class));
        reset(jobRepository);
        doReturn(createDefaultWorkflowData()).when(workflowParser).parse(any(File.class));

        // Test IOException
        doThrow(new IOException("IO error"))
            .when(jobRepository).save(any(Job.class));
        assertDoesNotThrow(() -> newJobRunCommand.run());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_PmlValidationFails() throws Exception {
        // Given - Create PML files in the temp directory
        Path prompt1File = tempDir.resolve("prompt1.xml");
        Path prompt2File = tempDir.resolve("prompt2.xml");
        Files.write(prompt1File, "<prompt><role>Test</role></prompt>".getBytes());
        Files.write(prompt2File, "<prompt><role>Test</role></prompt>".getBytes());

        // Setup workflow data with .xml files
        WorkflowData workflowData = new WorkflowData(
            new PromptInfo("prompt1.xml", "pml"),
            "test-model",
            "test-repo",
            List.of(new PromptInfo("prompt2.xml", "pml")),
            null, null, null
        );
        when(workflowParser.parse(any(File.class))).thenReturn(workflowData);

        // Mock PML validation to fail
        when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(false,
                List.of("Error: Invalid PML structure at line 1")));

        // When
        newJobRunCommand.run();

        // Then
        verify(workflowValidator).validate(any(File.class));
        verify(workflowParser).parse(any(File.class));
        verify(pmlValidator, atLeastOnce()).validate(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
        verify(jobRepository, never()).savePrompt(any(Prompt.class));
    }

    @Test
    void testRun_PmlValidationSucceeds() throws Exception {
        // Given - Create PML files in the temp directory
        Path prompt1File = tempDir.resolve("prompt1.xml");
        Path prompt2File = tempDir.resolve("prompt2.xml");
        Files.write(prompt1File, "<prompt><role>Test</role></prompt>".getBytes());
        Files.write(prompt2File, "<prompt><role>Test</role></prompt>".getBytes());

        // Setup workflow data with .xml files
        WorkflowData workflowData = new WorkflowData(
            new PromptInfo("prompt1.xml", "pml"),
            "test-model",
            "test-repo",
            List.of(new PromptInfo("prompt2.xml", "pml")),
            null, null, null
        );
        when(workflowParser.parse(any(File.class))).thenReturn(workflowData);

        // Mock PML validation to succeed
        when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(true, List.of()));

        // When
        newJobRunCommand.run();

        // Then
        verify(workflowValidator).validate(any(File.class));
        verify(workflowParser).parse(any(File.class));
        verify(pmlValidator, times(2)).validate(any(File.class)); // Both PML files validated
        verify(jobRepository).save(any(Job.class));
        verify(jobRepository, times(2)).savePrompt(any(Prompt.class));
    }

    @Test
    void testRun_NonPmlFilesSkipped() throws Exception {
        // Given - Create non-PML files in the temp directory
        Path prompt1File = tempDir.resolve("prompt1.md");
        Path prompt2File = tempDir.resolve("prompt2.txt");
        Files.write(prompt1File, "# Markdown prompt".getBytes());
        Files.write(prompt2File, "Text prompt".getBytes());

        // Setup workflow data with non-PML files
        WorkflowData workflowData = new WorkflowData(
            new PromptInfo("prompt1.md", "markdown"),
            "test-model",
            "test-repo",
            List.of(new PromptInfo("prompt2.txt", "text plain")),
            null, null, null
        );
        when(workflowParser.parse(any(File.class))).thenReturn(workflowData);

        // When
        newJobRunCommand.run();

        // Then - PML validator should not be called for non-PML files
        verify(workflowValidator).validate(any(File.class));
        verify(workflowParser).parse(any(File.class));
        verify(pmlValidator, never()).validate(any(File.class));
        verify(jobRepository).save(any(Job.class));
        verify(jobRepository, times(2)).savePrompt(any(Prompt.class));
    }

    @Test
    void testRun_MixedPmlAndNonPmlFiles() throws Exception {
        // Given - Create mixed files in the temp directory
        Path pmlFile = tempDir.resolve("prompt1.xml");
        Path mdFile = tempDir.resolve("prompt2.md");
        Files.write(pmlFile, "<prompt><role>Test</role></prompt>".getBytes());
        Files.write(mdFile, "# Markdown prompt".getBytes());

        // Setup workflow data with mixed file types
        WorkflowData workflowData = new WorkflowData(
            new PromptInfo("prompt1.xml", "pml"),
            "test-model",
            "test-repo",
            List.of(new PromptInfo("prompt2.md", "markdown")),
            null, null, null
        );
        when(workflowParser.parse(any(File.class))).thenReturn(workflowData);

        // Mock PML validation to succeed for the PML file
        when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(true, List.of()));

        // When
        newJobRunCommand.run();

        // Then - Only the PML file should be validated
        verify(workflowValidator).validate(any(File.class));
        verify(workflowParser).parse(any(File.class));
        verify(pmlValidator, times(1)).validate(any(File.class)); // Only PML file validated
        verify(jobRepository).save(any(Job.class));
        verify(jobRepository, times(2)).savePrompt(any(Prompt.class));
    }

    @Test
    void testRun_PmlFileNotFound() throws Exception {
        // Given - Don't create the PML file
        // Setup workflow data with .xml file that doesn't exist
        WorkflowData workflowData = new WorkflowData(
            new PromptInfo("nonexistent.xml", "pml"),
            "test-model",
            "test-repo",
            List.of(),
            null, null, null
        );
        when(workflowParser.parse(any(File.class))).thenReturn(workflowData);

        // Mock PML validation to return file not found error
        when(pmlValidator.validate(any(File.class)))
            .thenReturn(new PmlValidator.ValidationResult(false,
                List.of("PML file does not exist: " + tempDir.resolve("nonexistent.xml").toAbsolutePath())));

        // When
        newJobRunCommand.run();

        // Then
        verify(workflowValidator).validate(any(File.class));
        verify(workflowParser).parse(any(File.class));
        verify(pmlValidator).validate(any(File.class));
        verify(jobRepository, never()).save(any(Job.class));
        verify(jobRepository, never()).savePrompt(any(Prompt.class));
    }
}
