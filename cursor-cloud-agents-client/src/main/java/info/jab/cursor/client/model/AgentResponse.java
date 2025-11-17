package info.jab.cursor.client.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Domain model for an agent response.
 */
public record AgentResponse(
    String id,
    String name,
    AgentStatus status,
    Source source,
    Target target,
    OffsetDateTime createdAt
) {
    public AgentResponse {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        // Status can be null, default to CREATING
        if (status == null) {
            status = AgentStatus.CREATING;
        }
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
        AgentStatus status = AgentStatus.CREATING; // default
        if (generated.getStatus() != null) {
            status = mapStatusEnum(generated.getStatus());
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

    /**
     * Maps StatusEnum from generated OpenAPI model to AgentStatus enum.
     *
     * @param statusEnum the StatusEnum from generated model
     * @return corresponding AgentStatus enum, or CREATING if unknown
     */
    private static AgentStatus mapStatusEnum(info.jab.cursor.generated.client.model.AgentResponse.StatusEnum statusEnum) {
        if (statusEnum == null) {
            return AgentStatus.CREATING;
        }
        return switch (statusEnum) {
            case CREATING -> AgentStatus.CREATING;
            case RUNNING -> AgentStatus.RUNNING;
            case FINISHED -> AgentStatus.FINISHED;
            case ERROR -> AgentStatus.ERROR;
            case EXPIRED -> AgentStatus.EXPIRED;
        };
    }

}

