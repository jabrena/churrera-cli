package info.jab.churrera.workflow;

/**
 * Utility class to map bindResultType strings to Java types for deserialization.
 * Supports mapping of type strings like "List_Integer" to Java type representations.
 */
public final class BindResultTypeMapper {

    private BindResultTypeMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Maps a bindResultType string to a Java Class type.
     * Currently supports:
     * - "List_Integer" -> Integer.class (for use with ConversationJsonDeserializer.deserializeList)
     *
     * @param bindResultType the type string from XML (e.g., "List_Integer")
     * @return the Java Class for list elements
     * @throws UnsupportedOperationException if the type is not supported
     */
    public static Class<?> mapToElementType(String bindResultType) {
        if (bindResultType == null || bindResultType.trim().isEmpty()) {
            throw new IllegalArgumentException("bindResultType cannot be null or empty");
        }

        return switch (bindResultType) {
            case "List_Integer" -> Integer.class;
            default -> throw new UnsupportedOperationException(
                "Unsupported bindResultType: " + bindResultType + ". " +
                "Currently supported types: List_Integer"
            );
        };
    }

    /**
     * Checks if a bindResultType represents a List type.
     *
     * @param bindResultType the type string to check
     * @return true if it's a List type, false otherwise
     */
    public static boolean isListType(String bindResultType) {
        return bindResultType != null && bindResultType.startsWith("List_");
    }
}

