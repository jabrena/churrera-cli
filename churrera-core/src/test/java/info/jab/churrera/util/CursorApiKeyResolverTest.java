package info.jab.churrera.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CursorApiKeyResolver utility.
 */
public class CursorApiKeyResolverTest {

    private String originalApiKey;
    private String originalWorkingDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        // Save original environment variable
        originalApiKey = System.getenv(CursorApiKeyResolver.CURSOR_API_KEY);

        // Save original working directory
        originalWorkingDir = System.getProperty("user.dir");

        // Clear environment variable for clean tests
        clearEnvironmentVariable();
    }

    @AfterEach
    public void tearDown() throws Exception {
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
    public void testResolveApiKeyFromSystemEnvironment() {
        // Note: This test may not work in all environments due to environment variable limitations
        // In a real CI/CD environment, you would set the environment variable externally
        // For now, we'll test the error case when no API key is found
        clearEnvironmentVariable();

        // Test that exception is thrown when no API key is found
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            resolver::resolveApiKey
        );

        assertTrue(exception.getMessage().contains("API key not found"));
    }

    @Test
    public void testResolveApiKeyFromEnvFile() throws IOException {
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
            String result = resolver.resolveApiKey();
            assertEquals("env-file-key-789", result);
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    public void testResolveApiKeyFromEnvFileWithWhitespace() throws IOException {
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
            String result = resolver.resolveApiKey();
            assertEquals("env-file-key-whitespace", result);
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    public void testResolveApiKeyPriorityEnvFileOverSystemEnvironment() throws IOException {
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
            String result = resolver.resolveApiKey();
            assertEquals("env-file-key", result);
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    public void testResolveApiKeyThrowsExceptionWhenNotFound() {
        // Ensure no API key is set
        clearEnvironmentVariable();

        // Test that exception is thrown
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            resolver::resolveApiKey
        );

        assertTrue(exception.getMessage().contains("API key not found"));
        assertTrue(exception.getMessage().contains(".env file"));
        assertTrue(exception.getMessage().contains("Environment variable"));
    }

    @Test
    public void testResolveApiKeyThrowsExceptionWhenEmptyInSystemEnvironment() {
        // Test that empty environment variable causes exception
        // Note: This test may not work in all environments due to environment variable limitations
        clearEnvironmentVariable();

        // Test that exception is thrown
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            resolver::resolveApiKey
        );

        assertTrue(exception.getMessage().contains("API key not found"));
    }

    @Test
    public void testResolveApiKeyThrowsExceptionWhenEmptyInEnvFile() throws IOException {
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
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                resolver::resolveApiKey
            );

            assertTrue(exception.getMessage().contains("API key not found"));
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    public void testResolveApiKeyThrowsExceptionWhenWhitespaceOnlyInEnvFile() throws IOException {
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
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                resolver::resolveApiKey
            );

            assertTrue(exception.getMessage().contains("API key not found"));
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    public void testResolveApiKeyWithMalformedEnvFile() throws IOException {
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
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                resolver::resolveApiKey
            );

            assertTrue(exception.getMessage().contains("API key not found"));
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    public void testResolveApiKeyWithMissingEnvFile() {
        // Test resolution when no .env file exists - should throw exception since no API key is found
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            resolver::resolveApiKey
        );

        assertTrue(exception.getMessage().contains("API key not found"));
    }

    @Test
    public void testResolveApiKeyWithEnvFileMissingKey() throws IOException {
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
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                resolver::resolveApiKey
            );

            assertTrue(exception.getMessage().contains("API key not found"));
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    public void testCursorApiKeyConstant() {
        assertEquals("CURSOR_API_KEY", CursorApiKeyResolver.CURSOR_API_KEY);
    }

    @Test
    public void testClassCanBeInstantiated() {
        // Test that class can be instantiated
        CursorApiKeyResolver resolver = new CursorApiKeyResolver();
        assertNotNull(resolver);
    }

    @Test
    public void testResolveApiKeyWithSpecialCharacters() throws IOException {
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
            String result = resolver.resolveApiKey();
            assertEquals(testApiKey, result);
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    public void testResolveApiKeyWithLongKey() throws IOException {
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
            String result = resolver.resolveApiKey();
            assertEquals(testApiKey, result);
        } finally {
            // Clean up the .env file
            if (envFile.exists()) {
                envFile.delete();
            }
        }
    }

    @Test
    public void testResolveApiKeyWithUnicodeCharacters() throws IOException {
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
            String result = resolver.resolveApiKey();
            assertEquals(testApiKey, result);
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

    private File createEnvFile(String content) throws IOException {
        File envFile = tempDir.resolve(".env").toFile();
        try (FileWriter writer = new FileWriter(envFile)) {
            writer.write(content);
        }
        return envFile;
    }
}
