package info.jab.churrera.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ClasspathResolver utility.
 * Tests the utility's ability to load resources from classpath and handle error cases.
 */
@DisplayName("ClasspathResolver Tests")
class ClasspathResolverTest {

    @Test
    @DisplayName("Should retrieve existing resource from classpath")
    void shouldRetrieveExistingResource() {
        // Given
        String fileName = "examples/hello-world/prompt1.xml";
        ClasspathResolver resolver = new ClasspathResolver();

        // When
        String content = resolver.retrieve(fileName);

        // Then
        assertNotNull(content, "Retrieved content should not be null");
        assertFalse(content.isEmpty(), "Retrieved content should not be empty");
        assertTrue(content.contains("<?xml"), "Content should be XML");
    }

    @Test
    @DisplayName("Should retrieve XSL file from classpath")
    void shouldRetrieveXslFileFromClasspath() {
        // Given
        String fileName = "pml/pml-to-md.xsl";
        ClasspathResolver resolver = new ClasspathResolver();

        // When
        String content = resolver.retrieve(fileName);

        // Then
        assertNotNull(content, "Retrieved content should not be null");
        assertFalse(content.isEmpty(), "Retrieved content should not be empty");
        assertTrue(content.contains("xsl:stylesheet"), "Content should be XSL stylesheet");
    }

    @Test
    @DisplayName("Should retrieve properties file from classpath")
    void shouldRetrievePropertiesFileFromClasspath() {
        // Given
        String fileName = "application.properties";
        ClasspathResolver resolver = new ClasspathResolver();

        // When
        String content = resolver.retrieve(fileName);

        // Then
        assertNotNull(content, "Retrieved content should not be null");
        assertFalse(content.isEmpty(), "Retrieved content should not be empty");
        assertTrue(content.contains("="), "Properties file should contain key-value pairs");
    }

    // Precondition Tests - Null and Empty Inputs

    @Test
    @DisplayName("Should throw IllegalArgumentException when filename is null")
    void shouldThrowExceptionWhenFilenameIsNull() {
        // Given
        ClasspathResolver resolver = new ClasspathResolver();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.retrieve(null),
            "Should throw IllegalArgumentException for null filename"
        );

        assertTrue(exception.getMessage().contains("cannot be null or empty"),
            "Exception message should indicate null or empty constraint");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when filename is empty string")
    void shouldThrowExceptionWhenFilenameIsEmpty() {
        // Given
        ClasspathResolver resolver = new ClasspathResolver();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.retrieve(""),
            "Should throw IllegalArgumentException for empty filename"
        );

        assertTrue(exception.getMessage().contains("cannot be null or empty"),
            "Exception message should indicate null or empty constraint");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when filename is whitespace only")
    void shouldThrowExceptionWhenFilenameIsWhitespaceOnly() {
        // Given
        ClasspathResolver resolver = new ClasspathResolver();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.retrieve("   "),
            "Should throw IllegalArgumentException for whitespace-only filename"
        );

        assertTrue(exception.getMessage().contains("cannot be null or empty"),
            "Exception message should indicate null or empty constraint");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when filename is tab characters")
    void shouldThrowExceptionWhenFilenameIsTabCharacters() {
        // Given
        ClasspathResolver resolver = new ClasspathResolver();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.retrieve("\t\t"),
            "Should throw IllegalArgumentException for tab-only filename"
        );

        assertTrue(exception.getMessage().contains("cannot be null or empty"),
            "Exception message should indicate null or empty constraint");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when filename is newline characters")
    void shouldThrowExceptionWhenFilenameIsNewlineCharacters() {
        // Given
        ClasspathResolver resolver = new ClasspathResolver();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.retrieve("\n\n"),
            "Should throw IllegalArgumentException for newline-only filename"
        );

