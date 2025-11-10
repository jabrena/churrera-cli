package info.jab.cursor.recorder;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.cursor.CursorAgentManagement;
import info.jab.cursor.CursorAgentManagementImpl;
import info.jab.cursor.client.ApiException;
import info.jab.cursor.client.model.Agent;
import info.jab.cursor.client.model.FollowUpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Recorder for Follow-up Agent endpoint responses.
 * Handles recording of various HTTP status codes for the POST /v0/agents/{id}/followup endpoint.
 */
public class FollowUpRecorder {

    private static final Logger logger = LoggerFactory.getLogger(FollowUpRecorder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String OUTPUT_DIR = "cursor-cloud-agents-api-java-client/src/test/resources/__files/cursor-agent-management";
    private static final String TEST_REPOSITORY = "https://github.com/jabrena/churrera";
    private static final String TEST_MODEL = "claude-4-sonnet-thinking";
    private static final String DEFAULT_API_BASE_URL = "https://api.cursor.com";

    /**
     * Record all follow-up endpoint cases
     */
    public void recordAllCases(String apiKey) {
        System.out.println("  FollowUpRecorder.recordAllCases() called");
        logger.info("\n=== Recording Follow-up Endpoint Cases ===");

        System.out.println("  → Recording success case...");
        recordFollowUpSuccess(apiKey);

        System.out.println("  → Recording 404 error...");
        record404NotFound(apiKey);

        System.out.println("  → Recording 401 error...");
        record401Unauthorized();

        System.out.println("  FollowUpRecorder.recordAllCases() complete");
    }

    /**
     * Record a successful follow-up request (200)
     */
    private void recordFollowUpSuccess(String apiKey) {
        try {
            System.out.println("    [SUCCESS] Starting...");
            logger.info("Recording successful follow-up request...");
            CursorAgentManagement agentManagement = new CursorAgentManagementImpl(apiKey, DEFAULT_API_BASE_URL);

            // First, launch an agent
            logger.info("  Launching agent for follow-up test...");
            Agent launchedAgent;
            try {
                launchedAgent = agentManagement.launch(
                    "Create a simple Java class with basic methods",
                    TEST_MODEL,
                    TEST_REPOSITORY,
                    true
                );
            } catch (RuntimeException launchEx) {
                // If repository access fails, create a template
                System.out.println("    [SUCCESS] ⚠ Could not launch agent (repository access): Creating template");
                String sampleSuccess = """
                    {
                      "id": "bc_followup_success"
                    }
                    """;
                saveStringToFile(sampleSuccess, "agent-followup-200-ok.json");
                System.out.println("    [SUCCESS] ✓ Template saved: agent-followup-200-ok.json");
                logger.warn("  Created template due to repository access issue: {}", launchEx.getMessage());
                return;
            }

            String agentId = launchedAgent.getId();
            logger.info("  Agent launched with ID: {}", agentId);

            // Wait briefly for agent to be ready
            Thread.sleep(3000);

            // Send follow-up request
            FollowUpResponse response = agentManagement.followUp(
                agentId,
                "Add unit tests for the new code"
            );

            saveResponseToFile(response, "agent-followup-200-ok.json");
            System.out.println("    [SUCCESS] ✓ File saved: agent-followup-200-ok.json");
            logger.info("✓ Follow-up success case recorded: agent-followup-200-ok.json");
            logger.info("  Follow-up response ID: {}", response.getId());

            // Clean up: delete the agent
            try {
                Thread.sleep(2000);
                agentManagement.delete(agentId);
                logger.info("  Cleaned up agent: {}", agentId);
            } catch (Exception e) {
                logger.warn("  Could not clean up agent (this is OK): {}", e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("    [SUCCESS] ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            logger.error("✗ Failed to record follow-up success case: {}", e.getMessage(), e);
        }
    }

    //TODO Add support for 400 error
    //TODO Add support for 409 error

    /**
     * Record a 404 Not Found error for follow-up by using invalid agent ID
     */
    private void record404NotFound(String apiKey) {
        try {
            System.out.println("    [404] Starting...");
            logger.info("Recording 404 Not Found error for follow-up...");
            CursorAgentManagement agentManagement = new CursorAgentManagementImpl(apiKey, DEFAULT_API_BASE_URL);

            // Use an invalid agent ID to trigger a 404 error from the API
            try {
                agentManagement.followUp(
                    "invalid-agent-id-that-does-not-exist-12345",
                    "Test follow-up message"
                );
                throw new AssertionError("Expected 404 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 404) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "agent-followup-404-not-found.json");
                            System.out.println("    [404] ✓ File saved: agent-followup-404-not-found.json");
                            logger.info("✓ 404 error recorded: agent-followup-404-not-found.json");
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
     * Record a 401 Unauthorized error for follow-up by using invalid API key
     */
    private void record401Unauthorized() {
        try {
            System.out.println("    [401] Starting...");
            logger.info("Recording 401 Unauthorized error for follow-up...");
            CursorAgentManagement agentManagement = new CursorAgentManagementImpl("invalid_api_key_12345", DEFAULT_API_BASE_URL);

            try {
                agentManagement.followUp(
                    "any-agent-id",
                    "Test follow-up message"
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
                            saveStringToFile(responseBody, "agent-followup-401-unauthorized.json");
                            System.out.println("    [401] ✓ File saved: agent-followup-401-unauthorized.json");
                            logger.info("✓ 401 error recorded: agent-followup-401-unauthorized.json");
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

