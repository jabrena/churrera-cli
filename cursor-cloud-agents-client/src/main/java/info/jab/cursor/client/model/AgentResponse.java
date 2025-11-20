package info.jab.cursor.client.model;

import java.time.OffsetDateTime;
import java.util.Objects;

import info.jab.cursor.generated.client.model.CreateAgent201Response;
import info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInner;

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
    //Compact record constructor with default values
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
    public static AgentResponse from(CreateAgent201Response generated) {
        if (generated == null) {
            return null;
        }
        return new AgentResponse(
            generated.getId(),
            generated.getName(),
            mapStatusEnum(generated.getStatus()),
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
    public static AgentResponse from(ListAgents200ResponseAgentsInner generated) {
        if (generated == null) {
            return null;
        }
        return new AgentResponse(
            generated.getId(),
            generated.getName(),
            mapStatusEnum(generated.getStatus()),
            Source.from(generated.getSource()),
            Target.from(generated.getTarget()),
            generated.getCreatedAt()
        );
    }


    /**
     * Maps CreateAgent201Response.StatusEnum to AgentStatus enum.
     *
     * @param statusEnum the status enum from CreateAgent201Response
     * @return corresponding AgentStatus enum, or CREATING if null or unknown
     */
    private static AgentStatus mapStatusEnum(CreateAgent201Response.StatusEnum statusEnum) {
        if (statusEnum == null) {
            return AgentStatus.CREATING;
        }
        return switch (statusEnum) {
            case CreateAgent201Response.StatusEnum.CREATING -> AgentStatus.CREATING;
        };
    }

    /**
     * Maps ListAgents200ResponseAgentsInner.StatusEnum to AgentStatus enum.
     *
     * @param statusEnum the status enum from ListAgents200ResponseAgentsInner
     * @return corresponding AgentStatus enum, or CREATING if null or unknown
     */
    private static AgentStatus mapStatusEnum(ListAgents200ResponseAgentsInner.StatusEnum statusEnum) {
        return switch (statusEnum) {
            case ListAgents200ResponseAgentsInner.StatusEnum.CREATING -> AgentStatus.CREATING;
            case ListAgents200ResponseAgentsInner.StatusEnum.RUNNING -> AgentStatus.RUNNING;
            case ListAgents200ResponseAgentsInner.StatusEnum.FINISHED -> AgentStatus.FINISHED;
            case ListAgents200ResponseAgentsInner.StatusEnum.ERROR -> AgentStatus.ERROR;
            case ListAgents200ResponseAgentsInner.StatusEnum.EXPIRED -> AgentStatus.EXPIRED;
            default -> AgentStatus.CREATING;
        };
    }

}

