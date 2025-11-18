package info.jab.cursor.client.model;

import java.util.List;
import java.util.Objects;

import info.jab.cursor.generated.client.model.ListAgents200Response;

/**
 * Domain model for a list of agents.
 */
public record AgentsList(
    List<AgentResponse> agents,
    String nextCursor
) {
    // Compact record constructor
    public AgentsList {
        Objects.requireNonNull(agents, "Agents cannot be null");
    }

    /**
     * Factory method to create AgentsList from ListAgents200Response.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static AgentsList from(ListAgents200Response generated) {
        if (generated == null) {
            return null;
        }
        List<AgentResponse> agents = generated.getAgents() != null
            ? generated.getAgents().stream()
                .map(AgentResponse::from)
                .toList()
            : List.of();
        return new AgentsList(agents, generated.getNextCursor());
    }
}

