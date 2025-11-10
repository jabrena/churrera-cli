package info.jab.cursor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import info.jab.cursor.recorder.LaunchRecorder;
import info.jab.cursor.recorder.FollowUpRecorder;
import info.jab.cursor.recorder.DeleteRecorder;
import info.jab.cursor.recorder.ListAgentsRecorder;
import info.jab.cursor.recorder.AgentStatusRecorder;
import info.jab.cursor.recorder.AgentConversationRecorder;
import info.jab.cursor.recorder.ApiKeyInfoRecorder;
import info.jab.cursor.recorder.ListModelsRecorder;
import info.jab.cursor.recorder.ListRepositoriesRecorder;
import info.jab.cursor.util.CursorApiKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * E2E API Response Recorder
 *
 * This utility records real API responses from the Cursor Agent API to be used
 * for WireMock testing later. It makes actual API calls and stores the JSON responses
 * in the test resources directory.
 *
 * MODULAR ARCHITECTURE:
 * - ApiResponseRecorder: Main orchestrator with utility methods
 * - LaunchRecorder: Handles POST /v0/agents endpoint recordings (Agent Management)
 * - FollowUpRecorder: Handles POST /v0/agents/{id}/followup endpoint recordings (Agent Management)
 * - DeleteRecorder: Handles DELETE /v0/agents/{id} endpoint recordings (Agent Management)
 * - ListAgentsRecorder: Handles GET /v0/agents endpoint recordings (Agent Information)
 * - AgentStatusRecorder: Handles GET /v0/agents/{id} endpoint recordings (Agent Information)
 * - AgentConversationRecorder: Handles GET /v0/agents/{id}/conversation endpoint recordings (Agent Information)
 * - ApiKeyInfoRecorder: Handles GET /v0/me endpoint recordings (General Endpoints)
 * - ListModelsRecorder: Handles GET /v0/models endpoint recordings (General Endpoints)
 * - ListRepositoriesRecorder: Handles GET /v0/repositories endpoint recordings (General Endpoints)
 *
 * HOW TO USE:
 * 1. Set your API key using one of these methods:
 *    - Create a .env file with: CURSOR_API_KEY=your_api_key_here
 *    - Or set environment variable: export CURSOR_API_KEY=your_api_key_here
 * 2. Run: mvn compile exec:java -pl cursor-cloud-agents-client -Pe2e -Dexec.mainClass="info.jab.cursor.ApiResponseRecorder"
 *
 * The responses will be saved to:
 * - Agent Management: cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-management/
 * - Agent Information: cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-information/
 * - General Endpoints: cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-general/
 *
 * ENDPOINTS COVERED:
 *
 * AGENT MANAGEMENT:
 *
 * Launch Endpoint (POST /v0/agents):
 * - 200/201 Success: Valid agent launch
 * - 400 Bad Request: Invalid request payload
 * - 401 Unauthorized: Invalid or missing API key
 *
 * Follow-up Endpoint (POST /v0/agents/{id}/followup):
 * - 200 Success: Valid follow-up request
 * - 400 Bad Request: Invalid follow-up payload
 * - 401 Unauthorized: Invalid or missing API key
 * - 409 Conflict: Agent in conflicting state
 *
 * Delete Endpoint (DELETE /v0/agents/{id}):
 * - 200 Success: Agent deleted successfully
 * - 400 Bad Request: Invalid agent ID format
 * - 401 Unauthorized: Invalid or missing API key
 * - 409 Conflict: Agent is in use and cannot be deleted
 *
 * AGENT INFORMATION:
 *
 * List Agents Endpoint (GET /v0/agents):
 * - 200 Success: Agents retrieved successfully with pagination
 * - 400 Bad Request: Invalid query parameters
 * - 401 Unauthorized: Invalid or missing API key
 *
 * Agent Status Endpoint (GET /v0/agents/{id}):
 * - 200 Success: Agent details retrieved successfully
 * - 400 Bad Request: Invalid agent ID format
 * - 401 Unauthorized: Invalid or missing API key
 *
 * Agent Conversation Endpoint (GET /v0/agents/{id}/conversation):
 * - 200 Success: Conversation history retrieved successfully
 * - 400 Bad Request: Invalid agent ID format
 * - 401 Unauthorized: Invalid or missing API key
 * - 409 Conflict: Agent has been deleted or conversation not accessible
 *
 * GENERAL ENDPOINTS:
 *
 * API Key Info Endpoint (GET /v0/me):
 * - 200 Success: API key metadata retrieved successfully
 * - 401 Unauthorized: Invalid or missing API key
 * - 404 Not Found: API key not found or has been deleted
 *
 * List Models Endpoint (GET /v0/models):
 * - 200 Success: Models list retrieved successfully
 * - 401 Unauthorized: Invalid or missing API key
 *
 * List Repositories Endpoint (GET /v0/repositories):
 * - 200 Success: GitHub repositories list retrieved successfully
 * - 401 Unauthorized: Invalid or missing API key
 * NOTE: This endpoint has strict rate limits (1/user/minute, 30/user/hour)
 *
 * NOT COVERED (for most endpoints):
 * - 403 Forbidden: Not covered as requested
 * - 404 Not Found: Only covered for API Key Info endpoint
 * - 429 Rate Limit: Skipped to avoid excessive API calls
 */
