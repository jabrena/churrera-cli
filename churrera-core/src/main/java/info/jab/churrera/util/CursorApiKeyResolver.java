package info.jab.churrera.util;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Optional;

/**
 * Class for resolving API keys from various sources.
 * Uses functional approach with Optional and flatMap.
 */
public class CursorApiKeyResolver {

    /**
     * The name of the environment variable for the Cursor API key.
     */
    public static final String CURSOR_API_KEY = "CURSOR_API_KEY";

    /**
     * Resolves the API key from .env file or system environment using functional approach.
     * Priority: .env file > system environment variable
     *
     * @return The resolved API key
     * @throws IllegalArgumentException if no API key is found
     */
    public String resolveApiKey() {
        return resolveFromEnvFile()
            .or(this::resolveFromSystemEnvironment)
            .orElseThrow(() -> new IllegalArgumentException(
                "API key not found. Please provide it via:\n" +
                "  1. .env file: " + CURSOR_API_KEY + "=YOUR_API_KEY\n" +
                "  2. Environment variable: export " + CURSOR_API_KEY + "=YOUR_API_KEY"
            ));
    }

    /**
     * Lambda that resolves API key from .env file.
     * Returns Optional.empty() if not found or if there's an error.
     */
    private Optional<String> resolveFromEnvFile() {
        try {

            try {
                Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

                String envApiKey = dotenv.get(CURSOR_API_KEY);
                if (envApiKey != null && !envApiKey.trim().isEmpty()) {
                    return Optional.of(envApiKey.trim());
                }
            } catch (Exception e) {
                System.out.println("⚠️  Error loading .env : " + e.getMessage());
                // Continue to next path
                return Optional.empty();
            }

            return Optional.empty();
        } catch (Exception e) {
            System.err.println("⚠️  Could not read .env file: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Lambda that resolves API key from system environment variable.
     * Returns Optional.empty() if not found.
     */
    private Optional<String> resolveFromSystemEnvironment() {
        return Optional.ofNullable(System.getenv(CURSOR_API_KEY))
            .filter(key -> !key.trim().isEmpty())
            .map(String::trim);
    }
}
