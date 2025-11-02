package info.jab.churrera.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BindResultTypeMapper.
 */
class BindResultTypeMapperTest {

    @Test
    void testMapToElementType_ListInteger() {
        // When
        Class<?> result = BindResultTypeMapper.mapToElementType("List_Integer");

        // Then
        assertNotNull(result);
        assertEquals(Integer.class, result);
    }

    @Test
    void testMapToElementType_UnsupportedType() {
        // When & Then
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> BindResultTypeMapper.mapToElementType("List_String")
        );

        assertTrue(exception.getMessage().contains("Unsupported bindResultType: List_String"));
        assertTrue(exception.getMessage().contains("Currently supported types: List_Integer"));
    }

    @Test
    void testMapToElementType_InvalidFormat() {
        // When & Then
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> BindResultTypeMapper.mapToElementType("InvalidFormat")
        );

        assertTrue(exception.getMessage().contains("Unsupported bindResultType: InvalidFormat"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testMapToElementType_NullOrEmpty(String input) {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> BindResultTypeMapper.mapToElementType(input)
        );

        assertEquals("bindResultType cannot be null or empty", exception.getMessage());
    }

    @Test
    void testMapToElementType_WhitespaceOnly() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> BindResultTypeMapper.mapToElementType("   ")
        );

        assertEquals("bindResultType cannot be null or empty", exception.getMessage());
    }

    @Test
    void testIsListType_ValidListType() {
        // When
        boolean result = BindResultTypeMapper.isListType("List_Integer");

        // Then
        assertTrue(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"List_String", "List_Long", "List_Custom", "List_"})
    void testIsListType_VariousListTypes(String input) {
        // When
        boolean result = BindResultTypeMapper.isListType(input);

        // Then
        assertTrue(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Integer", "String", "Map_Integer", "Set_String", ""})
    void testIsListType_NonListTypes(String input) {
        // When
        boolean result = BindResultTypeMapper.isListType(input);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsListType_Null() {
        // When
        boolean result = BindResultTypeMapper.isListType(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testConstructor_ThrowsException() {
        // When & Then
        var exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> {
                // Use reflection to access private constructor
                var constructor = BindResultTypeMapper.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            }
        );

        // Verify the cause is UnsupportedOperationException
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }
}

