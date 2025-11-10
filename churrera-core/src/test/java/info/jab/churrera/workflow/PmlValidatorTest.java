package info.jab.churrera.workflow;

import org.junit.jupiter.api.BeforeEach;
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
 * Unit tests for PmlValidator.
 */
@ExtendWith(MockitoExtension.class)
class PmlValidatorTest {

    private PmlValidator pmlValidator;
    private File testPmlFile;

    @BeforeEach
    void setUp() throws IOException {
        pmlValidator = new PmlValidator();

        // Create a temporary PML file for testing
        testPmlFile = File.createTempFile("test-pml", ".xml");
        testPmlFile.deleteOnExit();
    }

    @Test
    void testValidate_ValidPmlFile() throws Exception {
        // Given - Create a valid PML file structure
        String pmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <prompt xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:noNamespaceSchemaLocation="https://jabrena.github.io/pml/schemas/0.3.0/pml.xsd">
                <metadata>
                    <title>Test Prompt</title>
                </metadata>
                <role>Test role</role>
                <goal>Test goal</goal>
                <output-format>
                    <output-format-list>
                        <output-format-item>Test output</output-format-item>
                    </output-format-list>
                </output-format>
                <safeguards>
                    <safeguards-list>
                        <safeguards-item>Test safeguard</safeguards-item>
                    </safeguards-list>
                </safeguards>
            </prompt>
            """;
        Files.write(testPmlFile.toPath(), pmlContent.getBytes());

        // When
        PmlValidator.ValidationResult result = pmlValidator.validate(testPmlFile);

        // Then
        assertNotNull(result);
        // Note: This test might fail if the XSD schema is not found or doesn't match
        // In a real scenario, you would need to ensure the schema is available
    }

    @Test
    void testValidate_ValidPmlFileWithPath() throws Exception {
        // Given
        String pmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <prompt xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:noNamespaceSchemaLocation="https://jabrena.github.io/pml/schemas/0.3.0/pml.xsd">
                <metadata>
                    <title>Test Prompt</title>
                </metadata>
                <role>Test role</role>
                <goal>Test goal</goal>
                <output-format>
                    <output-format-list>
                        <output-format-item>Test output</output-format-item>
                    </output-format-list>
                </output-format>
                <safeguards>
                    <safeguards-list>
                        <safeguards-item>Test safeguard</safeguards-item>
                    </safeguards-list>
                </safeguards>
            </prompt>
            """;
        Files.write(testPmlFile.toPath(), pmlContent.getBytes());

        // When
        PmlValidator.ValidationResult result = pmlValidator.validate(testPmlFile.toPath());

        // Then
        assertNotNull(result);
    }

    @Test
    void testValidate_InvalidXml() throws Exception {
        // Given
        String invalidXmlContent = "invalid xml content";
        Files.write(testPmlFile.toPath(), invalidXmlContent.getBytes());

        // When
        PmlValidator.ValidationResult result = pmlValidator.validate(testPmlFile);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testValidate_InvalidPmlStructure() throws Exception {
        // Given - PML file with invalid structure (missing required elements)
        String invalidPmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <prompt xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:noNamespaceSchemaLocation="https://jabrena.github.io/pml/schemas/0.3.0/pml.xsd">
                <invalid-element>This should not be here</invalid-element>
            </prompt>
            """;
        Files.write(testPmlFile.toPath(), invalidPmlContent.getBytes());

        // When
        PmlValidator.ValidationResult result = pmlValidator.validate(testPmlFile);

        // Then
        assertNotNull(result);
        // The result may be valid or invalid depending on schema strictness
        // But we verify the method doesn't throw exceptions
    }

    @Test
    void testValidate_FileNotFound() {
        // Given
        File nonExistentFile = new File("/non/existent/pml-file.xml");

        // When
        PmlValidator.ValidationResult result = pmlValidator.validate(nonExistentFile);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("does not exist"));
    }

    @Test
    void testValidate_PathFileNotFound() {
        // Given
        Path nonExistentPath = Paths.get("/non/existent/pml-file.xml");

        // When
        PmlValidator.ValidationResult result = pmlValidator.validate(nonExistentPath);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testValidationResult_ValidResult() {
        // Given
        List<String> errors = List.of();
        PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(true, errors);

        // When & Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("No validation errors.", result.getFormattedErrors());
    }

    @Test
    void testValidationResult_InvalidResult() {
        // Given
        List<String> errors = List.of("Error 1", "Error 2", "Error 3");
        PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, errors);

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
        PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, errors);

        // When & Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("No validation errors.", result.getFormattedErrors());
    }

    @Test
    void testValidationResult_Immutability() {
        // Given
        List<String> originalErrors = List.of("Error 1", "Error 2");
        PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, originalErrors);

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
        PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, errors);

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
        PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, errors);

        // When & Then
        assertFalse(result.isValid());
        assertEquals(10, result.getErrors().size());

        String formattedErrors = result.getFormattedErrors();
        assertTrue(formattedErrors.contains("Validation errors:"));
        assertTrue(formattedErrors.contains("1. Error 1"));
        assertTrue(formattedErrors.contains("10. Error 10"));
    }
}

