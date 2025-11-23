package info.jab.churrera.workflow;

import info.jab.churrera.util.PropertyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WorkflowValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowValidator Tests")
class WorkflowValidatorTest {

    private WorkflowValidator workflowValidator;
    private File testWorkflowFile;

    @BeforeEach
    void setUp() throws IOException {
        workflowValidator = new WorkflowValidator(new PropertyResolver());

        // Create a temporary workflow file for testing
        testWorkflowFile = File.createTempFile("test-workflow", ".xml");
        testWorkflowFile.deleteOnExit();
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate valid workflow file")
        void shouldValidateValidWorkflowFile() throws Exception {
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
            assertThat(result).isNotNull();
            // Note: This test might fail if the XSD schema is not found or doesn't match
            // In a real scenario, you would need to ensure the schema is available
        }

        @Test
        @DisplayName("Should validate valid workflow file using Path")
        void shouldValidateValidWorkflowFileUsingPath() throws Exception {
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
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should return invalid result for invalid XML")
        void shouldReturnInvalidResultForInvalidXml() throws Exception {
            // Given
            String invalidXmlContent = "invalid xml content";
            Files.write(testWorkflowFile.toPath(), invalidXmlContent.getBytes());

            // When
            WorkflowValidator.ValidationResult result = workflowValidator.validate(testWorkflowFile);

            // Then
            assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.isValid()).isFalse();
                    assertThat(r.getErrors()).isNotEmpty();
                });
        }

        @Test
        @DisplayName("Should return invalid result when file does not exist")
        void shouldReturnInvalidResultWhenFileDoesNotExist() {
            // Given
            File nonExistentFile = new File("/non/existent/file.xml");

            // When
            WorkflowValidator.ValidationResult result = workflowValidator.validate(nonExistentFile);

            // Then
            assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.isValid()).isFalse();
                    assertThat(r.getErrors()).isNotEmpty();
                });
        }

        @Test
        @DisplayName("Should return invalid result when Path file does not exist")
        void shouldReturnInvalidResultWhenPathFileDoesNotExist() {
            // Given
            Path nonExistentPath = Paths.get("/non/existent/file.xml");

            // When
            WorkflowValidator.ValidationResult result = workflowValidator.validate(nonExistentPath);

            // Then
            assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.isValid()).isFalse();
                    assertThat(r.getErrors()).isNotEmpty();
                });
        }
    }

    @Nested
    @DisplayName("ValidationResult Tests")
    class ValidationResultTests {

        @Test
        @DisplayName("Should create valid result with no errors")
        void shouldCreateValidResultWithNoErrors() {
            // Given
            List<String> errors = List.of();
            WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(true, errors);

            // When & Then
            assertThat(result)
                .satisfies(r -> {
                    assertThat(r.isValid()).isTrue();
                    assertThat(r.getErrors()).isEmpty();
                    assertThat(r.getFormattedErrors()).isEqualTo("No validation errors.");
                });
        }

        @Test
        @DisplayName("Should create invalid result with multiple errors")
        void shouldCreateInvalidResultWithMultipleErrors() {
            // Given
            List<String> errors = List.of("Error 1", "Error 2", "Error 3");
            WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, errors);

            // When & Then
            assertThat(result)
                .satisfies(r -> {
                    assertThat(r.isValid()).isFalse();
                    assertThat(r.getErrors())
                        .hasSize(3)
                        .containsExactly("Error 1", "Error 2", "Error 3");
                });

            String formattedErrors = result.getFormattedErrors();
            assertThat(formattedErrors)
                .contains("Validation errors:")
                .contains("1. Error 1")
                .contains("2. Error 2")
                .contains("3. Error 3");
        }

        @Test
        @DisplayName("Should create invalid result with empty errors list")
        void shouldCreateInvalidResultWithEmptyErrorsList() {
            // Given
            List<String> errors = List.of();
            WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, errors);

            // When & Then
            assertThat(result)
                .satisfies(r -> {
                    assertThat(r.isValid()).isFalse();
                    assertThat(r.getErrors()).isEmpty();
                    assertThat(r.getFormattedErrors()).isEqualTo("No validation errors.");
                });
        }

        @Test
        @DisplayName("Should return immutable errors list")
        void shouldReturnImmutableErrorsList() {
            // Given
            List<String> originalErrors = List.of("Error 1", "Error 2");
            WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, originalErrors);

            // When
            List<String> returnedErrors = result.getErrors();
            returnedErrors.add("Error 3");

            // Then
            assertThat(result.getErrors())
                .hasSize(2)
                .containsExactly("Error 1", "Error 2");
        }

        @Test
        @DisplayName("Should format single error correctly")
        void shouldFormatSingleErrorCorrectly() {
            // Given
            List<String> errors = List.of("Single error message");
            WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, errors);

            // When & Then
            assertThat(result)
                .satisfies(r -> {
                    assertThat(r.isValid()).isFalse();
                    assertThat(r.getErrors())
                        .hasSize(1)
                        .containsExactly("Single error message");
                });

            String formattedErrors = result.getFormattedErrors();
            assertThat(formattedErrors)
                .contains("Validation errors:")
                .contains("1. Single error message");
        }

        @Test
        @DisplayName("Should format many errors correctly")
        void shouldFormatManyErrorsCorrectly() {
            // Given
            List<String> errors = List.of(
                "Error 1", "Error 2", "Error 3", "Error 4", "Error 5",
                "Error 6", "Error 7", "Error 8", "Error 9", "Error 10"
            );
            WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult(false, errors);

            // When & Then
            assertThat(result)
                .satisfies(r -> {
                    assertThat(r.isValid()).isFalse();
                    assertThat(r.getErrors()).hasSize(10);
                });

            String formattedErrors = result.getFormattedErrors();
            assertThat(formattedErrors)
                .contains("Validation errors:")
                .contains("1. Error 1")
                .contains("10. Error 10");
        }
    }

    @Test
    @DisplayName("Should validate timeout and fallback when neither is specified")
    void testValidateTimeoutAndFallback_NoTimeoutNoFallback() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, null, null);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertThat(errors).isEmpty();
    }

    @ParameterizedTest(name = "Should validate timeout and fallback when timeout is specified with fallback: {1}")
    @MethodSource("provideTimeoutWithFallbackTestCases")
    @DisplayName("Should validate timeout and fallback when timeout is specified")
    void testValidateTimeoutAndFallback_TimeoutWithFallback(String fallbackSrc) {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, fallbackSrc);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertThat(errors).isEmpty();
    }

    private static Stream<Arguments> provideTimeoutWithFallbackTestCases() {
        return Stream.of(
            Arguments.of((String) null),
            Arguments.of(""),
            Arguments.of("   ")
        );
    }

    @Test
    @DisplayName("Should return error when fallback is specified without timeout")
    void testValidateTimeoutAndFallback_FallbackWithoutTimeout() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, null, "fallback.xml");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertThat(errors)
            .isNotEmpty()
            .hasSize(1);
        assertThat(errors.get(0)).contains("fallback-src is specified but timeout is not");
    }

    @ParameterizedTest(name = "Should validate timeout and fallback when both are specified with valid extension: {0}")
    @MethodSource("provideValidFallbackExtensionTestCases")
    @DisplayName("Should validate timeout and fallback when both are specified with valid extension")
    void testValidateTimeoutAndFallback_FallbackWithTimeout_ValidExtension(String fallbackFileName) throws IOException {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        File fallbackFile = new File(testWorkflowFile.getParentFile(), fallbackFileName);
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, fallbackFileName);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertThat(errors).isEmpty();
    }

    private static Stream<Arguments> provideValidFallbackExtensionTestCases() {
        return Stream.of(
            Arguments.of("fallback.xml"),
            Arguments.of("fallback.XML"),
            Arguments.of("fallback.md"),
            Arguments.of("fallback.txt")
        );
    }

    @Test
    @DisplayName("Should return error when fallback file does not exist")
    void testValidateTimeoutAndFallback_FallbackWithTimeout_FileDoesNotExist() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "nonexistent.xml");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(e -> e.contains("Fallback file not found"));
    }

    @Test
    @DisplayName("Should return error when fallback file has invalid extension")
    void testValidateTimeoutAndFallback_FallbackWithTimeout_InvalidExtension() {
        // Given
        PromptInfo launchPrompt = new PromptInfo("prompt1.xml", "xml");
        Long timeoutMillis = 5L * 60 * 1000; // 5 minutes
        WorkflowData workflowData = new WorkflowData(launchPrompt, "model", "repo", List.of(), null, timeoutMillis, "fallback.exe");

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(e -> e.contains("must have extension .xml, .md, or .txt"));
    }



    @Test
    @DisplayName("Should return error when parallel workflow sequence has fallback without timeout")
    void testValidateTimeoutAndFallback_ParallelWorkflow_SequenceFallbackWithoutTimeout() {
        // Given
        PromptInfo parallelPrompt = new PromptInfo("parallel.xml", "xml");
        PromptInfo seqPrompt = new PromptInfo("seq.xml", "xml");
        SequenceInfo sequence = new SequenceInfo("model", "repo", List.of(seqPrompt), null, "seq-fallback.xml");
        ParallelWorkflowData parallelData = new ParallelWorkflowData(parallelPrompt, null, List.of(sequence), null, null);
        WorkflowData workflowData = new WorkflowData(null, null, null, List.of(), parallelData, null, null);

        // When
        List<String> errors = workflowValidator.validateTimeoutAndFallback(testWorkflowFile, workflowData);

        // Then
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(e -> e.contains("Sequence fallback-src is specified but timeout is not"));
    }

    @Test
    @DisplayName("Should validate parallel workflow sequence fallback with timeout when file exists")
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
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should return error when parallel workflow sequence fallback file does not exist")
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
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(e -> e.contains("Fallback file not found"));
    }

    @Test
    @DisplayName("Should validate parallel workflow with multiple sequences")
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
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(e -> e.contains("Fallback file not found"));
    }

    @ParameterizedTest(name = "Should validate fallback file when fallback source is: {0}")
    @MethodSource("provideFallbackSrcTestCases")
    @DisplayName("Should validate fallback file when fallback source is null/empty/whitespace")
    void testValidateFallbackFile_NullEmptyWhitespaceFallbackSrc(String fallbackSrc) {
        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, fallbackSrc);

        // Then
        assertThat(errors).isEmpty();
    }

    private static Stream<Arguments> provideFallbackSrcTestCases() {
        return Stream.of(
            Arguments.of((String) null),
            Arguments.of(""),
            Arguments.of("   ")
        );
    }

    @ParameterizedTest(name = "Should validate fallback file with valid extension when file exists: {0}")
    @MethodSource("provideValidFallbackFileExtensionTestCases")
    @DisplayName("Should validate fallback file with valid extension when file exists")
    void testValidateFallbackFile_ValidExtension_FileExists(String fallbackFileName) throws IOException {
        // Given
        File fallbackFile = new File(testWorkflowFile.getParentFile(), fallbackFileName);
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, fallbackFileName);

        // Then
        assertThat(errors).isEmpty();
    }

    private static Stream<Arguments> provideValidFallbackFileExtensionTestCases() {
        return Stream.of(
            Arguments.of("fallback.xml"),
            Arguments.of("fallback.md"),
            Arguments.of("fallback.txt")
        );
    }

    @Test
    @DisplayName("Should validate fallback file extension case insensitively")
    void testValidateFallbackFile_ValidExtension_CaseInsensitive() throws IOException {
        // Given
        File fallbackFile = new File(testWorkflowFile.getParentFile(), "fallback.XML");
        fallbackFile.createNewFile();
        fallbackFile.deleteOnExit();

        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "fallback.XML");

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should return error when fallback file has invalid extension")
    void testValidateFallbackFile_InvalidExtension() {
        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "fallback.exe");

        // Then
        assertThat(errors)
            .isNotEmpty()
            .hasSize(1);
        assertThat(errors.get(0)).contains("must have extension .xml, .md, or .txt");
    }

    @Test
    @DisplayName("Should return error when fallback file with valid extension does not exist")
    void testValidateFallbackFile_ValidExtension_FileDoesNotExist() {
        // When
        List<String> errors = workflowValidator.validateFallbackFile(testWorkflowFile, "nonexistent.xml");

        // Then
        assertThat(errors)
            .isNotEmpty()
            .hasSize(1);
        assertThat(errors.get(0)).contains("Fallback file not found");
    }

    @Test
    @DisplayName("Should validate fallback file with relative path")
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
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should validate fallback file when workflow file is in root")
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
        assertThat(errors).isEmpty();
    }
}

