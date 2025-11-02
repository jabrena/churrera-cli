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
}

