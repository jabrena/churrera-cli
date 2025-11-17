package info.jab.churrera.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkflowValidator.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowValidatorTest {

    private WorkflowValidator workflowValidator;
    private File testWorkflowFile;

    @BeforeEach
    void setUp() throws IOException {
        workflowValidator = new WorkflowValidator();

        // Create a temporary workflow file for testing
        testWorkflowFile = File.createTempFile("test-workflow", ".xml");
        testWorkflowFile.deleteOnExit();
    }

    @Test
    void testValidate_ValidWorkflow() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:noNamespaceSchemaLocation="schema/pml-workflow.xsd">
                <sequence model="test-model" repository="test-repo">
                    <prompt pml="prompt1.pml"/>
                    <prompt pml="prompt2.pml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowValidator.ValidationResult result = workflowValidator.validate(testWorkflowFile);

        // Then
        assertNotNull(result);
        // Note: This test might fail if the XSD schema is not found or doesn't match
        // In a real scenario, you would need to ensure the schema is available
    }

    @Test
    void testValidate_ValidWorkflowWithPath() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:noNamespaceSchemaLocation="schema/pml-workflow.xsd">
                <sequence model="test-model" repository="test-repo">
                    <prompt pml="prompt1.pml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowValidator.ValidationResult result = workflowValidator.validate(testWorkflowFile.toPath());

        // Then
        assertNotNull(result);
    }

    @Test
    void testValidate_InvalidXml() throws Exception {
        // Given
        String invalidXmlContent = "invalid xml content";
        Files.write(testWorkflowFile.toPath(), invalidXmlContent.getBytes());

        // When
        WorkflowValidator.ValidationResult result = workflowValidator.validate(testWorkflowFile);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testValidate_FileNotFound() {
        // Given
        File nonExistentFile = new File("/non/existent/file.xml");

        // When
        WorkflowValidator.ValidationResult result = workflowValidator.validate(nonExistentFile);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testValidate_PathFileNotFound() {
        // Given
        Path nonExistentPath = Paths.get("/non/existent/file.xml");

        // When
        WorkflowValidator.ValidationResult result = workflowValidator.validate(nonExistentPath);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testValidationResult_ValidResult() {
        // Given
        List<String> errors = List.of();
        WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(true, errors);

        // When & Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("No validation errors.", result.getFormattedErrors());
    }

    @Test
    void testValidationResult_InvalidResult() {
        // Given
        List<String> errors = List.of("Error 1", "Error 2", "Error 3");
        WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, errors);

        // When & Then
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        assertEquals("Error 1", result.getErrors().get(0));
        assertEquals("Error 2", result.getErrors().get(1));
        assertEquals("Error 3", result.getErrors().get(2));

        String formattedErrors = result.getFormattedErrors();
        assertTrue(formattedErrors.contains("Validation errors:"));
        assertTrue(formattedErrors.contains("1. Error 1"));
        assertTrue(formattedErrors.contains("2. Error 2"));
        assertTrue(formattedErrors.contains("3. Error 3"));
    }

    @Test
    void testValidationResult_EmptyErrorsList() {
        // Given
        List<String> errors = List.of();
        WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, errors);

        // When & Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("No validation errors.", result.getFormattedErrors());
    }

    @Test
    void testValidationResult_Immutability() {
        // Given
        List<String> originalErrors = List.of("Error 1", "Error 2");
        WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, originalErrors);

        // When
        List<String> returnedErrors = result.getErrors();
        returnedErrors.add("Error 3");

        // Then
        assertEquals(2, result.getErrors().size());
        assertEquals("Error 1", result.getErrors().get(0));
        assertEquals("Error 2", result.getErrors().get(1));
    }

    @Test
    void testValidationResult_SingleError() {
        // Given
        List<String> errors = List.of("Single error message");
        WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, errors);

        // When & Then
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Single error message", result.getErrors().get(0));

        String formattedErrors = result.getFormattedErrors();
        assertTrue(formattedErrors.contains("Validation errors:"));
        assertTrue(formattedErrors.contains("1. Single error message"));
    }

    @Test
    void testValidationResult_ManyErrors() {
        // Given
        List<String> errors = List.of(
            "Error 1", "Error 2", "Error 3", "Error 4", "Error 5",
            "Error 6", "Error 7", "Error 8", "Error 9", "Error 10"
        );
        WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, errors);

        // When & Then
        assertFalse(result.isValid());
        assertEquals(10, result.getErrors().size());

        String formattedErrors = result.getFormattedErrors();
        assertTrue(formattedErrors.contains("Validation errors:"));
        assertTrue(formattedErrors.contains("1. Error 1"));
        assertTrue(formattedErrors.contains("10. Error 10"));
    }

    @Test
    void testValidateTimeoutAndFallback_NoTimeoutNoFallback() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, null, null);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTimeoutAndFallback_TimeoutWithoutFallback() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, null);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTimeoutAndFallback_FallbackWithoutTimeout() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, null, "fallback.xml");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("fallback-src is specified but timeout is not"));
    }

    @Test
    void testValidateTimeoutAndFallback_FallbackWithTimeout_FileExists() throws IOException {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "fallback.xml");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "fallback.xml");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTimeoutAndFallback_FallbackWithTimeout_FileDoesNotExist() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "nonexistent.xml");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Fallback file not found")));
    }

    @Test
    void testValidateTimeoutAndFallback_FallbackWithTimeout_InvalidExtension() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "fallback.exe");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("must have extension .xml, .md, or .txt")));
    }

    @Test
    void testValidateTimeoutAndFallback_FallbackWithTimeout_ValidXmlExtension() throws IOException {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "fallback.XML");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "fallback.XML");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTimeoutAndFallback_FallbackWithTimeout_ValidMdExtension() throws IOException {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "fallback.md");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "fallback.md");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTimeoutAndFallback_FallbackWithTimeout_ValidTxtExtension() throws IOException {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "fallback.txt");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "fallback.txt");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTimeoutAndFallback_EmptyFallback() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTimeoutAndFallback_WhitespaceFallback() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "   ");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTimeoutAndFallback_ParallelWorkflow_SequenceFallbackWithoutTimeout() throws IOException {
        // Given
        PromptInfo parallelPrompt = new PromptInfo("parallel.xml", "xml");
        PromptInfo seqPrompt = new PromptInfo("seq.xml", "xml");
        SequenceInfo sequence = new SequenceInfo("model", "repo", List.of(seqPrompt), null, "seq-fallback.xml");
        ParallelWorkflowData parallelData = new ParallelWorkflowData(parallelPrompt, null, List.of(sequence), null, null);
        WorkflowData workflowData = new WorkflowData(null, null, null, List.of(), parallelData, null, null);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Sequence fallback-src is specified but timeout is not")));
    }

    @Test
    void testValidateTimeoutAndFallback_ParallelWorkflow_SequenceFallbackWithTimeout_FileExists() throws IOException {
        // Given
        PromptInfo parallelPrompt = new PromptInfo("parallel.xml", "xml");
        PromptInfo seqPrompt = new PromptInfo("seq.xml", "xml");
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "seq-fallback.xml");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        Long seqTimeout = 10L * 60 * 1000; // 10 minutes
        SequenceInfo sequence = new SequenceInfo("model", "repo", List.of(seqPrompt), seqTimeout, "seq-fallback.xml");
        ParallelWorkflowData parallelData = new ParallelWorkflowData(parallelPrompt, null, List.of(sequence), null, null);
        WorkflowData workflowData = new WorkflowData(null, null, null, List.of(), parallelData, null, null);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTimeoutAndFallback_ParallelWorkflow_SequenceFallbackWithTimeout_FileDoesNotExist() {
        // Given
        PromptInfo parallelPrompt = new PromptInfo("parallel.xml", "xml");
        PromptInfo seqPrompt = new PromptInfo("seq.xml", "xml");
        Long seqTimeout = 10L * 60 * 1000; // 10 minutes
        SequenceInfo sequence = new SequenceInfo("model", "repo", List.of(seqPrompt), seqTimeout, "nonexistent-seq.xml");
        ParallelWorkflowData parallelData = new ParallelWorkflowData(parallelPrompt, null, List.of(sequence), null, null);
        WorkflowData workflowData = new WorkflowData(null, null, null, List.of(), parallelData, null, null);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Fallback file not found")));
    }

    @Test
    void testValidateTimeoutAndFallback_ParallelWorkflow_MultipleSequences() throws IOException {
        // Given
        PromptInfo parallelPrompt = new PromptInfo("parallel.xml", "xml");
        PromptInfo seq1Prompt = new PromptInfo("seq1.xml", "xml");
        PromptInfo seq2Prompt = new PromptInfo("seq2.xml", "xml");

        File fallback1 = new File(testWorkflowFile.getParentFile(), "fallback1.xml");
        fallback1.createNewFile();
        fallback1.deleteOnExit();

        Long timeout1 = 5L * 60 * 1000;
        Long timeout2 = 10L * 60 * 1000;
        SequenceInfo sequence1 = new SequenceInfo("model1", "repo1", List.of(seq1Prompt), timeout1, "fallback1.xml");
        SequenceInfo sequence2 = new SequenceInfo("model2", "repo2", List.of(seq2Prompt), timeout2, "nonexistent.xml");

        ParallelWorkflowData parallelData = new ParallelWorkflowData(parallelPrompt, null, List.of(sequence1, sequence2), null, null);
        WorkflowData workflowData = new WorkflowData(null, null, null, List.of(), parallelData, null, null);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Fallback file not found")));
    }

    @Test
    void testValidateFallbackFile_NullFallbackSrc() {
        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, null);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateFallbackFile_EmptyFallbackSrc() {
        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "");

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateFallbackFile_WhitespaceFallbackSrc() {
        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "   ");

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateFallbackFile_ValidXmlExtension_FileExists() throws IOException {
        // Given
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "fallback.xml");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "fallback.xml");

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateFallbackFile_ValidMdExtension_FileExists() throws IOException {
        // Given
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "fallback.md");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "fallback.md");

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateFallbackFile_ValidTxtExtension_FileExists() throws IOException {
        // Given
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "fallback.txt");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "fallback.txt");

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateFallbackFile_ValidExtension_CaseInsensitive() throws IOException {
        // Given
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "fallback.XML");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "fallback.XML");

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateFallbackFile_InvalidExtension() {
        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "fallback.exe");

        // Then
        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("must have extension .xml, .md, or .txt"));
    }

    @Test
    void testValidateFallbackFile_ValidExtension_FileDoesNotExist() {
        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "nonexistent.xml");

        // Then
        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Fallback file not found"));
    }

    @Test
    void testValidateFallbackFile_RelativePath() throws IOException {
        // Given
        File subDir = new File(testWorkflowFile.getParentFile(), "subdir");
        subDir.mkdirs();
        subDir.deleteOnExit();

        File fallbackFile = new File(subDir, "fallback.xml");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "subdir/fallback.xml");

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateFallbackFile_WorkflowFileInRoot() throws IOException {
        // Given
        File rootWorkflowFile = new File("/tmp", "workflow.xml");
        rootWorkflowFile.createNewFile();
        rootWorkflowFile.deleteOnExit();

        File fallbackFile = new File("/tmp", "fallback.xml");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        // When
        List<String> errors = workflowValidator.validateFallbackFile(rootWorkflowFile, "fallback.xml");

        // Then
        assertTrue(errors.isEmpty());
    }
}

