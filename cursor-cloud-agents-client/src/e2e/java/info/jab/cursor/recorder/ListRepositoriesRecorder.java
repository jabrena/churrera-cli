package info.jab.cursor.recorder;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.cursor.client.CursorAgentGeneralEndpoints;
import info.jab.cursor.client.impl.CursorAgentGeneralEndpointsImpl;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.model.RepositoriesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Recorder for List GitHub Repositories endpoint responses.
 * Handles recording of various HTTP status codes for the GET /v0/repositories endpoint.
 *
 * NOTE: This endpoint has very strict rate limits (1/user/minute, 30/user/hour)
 * and can take tens of seconds to respond for users with many repositories.
 *
 * According to the Cursor API documentation: https://docs.cursor.com/en/background-agent/api/list-repositories
 */
public class ListRepositoriesRecorder {

    private static final Logger logger = LoggerFactory.getLogger(ListRepositoriesRecorder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String OUTPUT_DIR = "cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-general";
    private static final String DEFAULT_API_BASE_URL = "https://api.cursor.com";

    /**
     * Record all list repositories endpoint cases
     */
    public void recordAllCases(String apiKey) {
        System.out.println("  ListRepositoriesRecorder.recordAllCases() called");
        logger.info("\n=== Recording List Repositories Endpoint Cases ===");
        logger.warn("NOTE: This endpoint has strict rate limits (1/user/minute, 30/user/hour)");

        System.out.println("  → Recording success case...");
        recordListRepositoriesSuccess(apiKey);

        System.out.println("  → Recording 401 error...");
        record401Unauthorized();

        System.out.println("  ListRepositoriesRecorder.recordAllCases() complete");
    }

    /**
     * Record a successful list repositories request (200)
     * NOTE: This request can take tens of seconds for users with many repositories
     */
    private void recordListRepositoriesSuccess(String apiKey) {
        try {
            System.out.println("    [SUCCESS] Starting...");
            logger.info("Recording successful list repositories request...");
            logger.warn("  This may take tens of seconds to complete...");
            CursorAgentGeneralEndpoints generalEndpoints = new CursorAgentGeneralEndpointsImpl(apiKey, DEFAULT_API_BASE_URL);

            RepositoriesList response = generalEndpoints.getRepositories();

            saveResponseToFile(response, "repositories-list-200-ok.json");
            System.out.println("    [SUCCESS] ✓ File saved: repositories-list-200-ok.json");
            logger.info("✓ List repositories success case recorded: repositories-list-200-ok.json");
            if (response.getRepositories() != null) {
                logger.info("  Found {} repositories", response.getRepositories().size());
            }

        } catch (Exception e) {
            System.err.println("    [SUCCESS] ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            logger.error("✗ Failed to record list repositories success case: {}", e.getMessage(), e);
        }
    }

    /**
     * Record a 401 Unauthorized error for list repositories by using invalid API key
     */
    private void record401Unauthorized() {
        try {
            System.out.println("    [401] Starting...");
            logger.info("Recording 401 Unauthorized error for list repositories...");
            CursorAgentGeneralEndpoints generalEndpoints = new CursorAgentGeneralEndpointsImpl("invalid_api_key_12345", DEFAULT_API_BASE_URL);

            try {
                generalEndpoints.getRepositories();
                throw new AssertionError("Expected 401 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 401) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "repositories-list-401-unauthorized.json");
                            System.out.println("    [401] ✓ File saved: repositories-list-401-unauthorized.json");
                            logger.info("✓ 401 error recorded: repositories-list-401-unauthorized.json");
                        } else {
                            throw new AssertionError("Got 401 but response body was empty");
                        }
                    } else {
                        throw new AssertionError(String.format("Expected status code 401 but got %d. Response: %s",
                            apiException.getCode(), apiException.getResponseBody()));
                    }
                } else {
                    throw new AssertionError("Unexpected exception type: " + e.getClass().getName() + ". Message: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("    [401] ✗ FAILED: " + e.getMessage());
            logger.error("✗ Failed to record 401 error: {}", e.getMessage(), e);
        }
    }

    /**
     * Save a response object to a JSON file
     */
    private void saveResponseToFile(Object response, String filename) throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR, filename);
        // Create parent directory if it doesn't exist
        Files.createDirectories(outputPath.getParent());
        objectMapper.writeValue(outputPath.toFile(), response);
    }

    /**
     * Save a JSON string to a file
     */
    private void saveStringToFile(String content, String filename) throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR, filename);
        // Create parent directory if it doesn't exist
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, content);
    }
}

