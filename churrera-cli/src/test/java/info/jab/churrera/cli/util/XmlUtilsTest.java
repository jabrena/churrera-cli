package info.jab.churrera.cli.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for XmlUtils.
 */
@DisplayName("XmlUtils Tests")
class XmlUtilsTest {

    @Nested
    @DisplayName("EscapeXml Tests")
    class EscapeXmlTests {

        @Test
        @DisplayName("Should escape XML special characters")
        void shouldEscapeXmlSpecialCharacters() {
            // Given
            String text = "Hello <world> & \"friends\" 'test'";

            // When
            String escaped = XmlUtils.escapeXml(text);

            // Then
            assertThat(escaped).isEqualTo("Hello &lt;world&gt; &amp; &quot;friends&quot; &apos;test&apos;");
        }

        @Test
        @DisplayName("Should return empty string when text is null")
        void shouldReturnEmptyStringWhenTextIsNull() {
            // When
            String escaped = XmlUtils.escapeXml(null);

            // Then
            assertThat(escaped).isEmpty();
        }

        @Test
        @DisplayName("Should not escape normal text")
        void shouldNotEscapeNormalText() {
            // Given
            String text = "Hello World";

            // When
            String escaped = XmlUtils.escapeXml(text);

            // Then
            assertThat(escaped).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Should escape empty string")
        void shouldEscapeEmptyString() {
            // Given
            String text = "";

            // When
            String escaped = XmlUtils.escapeXml(text);

            // Then
            assertThat(escaped).isEmpty();
        }
    }

    @Nested
    @DisplayName("UnescapeXml Tests")
    class UnescapeXmlTests {

        @Test
        @DisplayName("Should unescape XML special characters")
        void shouldUnescapeXmlSpecialCharacters() {
            // Given
            String escaped = "Hello &lt;world&gt; &amp; &quot;friends&quot; &apos;test&apos;";

            // When
            String unescaped = XmlUtils.unescapeXml(escaped);

            // Then
            assertThat(unescaped).isEqualTo("Hello <world> & \"friends\" 'test'");
        }

        @Test
        @DisplayName("Should return null when text is null")
        void shouldReturnNullWhenTextIsNull() {
            // When
            String unescaped = XmlUtils.unescapeXml(null);

            // Then
            assertThat(unescaped).isNull();
        }

        @Test
        @DisplayName("Should not unescape normal text")
        void shouldNotUnescapeNormalText() {
            // Given
            String text = "Hello World";

            // When
            String unescaped = XmlUtils.unescapeXml(text);

            // Then
            assertThat(unescaped).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Should unescape empty string")
        void shouldUnescapeEmptyString() {
            // Given
            String text = "";

            // When
            String unescaped = XmlUtils.unescapeXml(text);

            // Then
            assertThat(unescaped).isEmpty();
        }
    }

    @Nested
    @DisplayName("ExtractXmlValue Tests")
    class ExtractXmlValueTests {

        @Test
        @DisplayName("Should extract XML value")
        void shouldExtractXmlValue() {
            // Given
            String xml = "<root><name>John</name><age>30</age></root>";

            // When
            String name = XmlUtils.extractXmlValue(xml, "name");
            String age = XmlUtils.extractXmlValue(xml, "age");

            // Then
            assertThat(name).isEqualTo("John");
            assertThat(age).isEqualTo("30");
        }

        @Test
        @DisplayName("Should throw exception when start tag is not found")
        void shouldThrowExceptionWhenStartTagNotFound() {
            // Given
            String xml = "<root><name>John</name></root>";

            // When & Then
            assertThatThrownBy(() -> XmlUtils.extractXmlValue(xml, "missing"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start tag not found");
        }

        @Test
        @DisplayName("Should throw exception when end tag is not found")
        void shouldThrowExceptionWhenEndTagNotFound() {
            // Given
            String xml = "<root><name>John</root>";

            // When & Then
            assertThatThrownBy(() -> XmlUtils.extractXmlValue(xml, "name"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End tag not found");
        }

        @Test
        @DisplayName("Should handle XML with escaped characters")
        void shouldHandleXmlWithEscapedCharacters() {
            // Given
            String xml = "<root><text>Hello &lt;world&gt;</text></root>";

            // When
            String text = XmlUtils.extractXmlValue(xml, "text");

            // Then - XML values should be unescaped when extracted
            assertThat(text).isEqualTo("Hello <world>");
        }

        @Test
        @DisplayName("Should extract empty XML value")
        void shouldExtractEmptyXmlValue() {
            // Given
            String xml = "<root><empty></empty></root>";

            // When
            String value = XmlUtils.extractXmlValue(xml, "empty");

            // Then
            assertThat(value).isEmpty();
        }
    }

    @Nested
    @DisplayName("ExtractXmlValueOptional Tests")
    class ExtractXmlValueOptionalTests {

        @Test
        @DisplayName("Should extract XML value optionally")
        void shouldExtractXmlValueOptional() {
            // Given
            String xml = "<root><name>John</name><age>30</age></root>";

            // When
            String name = XmlUtils.extractXmlValueOptional(xml, "name");
            String age = XmlUtils.extractXmlValueOptional(xml, "age");
            String missing = XmlUtils.extractXmlValueOptional(xml, "missing");

            // Then
            assertThat(name).isEqualTo("John");
            assertThat(age).isEqualTo("30");
            assertThat(missing).isNull();
        }

        @Test
        @DisplayName("Should return null when optional tag is not found")
        void shouldReturnNullWhenOptionalTagNotFound() {
            // Given
            String xml = "<root><name>John</name></root>";

            // When
            String result = XmlUtils.extractXmlValueOptional(xml, "missing");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null when end tag is missing for optional extraction")
        void shouldReturnNullWhenEndTagMissingForOptionalExtraction() {
            // Given
            String xml = "<root><name>John</root>";

            // When
            String result = XmlUtils.extractXmlValueOptional(xml, "name");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should extract empty XML value optionally")
        void shouldExtractEmptyXmlValueOptional() {
            // Given
            String xml = "<root><empty></empty></root>";

            // When
            String value = XmlUtils.extractXmlValueOptional(xml, "empty");

            // Then
            assertThat(value).isEmpty();
        }
    }
}

