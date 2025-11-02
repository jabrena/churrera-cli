package info.jab.churrera.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Test class for ConversationJsonDeserializer.
 */
class ConversationJsonDeserializerTest {

    @Test
    void testDeserializeListWithDirectArray() {
        // Test with direct array format: [1, 2, 3, 4]
        String conversationContent = "Some conversation text\n<result>[1, 2, 3, 4]</result>\nMore text";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        assertTrue(result.isPresent(), "Should successfully deserialize direct array");
        assertEquals(4, result.get().size(), "Should have 4 elements");
        assertEquals(List.of(1, 2, 3, 4), result.get(), "Should contain correct integers");
    }

    @Test
    void testDeserializeListWithObjectWrapper() {
        // Test with object wrapper format: {"integers": [1, 2, 3, 4]}
        String conversationContent = "Some conversation text\n<result>{\"integers\": [1, 2, 3, 4]}</result>\nMore text";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        assertTrue(result.isPresent(), "Should successfully deserialize object with array property");
        assertEquals(4, result.get().size(), "Should have 4 elements");
        assertEquals(List.of(1, 2, 3, 4), result.get(), "Should contain correct integers");
    }

    @Test
    void testDeserializeListWithRealConversationSample() throws IOException {
        // Load the real conversation sample from resources using classpath
        java.io.InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream("conversation-result-sample.txt");
        assertNotNull(inputStream, "Resource file should exist");
        String conversationContent = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        assertTrue(result.isPresent(), "Should successfully deserialize real conversation sample");
        assertEquals(4, result.get().size(), "Should have 4 elements");
        assertEquals(List.of(1, 2, 3, 4), result.get(), "Should contain correct integers");
    }

    @Test
    void testDeserializeListWithComplexObjectWrapper() {
        // Test with nested object structure
        String conversationContent = "<result>{\"data\": {\"values\": [5, 10, 15]}}</result>";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        assertTrue(result.isPresent(), "Should successfully deserialize nested array");
        assertEquals(3, result.get().size(), "Should have 3 elements");
        assertEquals(List.of(5, 10, 15), result.get(), "Should contain correct integers");
    }

    @Test
    void testDeserializeListWithAPILikeConversationFormat() {
        // Test with format similar to how getConversationContent() concatenates API messages
        // Each message is on a new line
        String conversationContent =
            "Let me help you with that task.\n" +
            "I'll process the data now.\n" +
            "<result>{\"numbers\": [10, 20, 30, 40]}</result>\n" +
            "The task is complete.\n";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        assertTrue(result.isPresent(), "Should successfully deserialize from API-like format");
        assertEquals(4, result.get().size(), "Should have 4 elements");
        assertEquals(List.of(10, 20, 30, 40), result.get(), "Should contain correct integers");
    }

    @Test
    void testDeserializeListWithNoResultTag() {
        String conversationContent = "Just some conversation text without result tags";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        assertFalse(result.isPresent(), "Should return empty when no result tag found");
    }

    @Test
    void testDeserializeListWithEmptyResult() {
        String conversationContent = "<result></result>";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        assertFalse(result.isPresent(), "Should return empty when result tag is empty");
    }

    @Test
    void testDeserializeListWithInvalidJson() {
        String conversationContent = "<result>{invalid json}</result>";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        assertFalse(result.isPresent(), "Should return empty when JSON is invalid");
    }

    @Test
    void testDeserializeListWithMultipleResultTags() {
        // Should use the last result tag
        String conversationContent =
            "<result>[1, 2]</result>\n" +
            "Some text\n" +
            "<result>[3, 4, 5]</result>";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(conversationContent, Integer.class);

        assertTrue(result.isPresent(), "Should successfully deserialize last result");
        assertEquals(List.of(3, 4, 5), result.get(), "Should use the last result tag");
    }

    @Test
    void testDeserializeWithNullConversationContent() {
        assertThrows(IllegalArgumentException.class,
            () -> ConversationJsonDeserializer.deserializeList(null, Integer.class),
            "Should throw IllegalArgumentException for null conversation content");
    }

    @Test
    void testDeserializeWithNullTargetType() {
        assertThrows(IllegalArgumentException.class,
            () -> ConversationJsonDeserializer.deserializeList("<result>[1, 2]</result>", null),
            "Should throw IllegalArgumentException for null target type");
    }

