package info.jab.cursor.recorder;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import info.jab.cursor.util.CursorApiKeyResolver;
import info.jab.cursor.CursorAgentManagement;
import info.jab.cursor.CursorAgentManagementImpl;
import info.jab.cursor.CursorAgentInformation;
import info.jab.cursor.CursorAgentInformationImpl;
import info.jab.cursor.client.model.Agent;
import info.jab.cursor.client.ApiException;
import info.jab.cursor.client.model.ConversationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Recorder for Agent Conversation endpoint responses.
 * Handles recording of various HTTP status codes for the GET /v0/agents/{id}/conversation endpoint.
 *
 * According to the Cursor API documentation: https://docs.cursor.com/en/background-agent/api/agent-conversation
 *
 * Note: If the background agent has been deleted, you cannot access the conversation.
 */
public class AgentConversationRecorder {

    private static final Logger logger = LoggerFactory.getLogger(AgentConversationRecorder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String OUTPUT_DIR = "cursor-cloud-agents-api-java-client/src/test/resources/__files/cursor-agent-information";
    private static final String TEST_REPOSITORY = "https://github.com/jabrena/churrera";
    private static final String TEST_MODEL = "claude-4-sonnet-thinking";
    private static final String DEFAULT_API_BASE_URL = "https://api.cursor.com";

    /**
     * Record all agent conversation endpoint cases
     */
    public void recordAllCases(String apiKey) {
        System.out.println("  AgentConversationRecorder.recordAllCases() called");
        logger.info("\n=== Recording Agent Conversation Endpoint Cases ===");

        System.out.println("  → Recording success case...");
        recordAgentConversationSuccess(apiKey);

        System.out.println("  → Recording 404 error...");
        record404NotFound(apiKey);

        System.out.println("  → Recording 401 error...");
        record401Unauthorized();

        System.out.println("  AgentConversationRecorder.recordAllCases() complete");
    }

    /**
     * Record a successful agent conversation request (200)
     */
    private void recordAgentConversationSuccess(String apiKey) {
        try {
            System.out.println("    [SUCCESS] Starting...");
            logger.info("Recording successful agent conversation request...");
            CursorAgentManagement agentManagement = new CursorAgentManagementImpl(apiKey, DEFAULT_API_BASE_URL);
            CursorAgentInformation agentInformation = new CursorAgentInformationImpl(apiKey, DEFAULT_API_BASE_URL);

            // Launch a new agent for conversation testing
            logger.info("  Launching agent for conversation test...");
            Agent launchedAgent = agentManagement.launch(
                "Create a simple test file and explain what you did",
                TEST_MODEL,
                TEST_REPOSITORY,
                true
            );

            String agentId = launchedAgent.getId();
            logger.info("  Agent launched with ID: {}", agentId);

            // Wait for 1 minute to allow the agent to process and generate conversation
            logger.info("  Waiting 60 seconds for agent to process and generate conversation...");
            System.out.println("    [SUCCESS] Waiting 60 seconds for agent processing...");
            Thread.sleep(60000); // 60 seconds

            // Now retrieve the conversation
            logger.info("  Retrieving agent conversation...");
            ConversationResponse conversationResponse = agentInformation.getAgentConversation(agentId);

            saveResponseToFile(conversationResponse, "agent-conversation-200-ok.json");
            System.out.println("    [SUCCESS] ✓ File saved: agent-conversation-200-ok.json");
            logger.info("✓ Agent conversation success case recorded: agent-conversation-200-ok.json");
            logger.info("  Agent ID: {}", conversationResponse.getId());
            if (conversationResponse.getMessages() != null) {
                logger.info("  Messages count: {}", conversationResponse.getMessages().size());
            }

            // Clean up: delete the test agent
            logger.info("  Cleaning up: deleting test agent...");
            agentManagement.delete(agentId);
            logger.info("  Test agent deleted successfully");

        } catch (Exception e) {
            System.err.println("    [SUCCESS] ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            logger.error("✗ Failed to record agent conversation success case: {}", e.getMessage(), e);
        }
    }

    /**
     * Record a 404 Not Found error for agent conversation by using invalid agent ID
     */
    private void record404NotFound(String apiKey) {
        try {
            System.out.println("    [404] Starting...");
            logger.info("Recording 404 Not Found error for agent conversation...");
            CursorAgentInformation agentInformation = new CursorAgentInformationImpl(apiKey, DEFAULT_API_BASE_URL);

            // Use an invalid agent ID to trigger a 404 error from the API
            try {
                agentInformation.getAgentConversation("invalid-agent-id-that-does-not-exist-12345");
                throw new AssertionError("Expected 404 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 404) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "agent-conversation-404-not-found.json");
                            System.out.println("    [404] ✓ File saved: agent-conversation-404-not-found.json");
                            logger.info("✓ 404 error recorded: agent-conversation-404-not-found.json");
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
     * Record a 401 Unauthorized error for agent conversation by using invalid API key
     */
    private void record401Unauthorized() {
        try {
            System.out.println("    [401] Starting...");
            logger.info("Recording 401 Unauthorized error for agent conversation...");
            CursorAgentInformation agentInformation = new CursorAgentInformationImpl("invalid_api_key_12345", DEFAULT_API_BASE_URL);

            try {
                agentInformation.getAgentConversation("any-agent-id");
                throw new AssertionError("Expected 401 error but request succeeded");
            } catch (RuntimeException e) {
                // Extract the underlying ApiException
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    logger.info("  Caught ApiException with status code: {}", apiException.getCode());

                    if (apiException.getCode() == 401) {
                        String responseBody = apiException.getResponseBody();
                        if (responseBody != null && !responseBody.isEmpty()) {
                            saveStringToFile(responseBody, "agent-conversation-401-unauthorized.json");
                            System.out.println("    [401] ✓ File saved: agent-conversation-401-unauthorized.json");
                            logger.info("✓ 401 error recorded: agent-conversation-401-unauthorized.json");
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

    //TODO Add support for 400 error
    //TODO Add support for 409 error

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

