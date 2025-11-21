package info.jab.churrera.workflow;

/**
 * Utility class for parsing timeout strings to milliseconds.
 * Supports formats like "5m" (5 minutes) and "1h" (1 hour).
 */
public class TimeoutParser {

    private TimeoutParser() {
        // Utility class - prevent instantiation
    }

    /**
     * Parses a timeout string to milliseconds.
     * Format: number + unit where unit is 'm' (minutes) or 'h' (hours).
     * Examples: "5m" = 5 minutes, "1h" = 1 hour, "30m" = 30 minutes
     *
     * @param timeoutStr the timeout string (e.g., "5m", "1h")
     * @return the timeout in milliseconds, or null if input is null or empty
     * @throws IllegalArgumentException if the format is invalid
     */
    public static Long parseToMillis(String timeoutStr) {
        if (timeoutStr == null || timeoutStr.trim().isEmpty()) {
            return null;
        }

        String trimmed = timeoutStr.trim();
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException("Invalid timeout format: '" + timeoutStr + "'. Expected format: number + unit (m or h)");
        }

        // Get the last character as the unit
        char unit = trimmed.charAt(trimmed.length() - 1);
        String numberStr = trimmed.substring(0, trimmed.length() - 1);

        // Parse the number part
        long number;
        try {
            number = Long.parseLong(numberStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid timeout format: '" + timeoutStr + "'. Number part '" + numberStr + "' is not a valid number");
        }

        if (number <= 0) {
            throw new IllegalArgumentException("Invalid timeout format: '" + timeoutStr + "'. Number must be greater than 0");
        }

        // Convert to milliseconds based on unit
        long milliseconds;
        if (unit == 'm' || unit == 'M') {
            milliseconds = number * 60 * 1000; // minutes to milliseconds
        } else if (unit == 'h' || unit == 'H') {
            milliseconds = number * 60 * 60 * 1000; // hours to milliseconds
        } else {
            throw new IllegalArgumentException("Invalid timeout format: '" + timeoutStr + "'. Unit must be 'm' (minutes) or 'h' (hours), got: '" + unit + "'");
        }

        return milliseconds;
    }
}

