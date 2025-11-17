package info.jab.cursor.client.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Domain model for an agent response.
 */
public record AgentResponse(
    String id,
    String name,
    String status,
    Source source,
    Target target,
    OffsetDateTime createdAt
) {
    public AgentResponse {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        // Status can be null (handled by AgentState.of())
        Objects.requireNonNull(source, "Source cannot be null");
        Objects.requireNonNull(target, "Target cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
    }

    /**
     * Factory method to create AgentResponse from generated OpenAPI model.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static AgentResponse from(info.jab.cursor.generated.client.model.AgentResponse generated) {
        if (generated == null) {
            return null;
        }
        //TODO: Handle status enum values
        String status = null;
        if (generated.getStatus() != null) {
            if (generated.getStatus() instanceof info.jab.cursor.generated.client.model.AgentResponse.StatusEnum) {
                status = ((info.jab.cursor.generated.client.model.AgentResponse.StatusEnum) generated.getStatus()).getValue();
            } else {
                status = generated.getStatus().toString();
            }
        }
        return new AgentResponse(
            generated.getId(),
            generated.getName(),
            status,
            Source.from(generated.getSource()),
            Target.from(generated.getTarget()),
            generated.getCreatedAt()
        );
    }
}

