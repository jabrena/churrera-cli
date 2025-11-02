package info.jab.churrera.agent;

import info.jab.cursor.client.model.AgentResponse;

/**
 * Enum representing all possible agent states.
 * Based on the Cursor API specification.
 */
public enum AgentState {
    PENDING,
    CREATING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED,
    FINISHED,
    UNKNOWN;

    /**
     * Checks if this agent state represents a terminal state.
     * Terminal states indicate the agent has finished processing.
     *
     * @return true if the state is terminal, false otherwise
     */
    public boolean isTerminal() {
        return switch (this) {
            case COMPLETED, FAILED, CANCELLED, EXPIRED, FINISHED -> true;
            case PENDING, CREATING, RUNNING, UNKNOWN -> false;
        };
    }

    /**
     * Checks if this agent state represents a successful completion.
     *
     * @return true if the agent completed successfully, false otherwise
     */
    public boolean isSuccessful() {
        return this == COMPLETED || this == FINISHED;
    }

    /**
     * Checks if this agent state represents a failure.
     *
     * @return true if the agent failed, false otherwise
     */
    public boolean isFailed() {
        return this == FAILED || this == CANCELLED || this == EXPIRED;
    }

    /**
     * Checks if this agent state represents an active (non-terminal) state.
     *
     * @return true if the agent is still active, false otherwise
     */
    public boolean isActive() {
        return !isTerminal();
    }

    /**
     * Parses an Agent object and returns the corresponding AgentStates enum.
     *
     * @param agent The agent to parse (can be null)
     * @return AgentStates enum representing the agent's current state
     */
    public static AgentState of(AgentResponse agent) {
        if (agent == null || agent.getStatus() == null) {
            return UNKNOWN;
        }

        String status = agent.getStatus().toString();
        return parseStatus(status);
    }

    /**
     * Parses a status string into an AgentStates enum.
     *
     * @param status The status string to parse
     * @return The corresponding AgentStates enum
     */
    private static AgentState parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return UNKNOWN;
        }

        String upperStatus = status.toUpperCase().trim();

        return switch (upperStatus) {
            case "PENDING" -> PENDING;
            case "CREATING" -> CREATING;
            case "RUNNING" -> RUNNING;
            case "COMPLETED" -> COMPLETED;
            case "FAILED" -> FAILED;
            case "CANCELLED" -> CANCELLED;
            case "EXPIRED" -> EXPIRED;
            case "FINISHED" -> FINISHED;
            default -> UNKNOWN;
        };
    }
}
