package info.jab.cursor.recorder;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.cursor.client.CursorAgentGeneralEndpoints;
import info.jab.cursor.client.impl.CursorAgentGeneralEndpointsImpl;
import info.jab.cursor.generated.client.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Recorder for List Models endpoint responses.
 * Handles recording of various HTTP status codes for the GET /v0/models endpoint.
 *
 * According to the Cursor API documentation: https://docs.cursor.com/en/background-agent/api/list-models
 */
public class ListModelsRecorder {

    private static final Logger logger = LoggerFactory.getLogger(ListModelsRecorder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String OUTPUT_DIR = "cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-general";
    private static final String DEFAULT_API_BASE_URL = "https://api.cursor.com";

    /**
     * Record all list models endpoint cases
     */
    public void recordAllCases(String apiKey) {
        System.out.println("  ListModelsRecorder.recordAllCases() called");
        logger.info("\n=== Recording List Models Endpoint Cases ===");

        System.out.println("  → Recording success case...");
        recordListModelsSuccess(apiKey);

        System.out.println("  → Recording 401 error...");
        record401Unauthorized();

        System.out.println("  ListModelsRecorder.recordAllCases() complete");
    }

    /**
     * Record a successful list models request (200)
     */
    private void recordListModelsSuccess(String apiKey) {
        try {
            System.out.println("    [SUCCESS] Starting...");
            logger.info("Recording successful list models request...");
            CursorAgentGeneralEndpoints generalEndpoints = new CursorAgentGeneralEndpointsImpl(apiKey, DEFAULT_API_BASE_URL);

            List<String> models = generalEndpoints.getModels();

            // Create a simple JSON object with models array
            String jsonResponse = objectMapper.writeValueAsString(
                new ModelsResponse(models)
            );

            saveStringToFile(jsonResponse, "models-list-200-ok.json");
            System.out.println("    [SUCCESS] ✓ File saved: models-list-200-ok.json");
            logger.info("✓ List models success case recorded: models-list-200-ok.json");
            logger.info("  Available models: {}", models);

        } catch (Exception e) {
            System.err.println("    [SUCCESS] ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            logger.error("✗ Failed to record list models success case: {}", e.getMessage(), e);
        }
    }

    /**
     * Record a 401 Unauthorized error for list models by using invalid API key
     */
    private void record401Unauthorized() {
        try {
            System.out.println("    [401] Starting...");
            logger.info("Recording 401 Unauthorized error for list models...");
            CursorAgentGeneralEndpoints generalEndpoints = new CursorAgentGeneralEndpointsImpl("invalid_api_key_12345", DEFAULT_API_BASE_URL);

            try {
                generalEndpoints.getModels();
                throw new AssertionError("Expected 401 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 401) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "models-list-401-unauthorized.json");
                            System.out.println("    [401] ✓ File saved: models-list-401-unauthorized.json");
                            logger.info("✓ 401 error recorded: models-list-401-unauthorized.json");
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
     * Save a JSON string to a file
     */
    private void saveStringToFile(String content, String filename) throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR, filename);
        // Create parent directory if it doesn't exist
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, content);
    }

    /**
     * Simple wrapper class for models response
     */
    private static class ModelsResponse {
        private final List<String> models;

        public ModelsResponse(List<String> models) {
            this.models = models;
        }

        public List<String> getModels() {
            return models;
        }
    }
}

