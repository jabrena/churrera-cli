package info.jab.churrera.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TimeoutParser.
 */
class TimeoutParserTest {

    @Test
    void testParseToMillis_NullInput() {
        // When
        Long result = TimeoutParser.parseToMillis(null);

        // Then
        assertNull(result);
    }

    @Test
    void testParseToMillis_EmptyString() {
        // When
        Long result = TimeoutParser.parseToMillis("");

        // Then
        assertNull(result);
    }

    @Test
    void testParseToMillis_WhitespaceOnly() {
        // When
        Long result = TimeoutParser.parseToMillis("   ");

        // Then
        assertNull(result);
    }

    @Test
    void testParseToMillis_ValidMinutes_Lowercase() {
        // When
        Long result = TimeoutParser.parseToMillis("5m");

        // Then
        assertNotNull(result);
        assertEquals(5L * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_ValidMinutes_Uppercase() {
        // When
        Long result = TimeoutParser.parseToMillis("5M");

        // Then
        assertNotNull(result);
        assertEquals(5L * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_ValidMinutes_WithWhitespace() {
        // When
        Long result = TimeoutParser.parseToMillis("  5m  ");

        // Then
        assertNotNull(result);
        assertEquals(5L * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_ValidMinutes_LargeNumber() {
        // When
        Long result = TimeoutParser.parseToMillis("30m");

        // Then
        assertNotNull(result);
        assertEquals(30L * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_ValidHours_Lowercase() {
        // When
        Long result = TimeoutParser.parseToMillis("1h");

        // Then
        assertNotNull(result);
        assertEquals(1L * 60 * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_ValidHours_Uppercase() {
        // When
        Long result = TimeoutParser.parseToMillis("1H");

        // Then
        assertNotNull(result);
        assertEquals(1L * 60 * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_ValidHours_LargeNumber() {
        // When
        Long result = TimeoutParser.parseToMillis("24h");

        // Then
        assertNotNull(result);
        assertEquals(24L * 60 * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_ValidHours_WithWhitespace() {
        // When
        Long result = TimeoutParser.parseToMillis("  2h  ");

        // Then
        assertNotNull(result);
        assertEquals(2L * 60 * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_InvalidFormat_TooShort() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("m")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
        assertTrue(exception.getMessage().contains("Expected format: number + unit (m or h)"));
    }

    @Test
    void testParseToMillis_InvalidFormat_SingleCharacter() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("h")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
    }

    @Test
    void testParseToMillis_InvalidFormat_InvalidUnit() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("5s")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
        assertTrue(exception.getMessage().contains("Unit must be 'm' (minutes) or 'h' (hours)"));
    }

    @Test
    void testParseToMillis_InvalidFormat_InvalidNumber() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("abcm")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
        assertTrue(exception.getMessage().contains("is not a valid number"));
    }

    @Test
    void testParseToMillis_InvalidFormat_ZeroMinutes() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("0m")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
        assertTrue(exception.getMessage().contains("Number must be greater than 0"));
    }

    @Test
    void testParseToMillis_InvalidFormat_NegativeMinutes() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("-5m")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
        assertTrue(exception.getMessage().contains("Number must be greater than 0"));
    }

    @Test
    void testParseToMillis_InvalidFormat_ZeroHours() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("0h")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
        assertTrue(exception.getMessage().contains("Number must be greater than 0"));
    }

    @Test
    void testParseToMillis_EdgeCase_OneMinute() {
        // When
        Long result = TimeoutParser.parseToMillis("1m");

        // Then
        assertNotNull(result);
        assertEquals(60 * 1000, result);
    }

    @Test
    void testParseToMillis_EdgeCase_OneHour() {
        // When
        Long result = TimeoutParser.parseToMillis("1h");

        // Then
        assertNotNull(result);
        assertEquals(60 * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_EdgeCase_LargeMinutes() {
        // When
        Long result = TimeoutParser.parseToMillis("999m");

        // Then
        assertNotNull(result);
        assertEquals(999L * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_EdgeCase_LargeHours() {
        // When
        Long result = TimeoutParser.parseToMillis("999h");

        // Then
        assertNotNull(result);
        assertEquals(999L * 60 * 60 * 1000, result);
    }

    @Test
    void testParseToMillis_InvalidFormat_OnlyNumber() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("5")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
    }

    @Test
    void testParseToMillis_InvalidFormat_OnlyUnit() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("m")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
    }

    @Test
    void testParseToMillis_InvalidFormat_MultipleUnits() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("5mm")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
        assertTrue(exception.getMessage().contains("is not a valid number"));
    }

    @Test
    void testParseToMillis_InvalidFormat_DecimalNumber() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TimeoutParser.parseToMillis("5.5m")
        );

        assertTrue(exception.getMessage().contains("Invalid timeout format"));
        assertTrue(exception.getMessage().contains("is not a valid number"));
    }
}