    @Test
    void testDeserializeListWithStrings() {
        String conversationContent = "<result>[\"apple\", \"banana\", \"cherry\"]</result>";

        Optional<List<String>> result = ConversationJsonDeserializer.deserializeList(conversationContent, String.class);

        assertTrue(result.isPresent(), "Should successfully deserialize string array");
        assertEquals(3, result.get().size(), "Should have 3 elements");
        assertEquals(List.of("apple", "banana", "cherry"), result.get(), "Should contain correct strings");
    }

    @Test
    void testDeserializeListWithObjectWrapperAndStrings() {
        String conversationContent = "<result>{\"names\": [\"Alice\", \"Bob\", \"Charlie\"]}</result>";

        Optional<List<String>> result = ConversationJsonDeserializer.deserializeList(conversationContent, String.class);

        assertTrue(result.isPresent(), "Should successfully deserialize object with string array");
        assertEquals(3, result.get().size(), "Should have 3 elements");
        assertEquals(List.of("Alice", "Bob", "Charlie"), result.get(), "Should contain correct strings");
    }

    @Test
    void testDeserializeListWithSpecificKeyMatching() {
        // Test with specific key matching bindResultType
        String conversationContent = "<result>{\"List_Integer\": [1, 2, 3, 4]}</result>";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        assertTrue(result.isPresent(), "Should successfully deserialize with specific key");
        assertEquals(4, result.get().size(), "Should have 4 elements");
        assertEquals(List.of(1, 2, 3, 4), result.get(), "Should contain correct integers");
    }

    @Test
    void testDeserializeListWithMultipleResultTagsAndSpecificKey() {
        // Simulates real scenario: first <result> has template, second has actual data
        String conversationContent =
            "Here's the format:\n" +
            "<result>RESULT</result>\n" +
            "Now processing...\n" +
            "<result>{\"List_Integer\": [1, 2, 3, 4]}</result>";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        assertTrue(result.isPresent(), "Should successfully deserialize last result with specific key");
        assertEquals(4, result.get().size(), "Should have 4 elements");
        assertEquals(List.of(1, 2, 3, 4), result.get(), "Should contain correct integers");
    }

    @Test
    void testDeserializeListWithRealScenarioFromLogs() {
        // This is based on the actual conversation from logs
        String conversationContent =
            "## Role\n\n" +
            "You are a Senior software engineer\n\n" +
            "## Output Format\n\n" +
            "- Replace in the following xml fragment the RESULT with the actual List:\n" +
            "<result>RESULT</result>\n" +
            "- Only return the JSON format\n\n" +
            "I'll help you scrape the homework numbers. Let me start:\n\n" +
            "Now let me access the course page:\n\n" +
            "Perfect! I can see the homework assignments. Let me extract the first 4:\n\n" +
            "<result>{\"List_Integer\":[1,2,3,4]}</result>\n";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        assertTrue(result.isPresent(), "Should successfully deserialize real scenario");
        assertEquals(4, result.get().size(), "Should have 4 elements");
        assertEquals(List.of(1, 2, 3, 4), result.get(), "Should contain correct integers");
    }

    @Test
    void testDeserializeListWithSpecificKeyFallbackToAnyArray() {
        // When specific key not found, should fall back to first array found
        String conversationContent = "<result>{\"integers\": [7, 8, 9]}</result>";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        assertTrue(result.isPresent(), "Should fall back to any array when specific key not found");
        assertEquals(3, result.get().size(), "Should have 3 elements");
        assertEquals(List.of(7, 8, 9), result.get(), "Should contain correct integers");
    }

    @Test
    void testDeserializeListWithWrongKeyButCorrectData() {
        // Even if key doesn't match, should still find the array as fallback
        String conversationContent =
            "<result>RESULT</result>\n" +
            "<result>{\"integers\": [10, 20, 30]}</result>";

        Optional<List<Integer>> result = ConversationJsonDeserializer.deserializeList(
            conversationContent, Integer.class, "List_Integer");

        assertTrue(result.isPresent(), "Should find array even with wrong key as fallback");
        assertEquals(3, result.get().size(), "Should have 3 elements");
        assertEquals(List.of(10, 20, 30), result.get(), "Should contain correct integers");
    }
}

