package info.jab.cursor.recorder;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.cursor.client.CursorAgentInformation;
import info.jab.cursor.client.impl.CursorAgentInformationImpl;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.model.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Recorder for Agent Status endpoint responses.
 * Handles recording of various HTTP status codes for the GET /v0/agents/{id} endpoint.
 *
 * According to the Cursor API documentation: https://docs.cursor.com/en/background-agent/api/agent-status
 */
public class AgentStatusRecorder {

    private static final Logger logger = LoggerFactory.getLogger(AgentStatusRecorder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String OUTPUT_DIR = "cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-information";
    private static final String TEST_REPOSITORY = "https://github.com/jabrena/churrera";
    private static final String TEST_MODEL = "claude-4-sonnet-thinking";
    private static final String DEFAULT_API_BASE_URL = "https://api.cursor.com";

    /**
     * Record all agent status endpoint cases
     */
    public void recordAllCases(String apiKey) {
        System.out.println("  AgentStatusRecorder.recordAllCases() called");
        logger.info("\n=== Recording Agent Status Endpoint Cases ===");

        System.out.println("  → Recording success case...");
        recordAgentStatusSuccess(apiKey);

        System.out.println("  → Recording 404 error...");
        record404NotFound(apiKey);

        System.out.println("  → Recording 401 error...");
        record401Unauthorized();

        System.out.println("  AgentStatusRecorder.recordAllCases() complete");
    }

    /**
     * Record a successful agent status request (200)
     */
    private void recordAgentStatusSuccess(String apiKey) {
        try {
            System.out.println("    [SUCCESS] Starting...");
            logger.info("Recording successful agent status request...");
            CursorAgentInformation agentInformation = new CursorAgentInformationImpl(apiKey, DEFAULT_API_BASE_URL);

            // First, we need an agent ID to query status
            // Try to get agents list first
            var agentsList = agentInformation.getAgents(1, null);

            if (agentsList.getAgents() != null && !agentsList.getAgents().isEmpty()) {
                // Use the first agent from the list
                String agentId = agentsList.getAgents().get(0).getId();
                logger.info("  Using agent ID from list: {}", agentId);

                Agent statusResponse = agentInformation.getStatus(agentId);

                saveResponseToFile(statusResponse, "agent-status-200-ok.json");
                System.out.println("    [SUCCESS] ✓ File saved: agent-status-200-ok.json");
                logger.info("✓ Agent status success case recorded: agent-status-200-ok.json");
                logger.info("  Agent ID: {}", statusResponse.getId());
                logger.info("  Status: {}", statusResponse.getStatus());
            } else {
                throw new AssertionError("No agents available to test status endpoint");
            }

        } catch (Exception e) {
            System.err.println("    [SUCCESS] ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            logger.error("✗ Failed to record agent status success case: {}", e.getMessage(), e);
        }
    }

    /**
     * Record a 404 Not Found error for agent status by using invalid agent ID
     */
    private void record404NotFound(String apiKey) {
        try {
            System.out.println("    [404] Starting...");
            logger.info("Recording 404 Not Found error for agent status...");
            CursorAgentInformation agentInformation = new CursorAgentInformationImpl(apiKey, DEFAULT_API_BASE_URL);

            // Use an invalid agent ID to trigger a 404 error from the API
            try {
                agentInformation.getStatus("invalid-agent-id-that-does-not-exist-12345");
                throw new AssertionError("Expected 404 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 404) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "agent-status-404-not-found.json");
                            System.out.println("    [404] ✓ File saved: agent-status-404-not-found.json");
                            logger.info("✓ 404 error recorded: agent-status-404-not-found.json");
                        } else {
                            throw new AssertionError("Got 404 but response body was empty");
                        }
                    } else {
                        throw new AssertionError(String.format("Expected status code 404 but got %d. Response: %s",
                            apiException.getCode(), apiException.getResponseBody()));
                    }
                } else {
                    throw new AssertionError("Unexpected exception type: " + e.getClass().getName() + ". Message: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("    [404] ✗ FAILED: " + e.getMessage());
            logger.error("✗ Failed to record 404 error: {}", e.getMessage(), e);
        }
    }

    /**
     * Record a 401 Unauthorized error for agent status by using invalid API key
     */
    private void record401Unauthorized() {
        try {
            System.out.println("    [401] Starting...");
            logger.info("Recording 401 Unauthorized error for agent status...");
            CursorAgentInformation agentInformation = new CursorAgentInformationImpl("invalid_api_key_12345", DEFAULT_API_BASE_URL);

            try {
                agentInformation.getStatus("any-agent-id");
                throw new AssertionError("Expected 401 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 401) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "agent-status-401-unauthorized.json");
                            System.out.println("    [401] ✓ File saved: agent-status-401-unauthorized.json");
                            logger.info("✓ 401 error recorded: agent-status-401-unauthorized.json");
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

