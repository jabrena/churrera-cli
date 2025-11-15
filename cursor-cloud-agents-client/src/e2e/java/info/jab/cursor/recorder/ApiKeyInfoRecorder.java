package info.jab.cursor.recorder;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.cursor.client.CursorAgentGeneralEndpoints;
import info.jab.cursor.client.impl.CursorAgentGeneralEndpointsImpl;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.model.ApiKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Recorder for API Key Info endpoint responses.
 * Handles recording of various HTTP status codes for the GET /v0/me endpoint.
 *
 * According to the Cursor API documentation: https://docs.cursor.com/en/background-agent/api/api-key-info
 */
public class ApiKeyInfoRecorder {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyInfoRecorder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String OUTPUT_DIR = "cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-general";
    private static final String DEFAULT_API_BASE_URL = "https://api.cursor.com";

    /**
     * Record all API key info endpoint cases
     */
    public void recordAllCases(String apiKey) {
        System.out.println("  ApiKeyInfoRecorder.recordAllCases() called");
        logger.info("\n=== Recording API Key Info Endpoint Cases ===");

        System.out.println("  → Recording success case...");
        recordApiKeyInfoSuccess(apiKey);

        System.out.println("  → Recording 401 error...");
        record401Unauthorized();

        System.out.println("  ApiKeyInfoRecorder.recordAllCases() complete");
    }

    /**
     * Record a successful API key info request (200)
     */
    private void recordApiKeyInfoSuccess(String apiKey) {
        try {
            System.out.println("    [SUCCESS] Starting...");
            logger.info("Recording successful API key info request...");
            CursorAgentGeneralEndpoints generalEndpoints = new CursorAgentGeneralEndpointsImpl(apiKey, DEFAULT_API_BASE_URL);

            ApiKeyInfo response = generalEndpoints.getApiKeyInfo();

            saveResponseToFile(response, "api-key-info-200-ok.json");
            System.out.println("    [SUCCESS] ✓ File saved: api-key-info-200-ok.json");
            logger.info("✓ API key info success case recorded: api-key-info-200-ok.json");
            logger.info("  API Key Name: {}", response.getApiKeyName());
            logger.info("  User Email: {}", response.getUserEmail());
            logger.info("  Created At: {}", response.getCreatedAt());

        } catch (Exception e) {
            System.err.println("    [SUCCESS] ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            logger.error("✗ Failed to record API key info success case: {}", e.getMessage(), e);
        }
    }

    //TODO Add support for 404 error

    /**
     * Record a 401 Unauthorized error for API key info by using invalid API key
     */
    private void record401Unauthorized() {
        try {
            System.out.println("    [401] Starting...");
            logger.info("Recording 401 Unauthorized error for API key info...");
            CursorAgentGeneralEndpoints generalEndpoints = new CursorAgentGeneralEndpointsImpl("invalid_api_key_12345", DEFAULT_API_BASE_URL);

            try {
                generalEndpoints.getApiKeyInfo();
                throw new AssertionError("Expected 401 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 401) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "api-key-info-401-unauthorized.json");
                            System.out.println("    [401] ✓ File saved: api-key-info-401-unauthorized.json");
                            logger.info("✓ 401 error recorded: api-key-info-401-unauthorized.json");
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

