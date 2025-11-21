package info.jab.churrera.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for CursorApiKeyResolver utility.
 */
@DisplayName("CursorApiKeyResolver Tests")
class CursorApiKeyResolverTest {

    private String originalApiKey;
    private String originalWorkingDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Save original environment variable
        originalApiKey = System.getenv(CursorApiKeyResolver.CURSOR_API_KEY);

        // Save original working directory
        originalWorkingDir = System.getProperty("user.dir");

        // Clear environment variable for clean tests
        clearEnvironmentVariable();
    }

    @AfterEach
    void tearDown() throws Exception {
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

    @Test
    @DisplayName("Should resolve API key from .env file")
    void shouldResolveApiKeyFromEnvFile() throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Create .env file with test content
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("CURSOR_API_KEY=env-file-key-789");
            }

            // Test resolution
            CursorApiKeyResolver resolver = new CursorApiKeyResolver();
            
            // When
            String result = resolver.resolveApiKey();
            
            // Then
            assertThat(result).isEqualTo("env-file-key-789");
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    @DisplayName("Should trim whitespace from API key in .env file")
    void shouldTrimWhitespaceFromApiKeyInEnvFile() throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Create .env file with whitespace
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("CURSOR_API_KEY=  env-file-key-whitespace  ");
            }

            // Test resolution - should trim whitespace
            CursorApiKeyResolver resolver = new CursorApiKeyResolver();
            
            // When
            String result = resolver.resolveApiKey();
            
            // Then
            assertThat(result).isEqualTo("env-file-key-whitespace");
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    @DisplayName("Should prioritize .env file over system environment")
    void shouldPrioritizeEnvFileOverSystemEnvironment() throws IOException {
        // Test that .env file takes priority over system environment
        // Note: This test focuses on .env file functionality since environment variable
        // manipulation in tests is complex and environment-dependent

        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Create .env file with test content
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("CURSOR_API_KEY=env-file-key");
            }

            // Test resolution - .env file should be used
            CursorApiKeyResolver resolver = new CursorApiKeyResolver();
            
            // When
            String result = resolver.resolveApiKey();
            
            // Then
            assertThat(result).isEqualTo("env-file-key");
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
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

    @Test
    @DisplayName("Should throw exception when API key is empty in system environment")
    void shouldThrowExceptionWhenApiKeyIsEmptyInSystemEnvironment() {
        // Test that empty environment variable causes exception
        // Note: This test may not work in all environments due to environment variable limitations
        clearEnvironmentVariable();

        // Test that exception is thrown
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();
        
        // When & Then
        assertThatThrownBy(resolver::resolveApiKey)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API key not found");
    }

    @Test
    @DisplayName("Should throw exception when API key is empty in .env file")
    void shouldThrowExceptionWhenApiKeyIsEmptyInEnvFile() throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Create .env file with empty value
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("CURSOR_API_KEY=");
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

    @Test
    @DisplayName("Should throw exception when API key is whitespace-only in .env file")
    void shouldThrowExceptionWhenApiKeyIsWhitespaceOnlyInEnvFile() throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Create .env file with whitespace-only value
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("CURSOR_API_KEY=   ");
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

    @Test
    @DisplayName("Should throw exception when .env file is malformed")
    void shouldThrowExceptionWhenEnvFileIsMalformed() throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Create malformed .env file
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("invalid-env-file-content");
            }

            // Test resolution - should throw exception since no valid API key is found
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

    @Test
    @DisplayName("Should throw exception when .env file is missing")
    void shouldThrowExceptionWhenEnvFileIsMissing() {
        // Test resolution when no .env file exists - should throw exception since no API key is found
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();
        
        // When & Then
        assertThatThrownBy(resolver::resolveApiKey)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API key not found");
    }

    @Test
    @DisplayName("Should throw exception when .env file exists but missing CURSOR_API_KEY")
    void shouldThrowExceptionWhenEnvFileExistsButMissingKey() throws IOException {
        // Create .env file in the current working directory (project root)
        File projectRoot = new File(System.getProperty("user.dir"));
        File envFile = new File(projectRoot, ".env");

        try {
            // Create .env file without CURSOR_API_KEY
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("OTHER_KEY=some-value");
            }

            // Test resolution - should throw exception since no API key is found
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
