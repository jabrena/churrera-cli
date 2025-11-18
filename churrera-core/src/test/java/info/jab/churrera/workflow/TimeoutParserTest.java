package info.jab.churrera.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TimeoutParser.
 */
@DisplayName("TimeoutParser Tests")
class TimeoutParserTest {

    @Nested
    @DisplayName("Null and Empty Input Tests")
    class NullAndEmptyInputTests {

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            // When
            Long result = TimeoutParser.parseToMillis(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for empty string")
        void shouldReturnNullForEmptyString() {
            // When
            Long result = TimeoutParser.parseToMillis("");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for whitespace only")
        void shouldReturnNullForWhitespaceOnly() {
            // When
            Long result = TimeoutParser.parseToMillis("   ");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Valid Minutes Tests")
    class ValidMinutesTests {

        @Test
        @DisplayName("Should parse minutes with lowercase unit")
        void shouldParseMinutesWithLowercaseUnit() {
            // When
            Long result = TimeoutParser.parseToMillis("5m");

            // Then
            assertThat(result).isEqualTo(5L * 60 * 1000);
        }

        @Test
        @DisplayName("Should parse minutes with uppercase unit")
        void shouldParseMinutesWithUppercaseUnit() {
            // When
            Long result = TimeoutParser.parseToMillis("5M");

            // Then
            assertThat(result).isEqualTo(5L * 60 * 1000);
        }

        @Test
        @DisplayName("Should parse minutes with whitespace")
        void shouldParseMinutesWithWhitespace() {
            // When
            Long result = TimeoutParser.parseToMillis("  5m  ");

            // Then
            assertThat(result).isEqualTo(5L * 60 * 1000);
        }

        @Test
        @DisplayName("Should parse large number of minutes")
        void shouldParseLargeNumberOfMinutes() {
            // When
            Long result = TimeoutParser.parseToMillis("30m");

            // Then
            assertThat(result).isEqualTo(30L * 60 * 1000);
        }

        @Test
        @DisplayName("Should parse one minute")
        void shouldParseOneMinute() {
            // When
            Long result = TimeoutParser.parseToMillis("1m");

            // Then
            assertThat(result).isEqualTo(60 * 1000);
        }

        @Test
        @DisplayName("Should parse very large number of minutes")
        void shouldParseVeryLargeNumberOfMinutes() {
            // When
            Long result = TimeoutParser.parseToMillis("999m");

            // Then
            assertThat(result).isEqualTo(999L * 60 * 1000);
        }
    }

    @Nested
    @DisplayName("Valid Hours Tests")
    class ValidHoursTests {

        @Test
        @DisplayName("Should parse hours with lowercase unit")
        void shouldParseHoursWithLowercaseUnit() {
            // When
            Long result = TimeoutParser.parseToMillis("1h");

            // Then
            assertThat(result).isEqualTo(1L * 60 * 60 * 1000);
        }

        @Test
        @DisplayName("Should parse hours with uppercase unit")
        void shouldParseHoursWithUppercaseUnit() {
            // When
            Long result = TimeoutParser.parseToMillis("1H");

            // Then
            assertThat(result).isEqualTo(1L * 60 * 60 * 1000);
        }

        @Test
        @DisplayName("Should parse large number of hours")
        void shouldParseLargeNumberOfHours() {
            // When
            Long result = TimeoutParser.parseToMillis("24h");

            // Then
            assertThat(result).isEqualTo(24L * 60 * 60 * 1000);
        }

        @Test
        @DisplayName("Should parse hours with whitespace")
        void shouldParseHoursWithWhitespace() {
            // When
            Long result = TimeoutParser.parseToMillis("  2h  ");

            // Then
            assertThat(result).isEqualTo(2L * 60 * 60 * 1000);
        }

        @Test
        @DisplayName("Should parse one hour")
        void shouldParseOneHour() {
            // When
            Long result = TimeoutParser.parseToMillis("1h");

            // Then
            assertThat(result).isEqualTo(60 * 60 * 1000);
        }

        @Test
        @DisplayName("Should parse very large number of hours")
        void shouldParseVeryLargeNumberOfHours() {
            // When
            Long result = TimeoutParser.parseToMillis("999h");

            // Then
            assertThat(result).isEqualTo(999L * 60 * 60 * 1000);
        }
    }

    @Nested
    @DisplayName("Invalid Format Tests")
    class InvalidFormatTests {

        @Test
        @DisplayName("Should throw exception for too short input")
        void shouldThrowExceptionForTooShortInput() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format")
                .hasMessageContaining("Expected format: number + unit (m or h)");
        }

        @Test
        @DisplayName("Should throw exception for single character")
        void shouldThrowExceptionForSingleCharacter() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("h"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format");
        }

        @Test
        @DisplayName("Should throw exception for invalid unit")
        void shouldThrowExceptionForInvalidUnit() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("5s"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format")
                .hasMessageContaining("Unit must be 'm' (minutes) or 'h' (hours)");
        }

        @Test
        @DisplayName("Should throw exception for invalid number")
        void shouldThrowExceptionForInvalidNumber() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("abcm"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format")
                .hasMessageContaining("is not a valid number");
        }

        @Test
        @DisplayName("Should throw exception for zero minutes")
        void shouldThrowExceptionForZeroMinutes() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("0m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format")
                .hasMessageContaining("Number must be greater than 0");
        }

        @Test
        @DisplayName("Should throw exception for negative minutes")
        void shouldThrowExceptionForNegativeMinutes() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("-5m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format")
                .hasMessageContaining("Number must be greater than 0");
        }

        @Test
        @DisplayName("Should throw exception for zero hours")
        void shouldThrowExceptionForZeroHours() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("0h"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format")
                .hasMessageContaining("Number must be greater than 0");
        }

        @Test
        @DisplayName("Should throw exception for only number")
        void shouldThrowExceptionForOnlyNumber() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format");
        }

        @Test
        @DisplayName("Should throw exception for only unit")
        void shouldThrowExceptionForOnlyUnit() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format");
        }

        @Test
        @DisplayName("Should throw exception for multiple units")
        void shouldThrowExceptionForMultipleUnits() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("5mm"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format")
                .hasMessageContaining("is not a valid number");
        }

        @Test
        @DisplayName("Should throw exception for decimal number")
        void shouldThrowExceptionForDecimalNumber() {
            // When & Then
            assertThatThrownBy(() -> TimeoutParser.parseToMillis("5.5m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timeout format")
                .hasMessageContaining("is not a valid number");
        }
    }
}
