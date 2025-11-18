package info.jab.churrera.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for BindResultTypeMapper.
 */
@DisplayName("BindResultTypeMapper Tests")
class BindResultTypeMapperTest {

    @Nested
    @DisplayName("MapToElementType Tests")
    class MapToElementTypeTests {

        @Test
        @DisplayName("Should map List_Integer to Integer class")
        void shouldMapListIntegerToIntegerClass() {
            // When
            Class<?> result = BindResultTypeMapper.mapToElementType("List_Integer");

            // Then
            assertThat(result)
                .isNotNull()
                .isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("Should throw exception for unsupported type")
        void shouldThrowExceptionForUnsupportedType() {
            // When & Then
            assertThatThrownBy(() -> BindResultTypeMapper.mapToElementType("List_String"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unsupported bindResultType: List_String")
                .hasMessageContaining("Currently supported types: List_Integer");
        }

        @Test
        @DisplayName("Should throw exception for invalid format")
        void shouldThrowExceptionForInvalidFormat() {
            // When & Then
            assertThatThrownBy(() -> BindResultTypeMapper.mapToElementType("InvalidFormat"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unsupported bindResultType: InvalidFormat");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should throw exception for null or empty input")
        void shouldThrowExceptionForNullOrEmptyInput(String input) {
            // When & Then
            assertThatThrownBy(() -> BindResultTypeMapper.mapToElementType(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bindResultType cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for whitespace only input")
        void shouldThrowExceptionForWhitespaceOnlyInput() {
            // When & Then
            assertThatThrownBy(() -> BindResultTypeMapper.mapToElementType("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bindResultType cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("IsListType Tests")
    class IsListTypeTests {

        @Test
        @DisplayName("Should return true for valid list type")
        void shouldReturnTrueForValidListType() {
            // When
            boolean result = BindResultTypeMapper.isListType("List_Integer");

            // Then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"List_String", "List_Long", "List_Custom", "List_"})
        @DisplayName("Should return true for various list types")
        void shouldReturnTrueForVariousListTypes(String input) {
            // When
            boolean result = BindResultTypeMapper.isListType(input);

            // Then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Integer", "String", "Map_Integer", "Set_String", ""})
        @DisplayName("Should return false for non-list types")
        void shouldReturnFalseForNonListTypes(String input) {
            // When
            boolean result = BindResultTypeMapper.isListType(input);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null input")
        void shouldReturnFalseForNullInput() {
            // When
            boolean result = BindResultTypeMapper.isListType(null);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw exception when trying to instantiate utility class")
        void shouldThrowExceptionWhenTryingToInstantiateUtilityClass() {
            // When & Then
            assertThatThrownBy(() -> {
                // Use reflection to access private constructor
                var constructor = BindResultTypeMapper.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            })
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(UnsupportedOperationException.class)
                .extracting(Throwable::getMessage)
                .isEqualTo("Utility class cannot be instantiated");
        }
    }
}
