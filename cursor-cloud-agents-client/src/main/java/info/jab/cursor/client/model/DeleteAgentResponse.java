package info.jab.cursor.client.model;

import java.util.Objects;

/**
 * Domain model for a delete agent response.
 */
public record DeleteAgentResponse(
    String id
) {
    public DeleteAgentResponse {
        Objects.requireNonNull(id, "ID cannot be null");
    }


    /**
     * Factory method to create DeleteAgentResponse from DeleteAgent200Response.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static DeleteAgentResponse from(info.jab.cursor.generated.client.model.DeleteAgent200Response generated) {
        if (generated == null) {
            return null;
        }
        return new DeleteAgentResponse(generated.getId());
    }
}

