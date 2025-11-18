package info.jab.cursor.client.model;

import java.util.Objects;

import info.jab.cursor.generated.client.model.DeleteAgent200Response;

/**
 * Domain model for a follow-up response.
 */
public record FollowUpResponse(
    String id
) {
    // Compact record constructor
    public FollowUpResponse {
        Objects.requireNonNull(id, "ID cannot be null");
    }

    /**
     * Factory method to create FollowUpResponse from DeleteAgent200Response.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static FollowUpResponse from(DeleteAgent200Response generated) {
        return new FollowUpResponse(generated.getId());
    }
}

