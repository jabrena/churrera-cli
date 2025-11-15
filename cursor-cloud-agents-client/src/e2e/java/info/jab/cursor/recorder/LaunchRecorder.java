package info.jab.cursor.recorder;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.cursor.client.CursorAgentManagement;
import info.jab.cursor.client.impl.CursorAgentManagementImpl;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.model.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Recorder for Launch Agent endpoint responses.
 * Handles recording of various HTTP status codes for the POST /v0/agents endpoint.
 */
public class LaunchRecorder {

    private static final Logger logger = LoggerFactory.getLogger(LaunchRecorder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String OUTPUT_DIR = "cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-management";
    private static final String TEST_REPOSITORY = "https://github.com/jabrena/churrera";
    private static final String TEST_MODEL = "claude-4-sonnet-thinking";
    private static final String DEFAULT_API_BASE_URL = "https://api.cursor.com";

    /**
     * Record all launch endpoint cases
     */
    public void recordAllCases(String apiKey) {
        System.out.println("  LaunchRecorder.recordAllCases() called");
        logger.info("\n=== Recording Launch Endpoint Cases ===");

        System.out.println("  → Recording success case...");
        recordSuccessCase(apiKey);

        System.out.println("  → Recording 400 model validation error...");
        record400ModelValidationError(apiKey);

        System.out.println("  → Recording 400 repository validation error...");
        record400RepositoryValidationError(apiKey);

        System.out.println("  → Recording 401 error...");
        record401Unauthorized();

        System.out.println("  LaunchRecorder.recordAllCases() complete");
    }

    /**
     * Record a successful agent launch (200/201)
     */
    private void recordSuccessCase(String apiKey) {
        try {
            System.out.println("    [SUCCESS] Starting...");
            logger.info("Recording successful agent launch...");
            CursorAgentManagement agentManagement = new CursorAgentManagementImpl(apiKey, DEFAULT_API_BASE_URL);

            Agent response = agentManagement.launch(
                "Add a README.md file with installation instructions",
                TEST_MODEL,
                TEST_REPOSITORY,
                true
            );

            saveResponseToFile(response, "agent-launch-ok.json");
            System.out.println("    [SUCCESS] ✓ File saved: agent-launch-ok.json");
            logger.info("✓ Success case recorded: agent-launch-ok.json");
            logger.info("  Agent ID: {}", response.getId());
            logger.info("  Status: {}", response.getStatus());

            // Clean up: delete the agent to avoid clutter
            try {
                Thread.sleep(2000); // Brief pause before cleanup
                agentManagement.delete(response.getId());
                logger.info("  Cleaned up agent: {}", response.getId());
            } catch (Exception e) {
                logger.warn("  Could not clean up agent (this is OK): {}", e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("    [SUCCESS] ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            logger.error("✗ Failed to record success case: {}", e.getMessage(), e);
        }
    }

    /**
     * Record a 400 Bad Request error by sending invalid model
     */
    private void record400ModelValidationError(String apiKey) {
        try {
            System.out.println("    [400] Starting...");
            logger.info("Recording 400 Bad Request error...");
            CursorAgentManagement agentManagement = new CursorAgentManagementImpl(apiKey, DEFAULT_API_BASE_URL);

            // Use an invalid model name to trigger a 400 error from the API
            try {
                agentManagement.launch(
                    "Test prompt for invalid model",
                    "invalid-model-that-does-not-exist",
                    TEST_REPOSITORY,
                    true
                );
                throw new AssertionError("Expected 400 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 400) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "agent-launch-400-model-validation-error.json");
                            System.out.println("    [400] ✓ File saved: agent-launch-400-model-validation-error.json");
                            logger.info("✓ 400 error recorded: agent-launch-400-model-validation-error.json");
                        } else {
                            throw new AssertionError("Got 400 but response body was empty");
                        }
                    } else {
                        throw new AssertionError(String.format("Expected status code 400 but got %d. Response: %s",
                            apiException.getCode(), apiException.getResponseBody()));
                    }
                } else {
                    throw new AssertionError("Unexpected exception type: " + e.getClass().getName() + ". Message: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("    [400] ✗ FAILED: " + e.getMessage());
            logger.error("✗ Failed to record 400 error: {}", e.getMessage(), e);
        }
    }

    /**
     * Record a 400 Bad Request error by sending invalid repository
     */
    private void record400RepositoryValidationError(String apiKey) {
        try {
            System.out.println("    [400] Starting...");
            logger.info("Recording 400 Bad Request error (invalid repository)...");
            CursorAgentManagement agentManagement = new CursorAgentManagementImpl(apiKey, DEFAULT_API_BASE_URL);

            // Use an invalid GitHub repository URL to trigger a 400 error from the API
            try {
                agentManagement.launch(
                    "Test prompt for invalid repository",
                    TEST_MODEL,
                    "https://github.com/this-is-not-a-valid-repo-12345/definitely-does-not-exist",
                    true
                );
                throw new AssertionError("Expected 400 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 400) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "agent-launch-400-repository-validation-error.json");
                            System.out.println("    [400] ✓ File saved: agent-launch-400-repository-validation-error.json");
                            logger.info("✓ 400 error recorded: agent-launch-400-repository-validation-error.json");
                        } else {
                            throw new AssertionError("Got 400 but response body was empty");
                        }
                    } else {
                        throw new AssertionError(String.format("Expected status code 400 but got %d. Response: %s",
                            apiException.getCode(), apiException.getResponseBody()));
                    }
                } else {
                    throw new AssertionError("Unexpected exception type: " + e.getClass().getName() + ". Message: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("    [400] ✗ FAILED: " + e.getMessage());
            logger.error("✗ Failed to record 400 repository error: {}", e.getMessage(), e);
        }
    }

    /**
     * Record a 401 Unauthorized error by using invalid API key
     */
    private void record401Unauthorized() {
        try {
            System.out.println("    [401] Starting...");
            logger.info("Recording 401 Unauthorized error...");
            CursorAgentManagement agentManagement = new CursorAgentManagementImpl("invalid_api_key_12345", DEFAULT_API_BASE_URL);

            try {
                agentManagement.launch(
                    "Test prompt",
                    TEST_MODEL,
                    TEST_REPOSITORY,
                    true
                );
                throw new AssertionError("Expected 401 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 401) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "agent-launch-401-unauthorized.json");
                            System.out.println("    [401] ✓ File saved: agent-launch-401-unauthorized.json");
                            logger.info("✓ 401 error recorded: agent-launch-401-unauthorized.json");
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
        objectMapper.writeValue(outputPath.toFile(), response);
    }

    /**
     * Save a JSON string to a file
     */
    private void saveStringToFile(String content, String filename) throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR, filename);
        Files.writeString(outputPath, content);
    }
}
