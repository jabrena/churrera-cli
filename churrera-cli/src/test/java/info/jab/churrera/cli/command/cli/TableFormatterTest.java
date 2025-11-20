package info.jab.churrera.cli.command.cli;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for TableFormatter utility class.
 */
class TableFormatterTest {

    @Test
    void shouldThrowExceptionWhenInstantiating() {
        // When & Then
        assertThatThrownBy(() -> {
            var constructor = TableFormatter.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
            .hasCauseInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getCause)
            .extracting(Throwable::getMessage)
            .isEqualTo("Utility class cannot be instantiated");
    }

    @Test
    void shouldFormatSimpleTable() {
        // Given
        String[] headers = {"Name", "Age", "City"};
        List<String[]> rows = Arrays.asList(
            new String[]{"John", "25", "New York"},
            new String[]{"Jane", "30", "London"},
            new String[]{"Bob", "35", "Paris"}
        );

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("Name");
        assertThat(result).contains("Age");
        assertThat(result).contains("City");
        assertThat(result).contains("John");
        assertThat(result).contains("Jane");
        assertThat(result).contains("Bob");
        assertThat(result).contains("|"); // Table separators
        assertThat(result).contains("+"); // Header separators
    }

    @Test
    void shouldFormatEmptyTable() {
        // Given
        String[] headers = {"Name", "Age"};
        List<String[]> rows = Arrays.asList();

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("Name");
        assertThat(result).contains("Age");
        assertThat(result).contains("|");
    }

    @Test
    void shouldFormatTimestamp() {
        // Given
        LocalDateTime timestamp = LocalDateTime.of(2024, 10, 11, 14, 30);

        // When
        String result = TableFormatter.formatTimestamp(timestamp);

        // Then
        assertThat(result).isEqualTo("2024-10-11 14:30");
    }

    @Test
    void shouldFormatNullTimestamp() {
        // When
        String result = TableFormatter.formatTimestamp(null);

        // Then
        assertThat(result).isEqualTo("N/A");
    }

    @Test
    void shouldTruncateLongText() {
        // Given
        String longText = "This is a very long text that should be truncated";

        // When
        String result = TableFormatter.truncateText(longText, 20);

        // Then
        assertThat(result).hasSize(20);
        assertThat(result).endsWith("...");
        assertThat(result).isEqualTo("This is a very lo...");
    }

    @Test
    void shouldNotTruncateShortText() {
        // Given
        String shortText = "Short";

        // When
        String result = TableFormatter.truncateText(shortText, 20);

        // Then
        assertThat(result).isEqualTo("Short");
    }

    @Test
    void shouldHandleNullText() {
        // When
        String result = TableFormatter.truncateText(null, 20);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFormatTableWithNullHeaders() {
        // Given
        String[] headers = null;
        String[] row = {"John", "25", "New York"};
        List<String[]> rows = new ArrayList<>();
        rows.add(row);

        // When & Then
        assertThatThrownBy(() -> TableFormatter.formatTable(headers, rows))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Headers cannot be null or empty");
    }

    @Test
    void shouldFormatTableWithEmptyHeaders() {
        // Given
        String[] headers = {};
        String[] row = {"John", "25", "New York"};
        List<String[]> rows = new ArrayList<>();
        rows.add(row);

        // When & Then
        assertThatThrownBy(() -> TableFormatter.formatTable(headers, rows))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Headers cannot be null or empty");
    }

    @Test
    void shouldFormatTableWithNullRows() {
        // Given
        String[] headers = {"Name", "Age", "City"};

        // When
        String result = TableFormatter.formatTable(headers, null);

        // Then
        assertThat(result).contains("Name");
        assertThat(result).contains("Age");
        assertThat(result).contains("City");
        assertThat(result).contains("|");
    }

    @Test
    void shouldFormatTableWithUnevenRows() {
        // Given
        String[] headers = {"Name", "Age", "City", "Country"};
        List<String[]> rows = Arrays.asList(
            new String[]{"John", "25"}, // Missing columns
            new String[]{"Jane", "30", "London", "UK", "Extra"}, // Extra column
            new String[]{"Bob"} // Only one column
        );

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("John");
        assertThat(result).contains("Jane");
        assertThat(result).contains("Bob");
        assertThat(result).contains("|");
    }

    @Test
    void shouldFormatTableWithNullCells() {
        // Given
        String[] headers = {"Name", "Age", "City"};
        List<String[]> rows = Arrays.asList(
            new String[]{"John", null, "New York"},
            new String[]{null, "30", null}
        );

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("John");
        assertThat(result).contains("30");
        assertThat(result).contains("New York");
        assertThat(result).contains("|");
    }

    @Test
    void shouldFormatTableWithVeryLongHeaders() {
        // Given
        String[] headers = {"Very Long Header Name That Exceeds Maximum Width", "Age", "City"};
        String[] row = {"John", "25", "New York"};
        List<String[]> rows = new ArrayList<>();
        rows.add(row);

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("John");
        assertThat(result).contains("25");
        assertThat(result).contains("New York");
        assertThat(result).contains("|");
    }

    @Test
    void shouldFormatTableWithVeryLongData() {
        // Given
        String[] headers = {"Name", "Description", "City"};
        List<String[]> rows = Arrays.<String[]>asList(
            new String[]{"John", "This is a very long description that should be truncated because it exceeds the maximum column width", "New York"}
        );

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("John");
        assertThat(result).contains("New York");
        assertThat(result).contains("|");
    }

    @Test
    void shouldTruncateTextWithMinimumWidth() {
        // Given
        String text = "Hello";

        // When
        String result = TableFormatter.truncateText(text, 3);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).isEqualTo("Hel");
    }

    @Test
    void shouldTruncateTextWithVerySmallWidth() {
        // Given
        String text = "Hello";

        // When
        String result = TableFormatter.truncateText(text, 1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo("H");
    }

    @Test
    void shouldTruncateTextWithZeroWidth() {
        // Given
        String text = "Hello";

        // When
        String result = TableFormatter.truncateText(text, 0);

        // Then
        assertThat(result).hasSize(0);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldTruncateTextWithExactWidth() {
        // Given
        String text = "Hello";

        // When
        String result = TableFormatter.truncateText(text, 5);

        // Then
        assertThat(result).hasSize(5);
        assertThat(result).isEqualTo("Hello");
    }

    @Test
    void shouldTruncateTextWithWidthOneLessThanText() {
        // Given
        String text = "Hello";

        // When
        String result = TableFormatter.truncateText(text, 4);

        // Then
        assertThat(result).hasSize(4);
        assertThat(result).isEqualTo("H...");
    }

    @Test
    void shouldFormatTimestampWithDifferentFormats() {
        // Given
        LocalDateTime timestamp1 = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime timestamp2 = LocalDateTime.of(2024, 12, 31, 23, 59);
        LocalDateTime timestamp3 = LocalDateTime.of(2024, 6, 15, 12, 30);

        // When
        String result1 = TableFormatter.formatTimestamp(timestamp1);
        String result2 = TableFormatter.formatTimestamp(timestamp2);
        String result3 = TableFormatter.formatTimestamp(timestamp3);

        // Then
        assertThat(result1).isEqualTo("2024-01-01 00:00");
        assertThat(result2).isEqualTo("2024-12-31 23:59");
        assertThat(result3).isEqualTo("2024-06-15 12:30");
    }

    @Test
    void shouldFormatTableWithSpecialCharacters() {
        // Given
        String[] headers = {"Name", "Special Chars"};
        List<String[]> rows = Arrays.asList(
            new String[]{"John & Jane", "Special: <>&\"'"},
            new String[]{"Bob's", "Test & More"}
        );

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("John & Jane");
        assertThat(result).contains("Special: <>&\"'");
        assertThat(result).contains("Bob's");
        assertThat(result).contains("Test & More");
        assertThat(result).contains("|");
    }

    @Test
    void shouldFormatTableWithEmptyStrings() {
        // Given
        String[] headers = {"Name", "Age", "City"};
        List<String[]> rows = Arrays.asList(
            new String[]{"", "25", ""},
            new String[]{"Jane", "", "London"}
        );

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("Jane");
        assertThat(result).contains("25");
        assertThat(result).contains("London");
        assertThat(result).contains("|");
    }

    @Test
    void shouldFormatTableWithSingleColumn() {
        // Given
        String[] headers = {"Name"};
        List<String[]> rows = Arrays.asList(
            new String[]{"John"},
            new String[]{"Jane"},
            new String[]{"Bob"}
        );

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("Name");
        assertThat(result).contains("John");
        assertThat(result).contains("Jane");
        assertThat(result).contains("Bob");
        assertThat(result).contains("|");
    }

    @Test
    void shouldFormatTableWithManyColumns() {
        // Given
        String[] headers = {"Col1", "Col2", "Col3", "Col4", "Col5", "Col6", "Col7", "Col8", "Col9", "Col10"};
        String[] row = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        List<String[]> rows = new ArrayList<>();
        rows.add(row);

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("Col1");
        assertThat(result).contains("Col10");
        assertThat(result).contains("1");
        assertThat(result).contains("10");
        assertThat(result).contains("|");
    }

    @Test
    void shouldFormatTableWithManyRows() {
        // Given
        String[] headers = {"Name", "Age"};
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            rows.add(new String[]{"Person" + i, String.valueOf(20 + i)});
        }

        // When
        String result = TableFormatter.formatTable(headers, rows);

        // Then
        assertThat(result).contains("Name");
        assertThat(result).contains("Age");
        assertThat(result).contains("Person1");
        assertThat(result).contains("Person100");
        assertThat(result).contains("21");
        assertThat(result).contains("120");
        assertThat(result).contains("|");
    }
}

