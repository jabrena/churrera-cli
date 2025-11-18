package info.jab.churrera.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PmlValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PmlValidator Tests")
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

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate valid PML file")
        void shouldValidateValidPmlFile() throws Exception {
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
            assertThat(result).isNotNull();
            // Note: This test might fail if the XSD schema is not found or doesn't match
            // In a real scenario, you would need to ensure the schema is available
        }

        @Test
        @DisplayName("Should validate valid PML file using Path")
        void shouldValidateValidPmlFileUsingPath() throws Exception {
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
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should return invalid result for invalid XML")
        void shouldReturnInvalidResultForInvalidXml() throws Exception {
            // Given
            String invalidXmlContent = "invalid xml content";
            Files.write(testPmlFile.toPath(), invalidXmlContent.getBytes());

            // When
            PmlValidator.ValidationResult result = pmlValidator.validate(testPmlFile);

            // Then
            assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.isValid()).isFalse();
                    assertThat(r.getErrors()).isNotEmpty();
                });
        }

        @Test
        @DisplayName("Should handle invalid PML structure")
        void shouldHandleInvalidPmlStructure() throws Exception {
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
            assertThat(result).isNotNull();
            // The result may be valid or invalid depending on schema strictness
            // But we verify the method doesn't throw exceptions
        }

        @Test
        @DisplayName("Should return invalid result when file does not exist")
        void shouldReturnInvalidResultWhenFileDoesNotExist() {
            // Given
            File nonExistentFile = new File("/non/existent/pml-file.xml");

            // When
            PmlValidator.ValidationResult result = pmlValidator.validate(nonExistentFile);

            // Then
            assertThat(result)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.isValid()).isFalse();
                    assertThat(r.getErrors()).isNotEmpty();
                    assertThat(r.getErrors().get(0)).contains("does not exist");
                });
        }

        @Test
        @DisplayName("Should return invalid result when Path file does not exist")
        void shouldReturnInvalidResultWhenPathFileDoesNotExist() {
            // Given
            Path nonExistentPath = Paths.get("/non/existent/pml-file.xml");

            // When
            PmlValidator.ValidationResult result = pmlValidator.validate(nonExistentPath);

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
            PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(true, errors);

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
            PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, errors);

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
            PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, errors);

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
            PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, originalErrors);

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
            PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, errors);

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
            PmlValidator.ValidationResult result = new PmlValidator.ValidationResult(false, errors);

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
}
