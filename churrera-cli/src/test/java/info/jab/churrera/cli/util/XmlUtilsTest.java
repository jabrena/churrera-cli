package info.jab.churrera.cli.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for XmlUtils.
 */
class XmlUtilsTest {

    @Test
    void shouldEscapeXmlSpecialCharacters() {
        // Given
        String text = "Hello <world> & \"friends\" 'test'";

        // When
        String escaped = XmlUtils.escapeXml(text);

        // Then
        assertThat(escaped).isEqualTo("Hello &lt;world&gt; &amp; &quot;friends&quot; &apos;test&apos;");
    }

    @Test
    void shouldReturnEmptyStringWhenTextIsNull() {
        // When
        String escaped = XmlUtils.escapeXml(null);

        // Then
        assertThat(escaped).isEmpty();
    }

    @Test
    void shouldNotEscapeNormalText() {
        // Given
        String text = "Hello World";

        // When
        String escaped = XmlUtils.escapeXml(text);

        // Then
        assertThat(escaped).isEqualTo("Hello World");
    }

    @Test
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
    void shouldThrowExceptionWhenStartTagNotFound() {
        // Given
        String xml = "<root><name>John</name></root>";

        // When/Then
        assertThatThrownBy(() -> XmlUtils.extractXmlValue(xml, "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start tag not found");
    }

    @Test
    void shouldThrowExceptionWhenEndTagNotFound() {
        // Given
        String xml = "<root><name>John</root>";

        // When/Then
        assertThatThrownBy(() -> XmlUtils.extractXmlValue(xml, "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End tag not found");
    }

    @Test
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
    void shouldReturnNullWhenOptionalTagNotFound() {
        // Given
        String xml = "<root><name>John</name></root>";

        // When
        String result = XmlUtils.extractXmlValueOptional(xml, "missing");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldHandleXmlWithEscapedCharacters() {
        // Given
        String xml = "<root><text>Hello &lt;world&gt;</text></root>";

        // When
        String text = XmlUtils.extractXmlValue(xml, "text");

        // Then - XML values should be unescaped when extracted
        assertThat(text).isEqualTo("Hello <world>");
    }
}

