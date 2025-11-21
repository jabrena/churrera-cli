package info.jab.churrera.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for deserializing JSON strings from conversation content into the specified type.
 * This class extracts JSON content from &lt;result&gt; tags and deserializes it.
 */
public final class ConversationJsonDeserializer {

    private static final Logger logger = LoggerFactory.getLogger(ConversationJsonDeserializer.class);

    /**
     * Regular expression pattern to match content within &lt;result&gt; tags.
     * Uses DOTALL flag to match across multiple lines and captures the content.
     */
    private static final Pattern RESULT_PATTERN = Pattern.compile(
        "<result>(.*?)</result>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * Jackson ObjectMapper for JSON deserialization.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ConversationJsonDeserializer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Extracts JSON content from &lt;result&gt; tags and deserializes it into the specified type.
     *
     * @param conversationContent the conversation content containing &lt;result&gt; tags with JSON
     * @param targetType the target type class
     * @param <T> the type parameter
     * @return Optional containing the deserialized object, or empty if not found or deserialization fails
     * @throws IllegalArgumentException if conversationContent is null or targetType is null
     */
    public static <T> Optional<T> deserialize(String conversationContent, Class<T> targetType) {
        if (conversationContent == null) {
            throw new IllegalArgumentException("Conversation content cannot be null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Target type cannot be null");
        }

        Optional<String> jsonContent = extractJsonFromResult(conversationContent);
        if (!jsonContent.isPresent()) {
            return Optional.empty();
        }

        return deserializeJson(jsonContent.get(), targetType);
    }

    /**
     * Extracts JSON content from &lt;result&gt; tags and deserializes it into a list of the specified type.
     *
     * @param conversationContent the conversation content containing &lt;result&gt; tags with JSON
     * @param targetType the target type class for list elements
     * @param <T> the type parameter
     * @return Optional containing the deserialized list, or empty if not found or deserialization fails
     * @throws IllegalArgumentException if conversationContent is null or targetType is null
     */
    public static <T> Optional<List<T>> deserializeList(String conversationContent, Class<T> targetType) {
        return deserializeList(conversationContent, targetType, null);
    }

    /**
     * Extracts JSON content from &lt;result&gt; tags and deserializes it into a list of the specified type.
     * If a preferred key is provided, it will first try to find an array with that key name.
     *
     * @param conversationContent the conversation content containing &lt;result&gt; tags with JSON
     * @param targetType the target type class for list elements
     * @param preferredKey the preferred key name to look for in the JSON object (e.g., "List_Integer")
     * @param <T> the type parameter
     * @return Optional containing the deserialized list, or empty if not found or deserialization fails
     * @throws IllegalArgumentException if conversationContent is null or targetType is null
     */
    public static <T> Optional<List<T>> deserializeList(String conversationContent, Class<T> targetType, String preferredKey) {
        if (conversationContent == null) {
            throw new IllegalArgumentException("Conversation content cannot be null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Target type cannot be null");
        }

        Optional<String> jsonContent = extractJsonFromResult(conversationContent);
        if (!jsonContent.isPresent()) {
            return Optional.empty();
        }

        return deserializeJsonList(jsonContent.get(), targetType, preferredKey);
    }

    /**
     * Extracts JSON content from the last &lt;result&gt; tag found in the conversation content.
     * This method finds the last occurrence to avoid matching instruction text.
     *
     * @param conversationContent the conversation content containing &lt;result&gt; tags
     * @return Optional containing the extracted JSON content, or empty if not found
     */
    private static Optional<String> extractJsonFromResult(String conversationContent) {
        Matcher matcher = RESULT_PATTERN.matcher(conversationContent);
        String lastMatch = null;
        int matchCount = 0;

        // Find all matches and keep the last one
        while (matcher.find()) {
            lastMatch = matcher.group(1).trim();
            matchCount++;
        }

        if (lastMatch != null && !lastMatch.isEmpty()) {
            logger.info("Found {} result tag(s), using last match with length: {}", matchCount, lastMatch.length());
            logger.info("Extracted JSON content: {}", lastMatch);
            return Optional.of(lastMatch);
        }

        logger.error("No <result> tags found in conversation content (length: {} chars)",
            conversationContent.length());
        if (conversationContent.length() > 0) {
            int previewLength = Math.min(500, conversationContent.length());
            logger.error("Conversation content preview (first {} chars): {}",
                previewLength, conversationContent.substring(0, previewLength));
        }
        return Optional.empty();
    }

    /**
     * Deserializes a JSON string into the specified type.
     *
     * @param jsonContent the JSON content to deserialize
     * @param targetType the target type class
     * @param <T> the type parameter
     * @return Optional containing the deserialized object, or empty if deserialization fails
     */
    private static <T> Optional<T> deserializeJson(String jsonContent, Class<T> targetType) {
        try {
            T result = OBJECT_MAPPER.readValue(jsonContent, targetType);
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Deserializes a JSON string into a list of the specified type.
     * Supports two formats:
     * 1. Direct array: [1, 2, 3]
     * 2. Object with array property: {"key": [1, 2, 3]}
     *
     * @param jsonContent the JSON content to deserialize
     * @param targetType the target type class for list elements
     * @param preferredKey the preferred key name to look for (optional, can be null)
     * @param <T> the type parameter
     * @return Optional containing the deserialized list, or empty if deserialization fails
     */
    private static <T> Optional<List<T>> deserializeJsonList(String jsonContent, Class<T> targetType, String preferredKey) {
        try {
            logger.info("Attempting to parse JSON content for list deserialization (length: {})", jsonContent.length());
            logger.info("JSON content to deserialize: {}", jsonContent);
            if (preferredKey != null) {
                logger.info("Preferred key for deserialization: {}", preferredKey);
            }
            // First, try to determine if it's a direct array or an object
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonContent);

            if (rootNode.isArray()) {
                // Direct array case: [1, 2, 3]
                logger.info("Detected direct array format");
                return deserializeDirectArray(rootNode, targetType);
            } else if (rootNode.isObject()) {
                // Object with array property case: {"key": [1, 2, 3]}
                logger.info("Detected object wrapper format, searching for array");
                return deserializeObjectWithArray(rootNode, targetType, preferredKey);
            }

            logger.error("JSON root node is neither array nor object: {}", rootNode.getNodeType());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to deserialize JSON list: {}", e.getMessage(), e);
            logger.error("Failed JSON content: {}", jsonContent);
            return Optional.empty();
        }
    }

    /**
     * Deserializes a direct JSON array node into a list of the specified type.
     *
     * @param arrayNode the JSON array node
     * @param targetType the target type class for list elements
     * @param <T> the type parameter
     * @return Optional containing the deserialized list
     */
    private static <T> Optional<List<T>> deserializeDirectArray(
            com.fasterxml.jackson.databind.JsonNode arrayNode, Class<T> targetType) {
        try {
            List<T> result = new ArrayList<>();

            for (JsonNode element : arrayNode) {
                T item = OBJECT_MAPPER.treeToValue(element, targetType);
                result.add(item);
            }

            logger.info("Successfully deserialized {} elements from direct array", result.size());
            return Optional.of(result);
        } catch (Exception e) {
            logger.error("Failed to deserialize direct array: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Deserializes a JSON object containing an array property into a list.
     * If a preferred key is provided, it will first try to find an array with that key name.
     * Otherwise, it searches for the first array property in the object (or nested objects).
     *
     * @param objectNode the JSON object node
     * @param targetType the target type class for list elements
     * @param preferredKey the preferred key name to look for (optional, can be null)
     * @param <T> the type parameter
     * @return Optional containing the deserialized list from the array found
     */
    private static <T> Optional<List<T>> deserializeObjectWithArray(
            JsonNode objectNode, Class<T> targetType, String preferredKey) {
        try {
            JsonNode arrayNode = null;

            // First, try to find array with the preferred key if specified
            if (preferredKey != null && objectNode.has(preferredKey)) {
                JsonNode keyNode = objectNode.get(preferredKey);
                if (keyNode != null && keyNode.isArray()) {
                    arrayNode = keyNode;
                    logger.info("Found array with preferred key '{}' containing {} elements", preferredKey, arrayNode.size());
                }
            }

            // If preferred key not found or not specified, fall back to finding first array
            if (arrayNode == null) {
                if (preferredKey != null) {
                    logger.info("Preferred key '{}' not found, falling back to finding first array", preferredKey);
                }
                arrayNode = findFirstArray(objectNode);
            }

            if (arrayNode != null && arrayNode.isArray()) {
                logger.info("Found array in object wrapper with {} elements", arrayNode.size());
                return deserializeDirectArray(arrayNode, targetType);
            }

            logger.error("No array found in object wrapper");
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to deserialize object with array: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Recursively searches for the first array node in a JSON structure.
     *
     * @param node the JSON node to search
     * @return the first array node found, or null if none found
     */
    private static JsonNode findFirstArray(JsonNode node) {
        if (node == null) {
            return null;
        }

        if (node.isArray()) {
            return node;
        }

        if (node.isObject()) {
            // Check all fields
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                JsonNode child = elements.next();
                JsonNode result = findFirstArray(child);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }
}
