package info.jab.churrera.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

/**
 * Test class for PropertyResolver utility.
 */
@DisplayName("PropertyResolver Tests")
public class PropertyResolverTest {

    @Nested
    @DisplayName("getProperty Tests")
    class GetPropertyTests {

        @Test
        @DisplayName("Should get string property")
        public void shouldGetStringProperty() {
            // Given
            PropertyResolver propertyResolver = new PropertyResolver();

            // When
            Optional<String> modelProperty =
                propertyResolver.getProperty("application.properties", "model");

            // Then
            assertTrue(modelProperty.isPresent());
            assertNotNull(modelProperty.get());
            assertFalse(modelProperty.get().isEmpty());
        }

        @Test
        @DisplayName("Should get numeric property as string")
        public void shouldGetNumericPropertyAsString() {
            // Given
            PropertyResolver propertyResolver = new PropertyResolver();

            // When
            Optional<String> delayProperty =
                propertyResolver.getProperty("application.properties", "delay");

            // Then
            assertTrue(delayProperty.isPresent());
            assertNotNull(delayProperty.get());
            assertDoesNotThrow(() -> Integer.parseInt(delayProperty.get()));
        }

        @Test
        @DisplayName("Should return empty Optional for non-existent property")
        public void shouldReturnEmptyOptionalForNonExistentProperty() {
            // Given
            PropertyResolver propertyResolver = new PropertyResolver();

            // When
            Optional<String> nonExistentProperty =
                propertyResolver.getProperty("application.properties", "nonExistent");

            // Then
            assertFalse(nonExistentProperty.isPresent());
        }

        @Test
        @DisplayName("Should return empty Optional for null resource path")
        public void shouldReturnEmptyOptionalForNullResourcePath() {
            // Given
            PropertyResolver propertyResolver = new PropertyResolver();

            // When
            Optional<String> result =
                propertyResolver.getProperty(null, "model");

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty Optional for null key")
        public void shouldReturnEmptyOptionalForNullKey() {
            // Given
            PropertyResolver propertyResolver = new PropertyResolver();

            // When
            Optional<String> result =
                propertyResolver.getProperty("application.properties", null);

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty Optional for empty resource path")
        public void shouldReturnEmptyOptionalForEmptyResourcePath() {
            // Given
            PropertyResolver propertyResolver = new PropertyResolver();

            // When
            Optional<String> result =
                propertyResolver.getProperty("", "model");

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty Optional for empty key")
        public void shouldReturnEmptyOptionalForEmptyKey() {
            // Given
            PropertyResolver propertyResolver = new PropertyResolver();

            // When
            Optional<String> result =
                propertyResolver.getProperty("application.properties", "");

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty Optional for non-existent resource")
        public void shouldReturnEmptyOptionalForNonExistentResource() {
            // Given
            PropertyResolver propertyResolver = new PropertyResolver();

            // When
            Optional<String> result =
                propertyResolver.getProperty("non-existent.properties", "key");

            // Then
            assertFalse(result.isPresent());
        }
    }
}
