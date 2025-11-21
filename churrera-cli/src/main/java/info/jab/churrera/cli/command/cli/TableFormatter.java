package info.jab.churrera.cli.command.cli;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for formatting data as ASCII tables.
 */
public final class TableFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MIN_COLUMN_WIDTH = 3;
    private static final int MAX_COLUMN_WIDTH = 50;

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TableFormatter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Formats a list of rows as an ASCII table.
     *
     * @param headers the column headers
     * @param rows the data rows
     * @return formatted ASCII table as String
     */
    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            throw new IllegalArgumentException("Headers cannot be null or empty");
        }
        if (rows == null) {
            rows = new ArrayList<>();
        }

        // Calculate column widths
        int[] columnWidths = calculateColumnWidths(headers, rows);

        StringBuilder result = new StringBuilder();

        // Print header
        result.append(formatRow(headers, columnWidths)).append("\n");
        result.append(formatSeparator(columnWidths)).append("\n");

        // Print data rows
        for (String[] row : rows) {
            result.append(formatRow(row, columnWidths)).append("\n");
        }

        return result.toString();
    }

    /**
     * Formats a timestamp for display in tables.
     *
     * @param timestamp the timestamp to format
     * @return formatted timestamp string
     */
    public static String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        return timestamp.format(TIMESTAMP_FORMATTER);
    }

    /**
     * Truncates text to fit within the specified width.
     *
     * @param text the text to truncate
     * @param maxWidth the maximum width
     * @return truncated text
     */
    public static String truncateText(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxWidth) {
            return text;
        }
        if (maxWidth <= 3) {
            return text.substring(0, maxWidth);
        }
        return text.substring(0, maxWidth - 3) + "...";
    }

    /**
     * Calculates the optimal column widths based on headers and data.
     */
    private static int[] calculateColumnWidths(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];

        // Initialize with header widths
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i] != null ? headers[i].length() : 0;
        }

        // Update with data widths
        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, widths.length); i++) {
                if (row[i] != null) {
                    widths[i] = Math.max(widths[i], row[i].length());
                }
            }
        }

        // Apply minimum and maximum constraints
        for (int i = 0; i < widths.length; i++) {
            widths[i] = Math.clamp(widths[i], MIN_COLUMN_WIDTH, MAX_COLUMN_WIDTH);
        }

        return widths;
    }

    /**
     * Formats a single row with proper spacing.
     */
    private static String formatRow(String[] row, int[] columnWidths) {
        StringBuilder result = new StringBuilder();
        result.append("|");

        for (int i = 0; i < columnWidths.length; i++) {
            String cell = i < row.length && row[i] != null ? row[i] : "";
            String truncated = truncateText(cell, columnWidths[i]);
            StringBuilder padded = new StringBuilder(truncated);
            padded.append(" ".repeat(Math.max(0, columnWidths[i] - truncated.length())));
            result.append(" ").append(padded).append(" |");
        }

        return result.toString();
    }

    /**
     * Formats the separator line between header and data.
     */
    private static String formatSeparator(int[] columnWidths) {
        StringBuilder result = new StringBuilder();
        result.append("+");

        for (int width : columnWidths) {
            result.append("-".repeat(width + 2)).append("+");
        }

        return result.toString();
    }
}

