package info.jab.cursor.client.model;

import java.util.Objects;

/**
 * Domain model for a conversation message.
 */
public record ConversationMessage(
    String id,
    String type,
    String text
) {
    public ConversationMessage {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");
        // text can be null
    }


    /**
     * Factory method to create ConversationMessage from GetAgentConversation200ResponseMessagesInner.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static ConversationMessage from(info.jab.cursor.generated.client.model.GetAgentConversation200ResponseMessagesInner generated) {
        if (generated == null) {
            return null;
        }
        String type = null;
        if (generated.getType() != null) {
            type = generated.getType().toString();
        }
        return new ConversationMessage(
            generated.getId(),
            type,
            generated.getText()
        );
    }
}

