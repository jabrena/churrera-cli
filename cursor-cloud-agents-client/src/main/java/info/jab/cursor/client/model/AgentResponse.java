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
     * Factory method to create AgentResponse from CreateAgent201Response.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static AgentResponse from(info.jab.cursor.generated.client.model.CreateAgent201Response generated) {
        if (generated == null) {
            return null;
        }
        AgentStatus status = AgentStatus.CREATING; // default
        if (generated.getStatus() != null) {
            status = mapStatusEnumFromString(generated.getStatus().getValue());
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
     * Factory method to create AgentResponse from ListAgents200ResponseAgentsInner.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static AgentResponse from(info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInner generated) {
        if (generated == null) {
            return null;
        }
        AgentStatus status = AgentStatus.CREATING; // default
        if (generated.getStatus() != null) {
            status = mapStatusEnumFromString(generated.getStatus().getValue());
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
     * Maps status string to AgentStatus enum.
     *
     * @param statusValue the status string value
     * @return corresponding AgentStatus enum, or CREATING if unknown
     */
    private static AgentStatus mapStatusEnumFromString(String statusValue) {
        if (statusValue == null) {
            return AgentStatus.CREATING;
        }
        return switch (statusValue) {
            case "CREATING" -> AgentStatus.CREATING;
            case "RUNNING" -> AgentStatus.RUNNING;
            case "FINISHED" -> AgentStatus.FINISHED;
            case "ERROR" -> AgentStatus.ERROR;
            case "EXPIRED" -> AgentStatus.EXPIRED;
            default -> AgentStatus.CREATING;
        };
    }

}

