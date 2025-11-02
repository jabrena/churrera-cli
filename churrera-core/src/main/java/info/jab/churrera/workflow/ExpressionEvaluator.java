package info.jab.churrera.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates expressions in prompts and replaces placeholders with actual values.
 * Supports simple expression evaluation for parallel job execution.
 */
public final class ExpressionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(ExpressionEvaluator.class);

    private ExpressionEvaluator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Evaluates a simple $get() expression by returning the provided value.
     * This is used in parallel workflows where each iteration gets a value from a list.
     *
     * @param expression the expression to evaluate (e.g., "$get()")
     * @param value the value to return for this iteration
     * @return the value as a string, or null if expression is not supported
     */
    public static String evaluate(String expression, Object value) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        if ("$get()".equals(expression.trim())) {
            return value != null ? value.toString() : "";
        }

        // Future expressions can be added here:
        // - $get(index)
        // - $get().field
        // - etc.

        return null; // Unsupported expression
    }

    /**
     * Replaces INPUT placeholder in prompt content with the actual value.
     * This replaces occurrences of <input>INPUT</input> with the provided value.
     *
     * @param promptContent the prompt content (markdown or PML)
     * @param value the value to replace INPUT with
     * @return the prompt content with INPUT replaced
     */
    public static String replaceInputPlaceholder(String promptContent, String value) {
        if (promptContent == null) {
            logger.warn("replaceInputPlaceholder: promptContent is null");
            return null;
        }
        if (value == null) {
            value = "";
        }

        String pattern = "<input>INPUT</input>";
        boolean containsPattern = promptContent.contains(pattern);
        
        logger.info("🔍 Pattern search: looking for '{}' in content (length: {})", pattern, promptContent.length());
        logger.info("🔍 Pattern found: {}", containsPattern);
        
        if (containsPattern) {
            String result = promptContent.replace(pattern, "<input>" + value + "</input>");
            logger.info("✅ Replacement applied: INPUT -> '{}' (content changed: {})", 
                value, !result.equals(promptContent));
            return result;
        } else {
            logger.warn("⚠️ Pattern '{}' NOT FOUND in content", pattern);
            logger.warn("⚠️ Content preview (first 300 chars): {}", 
                promptContent.length() > 300 ? promptContent.substring(0, 300) + "..." : promptContent);
            // Replace <input>INPUT</input> with the actual value anyway (in case it's there but not detected)
            return promptContent.replace(pattern, "<input>" + value + "</input>");
        }
    }

    /**
     * Checks if an expression is supported.
     *
     * @param expression the expression to check
     * @return true if the expression is supported, false otherwise
     */
    public static boolean isSupported(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        return "$get()".equals(expression.trim());
        // Future expressions can be added to this check
    }
}

