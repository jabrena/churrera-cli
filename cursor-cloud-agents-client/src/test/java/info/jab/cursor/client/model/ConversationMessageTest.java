package info.jab.cursor.client.model;

import info.jab.cursor.generated.client.model.GetAgentConversation200ResponseMessagesInner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConversationMessage model class.
 */
@DisplayName("ConversationMessage Model Tests")
class ConversationMessageTest {

    @Nested
    @DisplayName("from(GetAgentConversation200ResponseMessagesInner) Tests")
    class FromGetAgentConversation200ResponseMessagesInnerTests {

        @Test
        @DisplayName("Should return null when generated is null")
        void should_returnNull_when_generatedIsNull() {
            // When
            ConversationMessage result = ConversationMessage.from(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should create ConversationMessage when all fields are provided")
        void should_createConversationMessage_when_allFieldsProvided() {
            // Given
            GetAgentConversation200ResponseMessagesInner generated = 
                new GetAgentConversation200ResponseMessagesInner();
            generated.setId("message-id");
            generated.setType(GetAgentConversation200ResponseMessagesInner.TypeEnum.USER_MESSAGE);
            generated.setText("Hello, world!");

            // When
            ConversationMessage result = ConversationMessage.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("message-id");
            assertThat(result.type()).isEqualTo("user_message");
            assertThat(result.text()).isEqualTo("Hello, world!");
        }

        @Test
        @DisplayName("Should create ConversationMessage with null text when text is null")
        void should_createConversationMessageWithNullText_when_textIsNull() {
            // Given
            GetAgentConversation200ResponseMessagesInner generated = 
                new GetAgentConversation200ResponseMessagesInner();
            generated.setId("message-id");
            generated.setType(GetAgentConversation200ResponseMessagesInner.TypeEnum.USER_MESSAGE);
            generated.setText(null);

            // When
            ConversationMessage result = ConversationMessage.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("message-id");
            assertThat(result.type()).isEqualTo("user_message");
            assertThat(result.text()).isNull();
        }

        @Test
        @DisplayName("Should create ConversationMessage when type is assistant_message")
        void should_createConversationMessage_when_typeIsAssistantMessage() {
            // Given
            GetAgentConversation200ResponseMessagesInner generated = 
                new GetAgentConversation200ResponseMessagesInner();
            generated.setId("message-id");
            generated.setType(GetAgentConversation200ResponseMessagesInner.TypeEnum.ASSISTANT_MESSAGE);
            generated.setText("Assistant response");

            // When
            ConversationMessage result = ConversationMessage.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("message-id");
            assertThat(result.type()).isEqualTo("assistant_message");
            assertThat(result.text()).isEqualTo("Assistant response");
        }
    }
}

