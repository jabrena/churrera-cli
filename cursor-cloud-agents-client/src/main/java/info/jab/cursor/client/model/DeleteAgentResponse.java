package info.jab.cursor.client.model;

import java.util.Objects;

import info.jab.cursor.generated.client.model.DeleteAgent200Response;

/**
 * Domain model for a delete agent response.
 */
public record DeleteAgentResponse(
    String id
) {
    // Compact record constructor
    public DeleteAgentResponse {
        Objects.requireNonNull(id, "ID cannot be null");
    }

    /**
     * Factory method to create DeleteAgentResponse from DeleteAgent200Response.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static DeleteAgentResponse from(DeleteAgent200Response generated) {
        return new DeleteAgentResponse(generated.getId());
    }
}

