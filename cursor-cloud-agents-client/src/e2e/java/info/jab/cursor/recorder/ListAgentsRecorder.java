package info.jab.cursor.recorder;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.cursor.client.CursorAgentInformation;
import info.jab.cursor.client.impl.CursorAgentInformationImpl;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.model.AgentsList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Recorder for List Agents endpoint responses.
 * Handles recording of various HTTP status codes for the GET /v0/agents endpoint.
 *
 * According to the Cursor API documentation: https://docs.cursor.com/en/background-agent/api/list-agents
 */
public class ListAgentsRecorder {

    private static final Logger logger = LoggerFactory.getLogger(ListAgentsRecorder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String OUTPUT_DIR = "cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-information";
    private static final String DEFAULT_API_BASE_URL = "https://api.cursor.com";

    /**
     * Record all list agents endpoint cases
     */
    public void recordAllCases(String apiKey) {
        System.out.println("  ListAgentsRecorder.recordAllCases() called");
        logger.info("\n=== Recording List Agents Endpoint Cases ===");

        System.out.println("  → Recording success case...");
        // TODO: Commented to not record so much data
        //recordListAgentsSuccess(apiKey);

        System.out.println("  → Recording 401 error...");
        record401Unauthorized();

        System.out.println("  ListAgentsRecorder.recordAllCases() complete");
    }

    /**
     * Record a successful list agents request (200)
     */
    private void recordListAgentsSuccess(String apiKey) {
        try {
            System.out.println("    [SUCCESS] Starting...");
            logger.info("Recording successful list agents request...");
            CursorAgentInformation agentInformation = new CursorAgentInformationImpl(apiKey, DEFAULT_API_BASE_URL);

            // Call the get agents API
            AgentsList response = agentInformation.getAgents(20, null);

            saveResponseToFile(response, "agents-list-200-ok.json");
            System.out.println("    [SUCCESS] ✓ File saved: agents-list-200-ok.json");
            logger.info("✓ List agents success case recorded: agents-list-200-ok.json");

            if (response.getAgents() != null) {
                logger.info("  Listed {} agents", response.getAgents().size());
            }
            if (response.getNextCursor() != null) {
                logger.info("  Next cursor: {}", response.getNextCursor());
            }

        } catch (Exception e) {
            System.err.println("    [SUCCESS] ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            logger.error("✗ Failed to record list agents success case: {}", e.getMessage(), e);
        }
    }

    /**
     * Record a 401 Unauthorized error for list agents by using invalid API key
     */
    private void record401Unauthorized() {
        try {
            System.out.println("    [401] Starting...");
            logger.info("Recording 401 Unauthorized error for list agents...");
            CursorAgentInformation agentInformation = new CursorAgentInformationImpl("invalid_api_key_12345", DEFAULT_API_BASE_URL);

            try {
                agentInformation.getAgents(20, null);
                throw new AssertionError("Expected 401 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 401) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "agents-list-401-unauthorized.json");
                            System.out.println("    [401] ✓ File saved: agents-list-401-unauthorized.json");
                            logger.info("✓ 401 error recorded: agents-list-401-unauthorized.json");
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

