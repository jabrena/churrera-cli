package info.jab.churrera.cli.model;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.AgentStatus;

import java.util.Objects;

/**
 * Class representing all possible agent states.
 * Uses AgentStatus enum values directly from the Cursor API specification.
 */
public final class AgentState {
    private final AgentStatus status;

    private AgentState(AgentStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    /**
     * Creates an AgentState from an AgentResponse.
     * If the response or status is null, defaults to CREATING.
     *
     * @param agent The agent response to parse (can be null)
     * @return AgentState representing the agent's current state
     */
    public static AgentState of(AgentResponse agent) {
        if (agent == null || agent.status() == null) {
            return CREATING();
        }
        return of(agent.status());
    }

    /**
     * Creates an AgentState directly from an AgentStatus enum value.
     *
     * @param status The AgentStatus enum value
     * @return AgentState instance
     */
    public static AgentState of(AgentStatus status) {
        if (status == null) {
            return CREATING();
        }
        return new AgentState(status);
    }

    /**
     * Creates an AgentState from a string representation.
     * If the string is null, empty, or cannot be parsed, defaults to CREATING.
     *
     * @param statusStr the status string to parse (can be null or empty)
     * @return AgentState instance, defaults to CREATING if parsing fails
     */
    public static AgentState of(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return CREATING();
        }

        String upperStatus = statusStr.toUpperCase().trim();

        return switch (upperStatus) {
            case "CREATING" -> CREATING();
            case "RUNNING" -> RUNNING();
            case "FINISHED" -> FINISHED();
            case "ERROR" -> ERROR();
            case "EXPIRED" -> EXPIRED();
            default -> CREATING();
        };
    }

    /**
     * Creates a CREATING state.
     *
     * @return AgentState with CREATING status
     */
    public static AgentState CREATING() {
        return new AgentState(AgentStatus.CREATING);
    }

    /**
     * Creates a RUNNING state.
     *
     * @return AgentState with RUNNING status
     */
    public static AgentState RUNNING() {
        return new AgentState(AgentStatus.RUNNING);
    }

    /**
     * Creates a FINISHED state.
     *
     * @return AgentState with FINISHED status
     */
    public static AgentState FINISHED() {
        return new AgentState(AgentStatus.FINISHED);
    }

    /**
     * Creates an ERROR state.
     *
     * @return AgentState with ERROR status
     */
    public static AgentState ERROR() {
        return new AgentState(AgentStatus.ERROR);
    }

    /**
     * Creates an EXPIRED state.
     *
     * @return AgentState with EXPIRED status
     */
    public static AgentState EXPIRED() {
        return new AgentState(AgentStatus.EXPIRED);
    }

    /**
     * Gets the underlying AgentStatus enum value.
     *
     * @return the AgentStatus enum value
     */
    public AgentStatus getStatus() {
        return status;
    }

    /**
     * Checks if this agent state represents a terminal state.
     * Terminal states indicate the agent has finished processing.
     *
     * @return true if the state is terminal, false otherwise
     */
    public boolean isTerminal() {
        return status == AgentStatus.FINISHED || status == AgentStatus.ERROR || status == AgentStatus.EXPIRED;
    }

    /**
     * Checks if this agent state represents a successful completion.
     *
     * @return true if the agent completed successfully, false otherwise
     */
    public boolean isSuccessful() {
        return status == AgentStatus.FINISHED;
    }

    /**
     * Checks if this agent state represents a failure.
     *
     * @return true if the agent failed, false otherwise
     */
    public boolean isFailed() {
        return status == AgentStatus.ERROR || status == AgentStatus.EXPIRED;
    }

    /**
     * Checks if this agent state represents an active (non-terminal) state.
     *
     * @return true if the agent is still active, false otherwise
     */
    public boolean isActive() {
        return !isTerminal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentState agentState = (AgentState) o;
        return status == agentState.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status);
    }

    @Override
    public String toString() {
        return status.name();
    }
}
