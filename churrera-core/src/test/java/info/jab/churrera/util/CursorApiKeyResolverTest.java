package info.jab.churrera.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for CursorApiKeyResolver utility.
 */
@DisplayName("CursorApiKeyResolver Tests")
class CursorApiKeyResolverTest {

    private String originalApiKey;
    private String originalWorkingDir;

    @BeforeEach
    void setUp() {
        // Save original environment variable
        originalApiKey = System.getenv(CursorApiKeyResolver.CURSOR_API_KEY);

        // Save original working directory
        originalWorkingDir = System.getProperty("user.dir");

        // Clear environment variable for clean tests
        clearEnvironmentVariable();
    }

    @AfterEach
    void tearDown() {
        // Restore original environment variable
        if (originalApiKey != null) {
            setEnvironmentVariable(originalApiKey);
        } else {
            clearEnvironmentVariable();
        }

        // Restore original working directory
        System.setProperty("user.dir", originalWorkingDir);
    }

    @Test
    @DisplayName("Should throw exception when API key is not found")
    void shouldThrowExceptionWhenApiKeyIsNotFound() {
        // Note: This test may not work in all environments due to environment variable limitations
        // In a real CI/CD environment, you would set the environment variable externally
        // For now, we'll test the error case when no API key is found
        clearEnvironmentVariable();

        // Test that exception is thrown when no API key is found
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();

        // When & Then
        assertThatThrownBy(resolver::resolveApiKey)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API key not found");
    }

    @ParameterizedTest(name = "Should resolve API key from .env file: {0}")
    @MethodSource("provideApiKeyTestCases")
    @DisplayName("Should resolve API key from .env file")
    void shouldResolveApiKeyFromEnvFile(String envFileContent, String expectedApiKey) throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Create .env file with test content
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write(envFileContent);
            }

            // Test resolution
            CursorApiKeyResolver resolver = new CursorApiKeyResolver();

            // When
            String result = resolver.resolveApiKey();

            // Then
            assertThat(result).isEqualTo(expectedApiKey);
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    private static Stream<Arguments> provideApiKeyTestCases() {
        return Stream.of(
            Arguments.of("CURSOR_API_KEY=env-file-key-789", "env-file-key-789"),
            Arguments.of("CURSOR_API_KEY=  env-file-key-whitespace  ", "env-file-key-whitespace"),
            Arguments.of("CURSOR_API_KEY=env-file-key", "env-file-key")
        );
    }

    @Test
    @DisplayName("Should throw exception when API key is not found")
    void shouldThrowExceptionWhenApiKeyIsNotFoundInAnySource() {
        // Ensure no API key is set
        clearEnvironmentVariable();

        // Test that exception is thrown
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();

        // When & Then
        assertThatThrownBy(resolver::resolveApiKey)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API key not found")
            .hasMessageContaining(".env file")
            .hasMessageContaining("Environment variable");
    }


    @ParameterizedTest(name = "Should throw exception when API key is invalid: {0}")
    @MethodSource("provideInvalidApiKeyTestCases")
    @DisplayName("Should throw exception when API key is invalid")
    void shouldThrowExceptionWhenApiKeyIsInvalid(String envFileContent, boolean createFile) throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        // Ensure file doesn't exist initially
        if (envFile.exists()) {
            envFile.delete();
        }

        try {
            if (createFile) {
                // Create .env file with invalid content
                try (FileWriter writer = new FileWriter(envFile)) {
                    writer.write(envFileContent);
                }
            }

            // Test that exception is thrown
            CursorApiKeyResolver resolver = new CursorApiKeyResolver();

            // When & Then
            assertThatThrownBy(resolver::resolveApiKey)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key not found");
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    private static Stream<Arguments> provideInvalidApiKeyTestCases() {
        return Stream.of(
            Arguments.of("CURSOR_API_KEY=", true),
            Arguments.of("CURSOR_API_KEY=   ", true),
            Arguments.of("invalid-env-file-content", true),
            Arguments.of("OTHER_KEY=some-value", true)
        );
    }

    @Test
    @DisplayName("Should have correct CURSOR_API_KEY constant")
    void shouldHaveCorrectCursorApiKeyConstant() {
        // Then
        assertThat(CursorApiKeyResolver.CURSOR_API_KEY).isEqualTo("CURSOR_API_KEY");
    }

    @Test
    @DisplayName("Should be able to instantiate CursorApiKeyResolver")
    void shouldBeAbleToInstantiateCursorApiKeyResolver() {
        // When
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();

        // Then
        assertThat(resolver).isNotNull();
    }

    @Test
    @DisplayName("Should resolve API key with special characters from .env file")
    void shouldResolveApiKeyWithSpecialCharacters() throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Test with special characters in API key via .env file
            // Note: Using simpler special characters that work well with .env format
            String testApiKey = "test-key-with-special-chars-123-abc";
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("CURSOR_API_KEY=" + testApiKey);
            }

            CursorApiKeyResolver resolver = new CursorApiKeyResolver();

            // When
            String result = resolver.resolveApiKey();

            // Then
            assertThat(result).isEqualTo(testApiKey);
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    @DisplayName("Should resolve very long API key from .env file")
    void shouldResolveVeryLongApiKey() throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Test with very long API key via .env file
            String testApiKey = "a".repeat(1000);
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("CURSOR_API_KEY=" + testApiKey);
            }

            CursorApiKeyResolver resolver = new CursorApiKeyResolver();

            // When
            String result = resolver.resolveApiKey();

            // Then
            assertThat(result).isEqualTo(testApiKey);
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    @DisplayName("Should resolve API key with Unicode characters from .env file")
    void shouldResolveApiKeyWithUnicodeCharacters() throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Test with Unicode characters via .env file
            String testApiKey = "test-key-with-unicode-🚀-测试-αβγ";
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("CURSOR_API_KEY=" + testApiKey);
            }

            CursorApiKeyResolver resolver = new CursorApiKeyResolver();

            // When
            String result = resolver.resolveApiKey();

            // Then
            assertThat(result).isEqualTo(testApiKey);
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    // Helper methods

    private void setEnvironmentVariable(String value) {
        // Note: This is a simplified approach for testing
        // In a real test environment, you might need to use a test framework
        // that supports environment variable manipulation
        System.setProperty(CursorApiKeyResolver.CURSOR_API_KEY, value);
    }

    private void clearEnvironmentVariable() {
        // Note: This is a simplified approach for testing
        System.clearProperty(CursorApiKeyResolver.CURSOR_API_KEY);
    }

}