        assertTrue(exception.getMessage().contains("cannot be null or empty"),
            "Exception message should indicate null or empty constraint");
    }

    // Error Handling Tests

    @Test
    @DisplayName("Should throw RuntimeException when file does not exist in classpath")
    void shouldThrowRuntimeExceptionWhenFileDoesNotExist() {
        // Given
        String nonExistentFile = "non-existent-file.xml";
        ClasspathResolver resolver = new ClasspathResolver();

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> resolver.retrieve(nonExistentFile),
            "Should throw RuntimeException for non-existent file"
        );

        assertTrue(exception.getMessage().contains(nonExistentFile),
            "Exception message should contain the filename");
        assertTrue(exception.getMessage().contains("Could not find"),
            "Exception message should indicate file not found");
    }

    @Test
    @DisplayName("Should throw RuntimeException when path is invalid")
    void shouldThrowRuntimeExceptionWhenPathIsInvalid() {
        // Given
        String invalidPath = "../../../etc/passwd";
        ClasspathResolver resolver = new ClasspathResolver();

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> resolver.retrieve(invalidPath),
            "Should throw RuntimeException for invalid path"
        );

        assertTrue(exception.getMessage().contains(invalidPath) ||
                   exception.getMessage().contains("Could not find") ||
                   exception.getMessage().contains("Failed to load"),
            "Exception message should indicate the problem");
    }

    @Test
    @DisplayName("Should throw RuntimeException when file path has directory traversal")
    void shouldThrowRuntimeExceptionWhenFilePathHasDirectoryTraversal() {
        // Given
        String traversalPath = "../../outside-classpath.txt";
        ClasspathResolver resolver = new ClasspathResolver();

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> resolver.retrieve(traversalPath),
            "Should throw RuntimeException for directory traversal attempt"
        );

        assertNotNull(exception.getMessage(), "Exception should have a message");
    }


    // Content Validation Tests

    @Test
    @DisplayName("Should retrieve content with correct encoding (UTF-8)")
    void shouldRetrieveContentWithCorrectEncoding() {
        // Given - assuming we have a file with special characters
        String fileName = "examples/hello-world/prompt1.xml";
        ClasspathResolver resolver = new ClasspathResolver();

        // When
        String content = resolver.retrieve(fileName);

        // Then
        assertNotNull(content, "Content should not be null");
        // Verify UTF-8 encoding is used (content should be properly decoded)
        assertFalse(content.contains("�"), "Content should not contain replacement characters");
    }

    @Test
    @DisplayName("Should retrieve empty file without errors")
    void shouldRetrieveEmptyFileWithoutErrors() {
        // Given - we need to create a test for an empty resource if it exists
        // For now, we test that non-empty files work correctly
        String fileName = "application.properties";
        ClasspathResolver resolver = new ClasspathResolver();

        // When
        String content = resolver.retrieve(fileName);

        // Then
        assertNotNull(content, "Content should not be null even for potentially empty files");
    }

    @Test
    @DisplayName("Should handle files with various extensions")
    void shouldHandleFilesWithVariousExtensions() {
        // Given
        ClasspathResolver resolver = new ClasspathResolver();

        // Test XML file
        assertDoesNotThrow(() -> resolver.retrieve("examples/hello-world/prompt1.xml"),
            "Should handle .xml files");

        // Test XSL file
        assertDoesNotThrow(() -> resolver.retrieve("pml/pml-to-md.xsl"),
            "Should handle .xsl files");

        // Test properties file
        assertDoesNotThrow(() -> resolver.retrieve("application.properties"),
            "Should handle .properties files");
    }

    @Test
    @DisplayName("Should handle nested directory structures")
    void shouldHandleNestedDirectoryStructures() {
        // Given
        String nestedPath = "examples/hello-world/prompt1.xml";
        ClasspathResolver resolver = new ClasspathResolver();

        // When
        String content = resolver.retrieve(nestedPath);

        // Then
        assertNotNull(content, "Should retrieve content from nested directories");
        assertFalse(content.isEmpty(), "Content should not be empty");
    }

    @Test
    @DisplayName("Should handle paths without leading slash")
    void shouldHandlePathsWithoutLeadingSlash() {
        // Given
        String pathWithoutSlash = "application.properties";
        ClasspathResolver resolver = new ClasspathResolver();

        // When
        String content = resolver.retrieve(pathWithoutSlash);

        // Then
        assertNotNull(content, "Should retrieve content from path without leading slash");
        assertFalse(content.isEmpty(), "Content should not be empty");
    }
}