public class ApiResponseRecorder {

    private static final Logger logger = LoggerFactory.getLogger(ApiResponseRecorder.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String TEST_REPOSITORY = "https://github.com/jabrena/churrera";
    private static final String TEST_MODEL = "claude-4-sonnet-thinking";

    public static void main(String[] args) {
        logger.info("=== Cursor Agent API Response Recorder ===");

        // Resolve API key using CursorApiKeyResolver (supports .env file and environment variables)
        String apiKey;
        try {
            apiKey = CursorApiKeyResolver.resolveApiKey();
            logger.info("✓ API key resolved successfully");
        } catch (IllegalArgumentException e) {
            logger.error("ERROR: {}", e.getMessage());
            System.exit(1);
            return; // Make compiler happy
        }

        recordManagementEndpoints(apiKey);
        recordInformationEndpoints(apiKey);
        recordGeneralEndpoints(apiKey);

        System.out.println("\n=== Recording Complete ===");
        System.out.println("Responses saved to multiple directories:");
        System.out.println("  - Agent Management: cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-management/");
        System.out.println("  - Agent Information: cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-information/");
        System.out.println("  - General Endpoints: cursor-cloud-agents-client/src/test/resources/__files/cursor-agent-general/");
        logger.info("\n=== Recording Complete ===");
        logger.info("Responses saved to: cursor-cloud-agents-client/src/test/resources/__files/");
    }

    private static void recordManagementEndpoints(String apiKey) {

        System.out.println("\n=== Starting Management endpoints recordings ===");

        // Launch endpoint recordings
        System.out.println("\n=== Starting Launch endpoint recordings ===");
        logger.info("Starting Launch endpoint recordings...");
        try {
            LaunchRecorder launchRecorder = new LaunchRecorder();
            launchRecorder.recordAllCases(apiKey);
            System.out.println("✓ Launch recordings complete");
        } catch (Exception e) {
            System.err.println("✗ Launch recordings failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Follow-up endpoint recordings
        System.out.println("\n=== Starting Follow-up endpoint recordings ===");
        logger.info("Starting Follow-up endpoint recordings...");
        try {
            FollowUpRecorder followUpRecorder = new FollowUpRecorder();
            followUpRecorder.recordAllCases(apiKey);
            System.out.println("✓ Follow-up recordings complete");
        } catch (Exception e) {
            System.err.println("✗ Follow-up recordings failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Delete endpoint recordings
        System.out.println("\n=== Starting Delete endpoint recordings ===");
        logger.info("Starting Delete endpoint recordings...");
        try {
            DeleteRecorder deleteRecorder = new DeleteRecorder();
            deleteRecorder.recordAllCases(apiKey);
            System.out.println("✓ Delete recordings complete");
        } catch (Exception e) {
            System.err.println("✗ Delete recordings failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void recordInformationEndpoints(String apiKey) {
        System.out.println("\n=== Starting Information endpoints recordings ===");
        logger.info("Starting Information endpoints recordings...");
        try {
            ListAgentsRecorder listAgentsRecorder = new ListAgentsRecorder();
            listAgentsRecorder.recordAllCases(apiKey);
            System.out.println("✓ List Agents recordings complete");
        } catch (Exception e) {
            System.err.println("✗ List Agents recordings failed: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            AgentStatusRecorder agentStatusRecorder = new AgentStatusRecorder();
            agentStatusRecorder.recordAllCases(apiKey);
            System.out.println("✓ Agent Status recordings complete");
        } catch (Exception e) {
            System.err.println("✗ Agent Status recordings failed: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            AgentConversationRecorder agentConversationRecorder = new AgentConversationRecorder();
            agentConversationRecorder.recordAllCases(apiKey);
            System.out.println("✓ Agent Conversation recordings complete");
        } catch (Exception e) {
            System.err.println("✗ Agent Conversation recordings failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("\n=== Information endpoints recordings complete ===");
    }

    private static void recordGeneralEndpoints(String apiKey) {
        System.out.println("\n=== Starting General endpoints recordings ===");
        logger.info("Starting General endpoints recordings...");
        try {
            ApiKeyInfoRecorder apiKeyInfoRecorder = new ApiKeyInfoRecorder();
            apiKeyInfoRecorder.recordAllCases(apiKey);
            System.out.println("✓ API Key Info recordings complete");
        } catch (Exception e) {
            System.err.println("✗ API Key Info recordings failed: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            ListModelsRecorder listModelsRecorder = new ListModelsRecorder();
            listModelsRecorder.recordAllCases(apiKey);
            System.out.println("✓ List Models recordings complete");
        } catch (Exception e) {
            System.err.println("✗ List Models recordings failed: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            ListRepositoriesRecorder listRepositoriesRecorder = new ListRepositoriesRecorder();
            listRepositoriesRecorder.recordAllCases(apiKey);
            System.out.println("✓ List Repositories recordings complete");
        } catch (Exception e) {
            System.err.println("✗ List Repositories recordings failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("\n=== General endpoints recordings complete ===");
    }
}

