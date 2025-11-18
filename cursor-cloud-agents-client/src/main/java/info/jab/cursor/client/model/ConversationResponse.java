package info.jab.cursor.client.model;

import java.util.List;
import java.util.Objects;

import info.jab.cursor.generated.client.model.GetAgentConversation200Response;

/**
 * Domain model for a conversation response.
 */
public record ConversationResponse(
    String id,
    List<ConversationMessage> messages
) {
    // Compact record constructor
    public ConversationResponse {
        Objects.requireNonNull(id, "ID cannot be null");
        // messages can be null
    }

    /**
     * Factory method to create ConversationResponse from GetAgentConversation200Response.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static ConversationResponse from(GetAgentConversation200Response generated) {
        List<ConversationMessage> messages =
            generated.getMessages().stream()
                .map(ConversationMessage::from)
                .toList();
        return new ConversationResponse(generated.getId(), messages);
    }
}

