package info.jab.churrera.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for ConversationJsonDeserializer.
 */
@DisplayName("ConversationJsonDeserializer Tests")
class ConversationJsonDeserializerTest {

    @Test
    @DisplayName("Should deserialize list from direct array format")
    void shouldDeserializeListWithDirectArray() {
        // Given
        String conversationContent = "Some conversation text\n<result>[1, 2, 3, 4]</result>\nMore text";

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(4)
                    .containsExactly(1, 2, 3, 4);
            });
    }

    @Test
    @DisplayName("Should deserialize list from object wrapper format")
    void shouldDeserializeListWithObjectWrapper() {
        // Given
        String conversationContent = "Some conversation text\n<result>{\"integers\": [1, 2, 3, 4]}</result>\nMore text";

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(4)
                    .containsExactly(1, 2, 3, 4);
            });
    }

    @Test
    @DisplayName("Should deserialize list from real conversation sample")
    void shouldDeserializeListWithRealConversationSample() throws IOException {
        // Given
        java.io.InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream("conversation-result-sample.txt");
        assertThat(inputStream).isNotNull();
        String conversationContent = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(4)
                    .containsExactly(1, 2, 3, 4);
            });
    }

    @Test
    @DisplayName("Should deserialize list from complex object wrapper")
    void shouldDeserializeListWithComplexObjectWrapper() {
        // Given
        String conversationContent = "<result>{\"data\": {\"values\": [5, 10, 15]}}</result>";

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(3)
                    .containsExactly(5, 10, 15);
            });
    }

    @Test
    @DisplayName("Should deserialize list from API-like conversation format")
    void shouldDeserializeListWithAPILikeConversationFormat() {
        // Given
        String conversationContent = """
            Let me help you with that task.
            I'll process the data now.
            <result>{"numbers": [10, 20, 30, 40]}</result>
            The task is complete.
            """;

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(4)
                    .containsExactly(10, 20, 30, 40);
            });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Just some conversation text without result tags",
        "<result></result>",
        "<result>{invalid json}</result>"
    })
    @DisplayName("Should return empty for invalid or missing content")
    void shouldReturnEmptyForInvalidOrMissingContent(String conversationContent) {
        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should use the last result tag when multiple result tags exist")
    void shouldUseLastResultTagWhenMultipleResultTagsExist() {
        // Given
        String conversationContent = """
            <result>[1, 2]</result>
            Some text
            <result>[3, 4, 5]</result>
            """;

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        // Then
        assertThat(result)
            .isPresent()
            .hasValue(List.of(3, 4, 5));
    }

    @Test
    @DisplayName("Should throw exception when conversation content is null")
    void shouldThrowExceptionWhenConversationContentIsNull() {
        // When & Then
        assertThatThrownBy(() -> ConversationJsonDeserializer.deserializeList(null, Integer.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception when target type is null")
    void shouldThrowExceptionWhenTargetTypeIsNull() {
        // When & Then
        assertThatThrownBy(() -> ConversationJsonDeserializer.deserializeList("<result>[1, 2]</result>", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should deserialize list of strings")
    void shouldDeserializeListWithStrings() {
        // Given
        String conversationContent = "<result>[\"apple\", \"banana\", \"cherry\"]</result>";

        // When
        Optional<List<String>> result = ConversationJsonDeserializer.deserializeList(conversationContent, String.class);

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(3)
                    .containsExactly("apple", "banana", "cherry");
            });
    }

    @Test
    @DisplayName("Should deserialize list of strings from object wrapper")
    void shouldDeserializeListWithObjectWrapperAndStrings() {
        // Given
        String conversationContent = "<result>{\"names\": [\"Alice\", \"Bob\", \"Charlie\"]}</result>";

        // When
        Optional<List<String>> result = ConversationJsonDeserializer.deserializeList(conversationContent, String.class);

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(3)
                    .containsExactly("Alice", "Bob", "Charlie");
            });
    }

    @Test
    @DisplayName("Should deserialize list with specific key matching")
    void shouldDeserializeListWithSpecificKeyMatching() {
        // Given
        String conversationContent = "<result>{\"List_Integer\": [1, 2, 3, 4]}</result>";

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(4)
                    .containsExactly(1, 2, 3, 4);
            });
    }

    @Test
    @DisplayName("Should deserialize last result with specific key when multiple result tags exist")
    void shouldDeserializeLastResultWithSpecificKeyWhenMultipleResultTagsExist() {
        // Given
        String conversationContent = """
            Here's the format:
            <result>RESULT</result>
            Now processing...
            <result>{"List_Integer": [1, 2, 3, 4]}</result>
            """;

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(4)
                    .containsExactly(1, 2, 3, 4);
            });
    }

    @Test
    @DisplayName("Should deserialize list from real scenario in logs")
    void shouldDeserializeListWithRealScenarioFromLogs() {
        // Given
        String conversationContent = """
            ## Role

            You are a Senior software engineer

            ## Output Format

            - Replace in the following xml fragment the RESULT with the actual List:
            <result>RESULT</result>
            - Only return the JSON format

            I'll help you scrape the homework numbers. Let me start:

            Now let me access the course page:

            Perfect! I can see the homework assignments. Let me extract the first 4:

            <result>{"List_Integer":[1,2,3,4]}</result>
            """;

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(4)
                    .containsExactly(1, 2, 3, 4);
            });
    }

    @Test
    @DisplayName("Should fall back to any array when specific key not found")
    void shouldFallBackToAnyArrayWhenSpecificKeyNotFound() {
        // Given
        String conversationContent = "<result>{\"integers\": [7, 8, 9]}</result>";

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(3)
                    .containsExactly(7, 8, 9);
            });
    }

    @Test
    @DisplayName("Should find array even with wrong key as fallback")
    void shouldFindArrayEvenWithWrongKeyAsFallback() {
        // Given
        String conversationContent =
            "<result>RESULT</result>\n" +
            "<result>{\"integers\": [10, 20, 30]}</result>";

        // When
        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        // Then
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(list -> {
                assertThat(list)
                    .hasSize(3)
                    .containsExactly(10, 20, 30);
            });
    }
}
