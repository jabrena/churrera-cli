package info.jab.churrera.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for PmlConverter class.
 */
@DisplayName("PmlConverter Tests")
class PmlConverterTest {

    private PmlConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PmlConverter();
    }

    @Nested
    @DisplayName("toMarkdownFromContent Tests")
    class ToMarkdownFromContentTests {

        @Test
        @DisplayName("Should convert PML content to Markdown")
        void shouldConvertPmlContentToMarkdown() {
            // Given
            String pmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <prompt>
                    <role>You are a helpful assistant</role>
                    <goal>Help the user with their task</goal>
                    <instructions>
                        <steps>
                            <step>
                                <step-title>Step 1</step-title>
                                <step-content>Do something useful</step-content>
                            </step>
                        </steps>
                    </instructions>
                </prompt>
                """;

            // When
            String result = converter.toMarkdownFromContent(pmlContent);

            // Then
            assertThat(result)
                .isNotNull()
                .contains("## Role")
                .contains("You are a helpful assistant")
                .contains("## Goal")
                .contains("Help the user with their task")
                .contains("## Instructions")
                .contains("### Step Step 1")
                .contains("Do something useful");
        }

        @Test
        @DisplayName("Should throw exception for null content")
        void shouldThrowExceptionForNullContent() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdownFromContent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML content cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for empty content")
        void shouldThrowExceptionForEmptyContent() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdownFromContent(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML content cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for whitespace-only content")
        void shouldThrowExceptionForWhitespaceOnlyContent() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdownFromContent("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML content cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for tab-only content")
        void shouldThrowExceptionForTabOnlyContent() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdownFromContent("\t\t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML content cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for newline-only content")
        void shouldThrowExceptionForNewlineOnlyContent() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdownFromContent("\n\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML content cannot be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @NullSource
        @DisplayName("Should throw exception for invalid XSLT file path")
        void shouldThrowExceptionForInvalidXsltFilePath(String xsltFilePath) {
            // Given
            String pmlContent = "<prompt><role>Test</role></prompt>";

            // When & Then
            assertThatThrownBy(() -> converter.toMarkdownFromContent(pmlContent, xsltFilePath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XSLT file path cannot be null or empty");
        }

        @Test
        @DisplayName("Should use default XSLT file")
        void shouldUseDefaultXsltFile() {
            // Given
            String pmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <prompt>
                    <role>Test role</role>
                </prompt>
                """;

            // When
            String result = converter.toMarkdownFromContent(pmlContent);

            // Then
            assertThat(result)
                .isNotNull()
                .contains("## Role")
                .contains("Test role");
        }

        @Test
        @DisplayName("Should convert with custom XSLT file")
        void shouldConvertWithCustomXsltFile() {
            // Given
            String pmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <prompt>
                    <role>Test role</role>
                    <goal>Test goal</goal>
                </prompt>
                """;

            // When
            String result = converter.toMarkdownFromContent(pmlContent, "pml/pml-to-md.xsl");

            // Then
            assertThat(result)
                .isNotNull()
                .contains("## Role")
                .contains("Test role")
                .contains("## Goal")
                .contains("Test goal");
        }
    }

    @Nested
    @DisplayName("toMarkdown(String) Tests")
    class ToMarkdownSingleParamTests {

        @Test
        @DisplayName("Should convert PML file to Markdown using default XSLT")
        void shouldConvertPmlFileToMarkdownUsingDefaultXslt() {
            // Given
            String pmlFile = "examples/hello-world/prompt1.xml";

            // When
            String result = converter.toMarkdown(pmlFile);

            // Then
            assertThat(result)
                .isNotNull()
                .contains("## Role")
                .contains("## Goal");
        }

        @Test
        @DisplayName("Should throw exception for null PML file")
        void shouldThrowExceptionForNullPmlFile() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML file path cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for empty PML file path")
        void shouldThrowExceptionForEmptyPmlFilePath() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML file path cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for whitespace-only PML file path")
        void shouldThrowExceptionForWhitespaceOnlyPmlFilePath() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML file path cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("toMarkdown(String, String) Tests")
    class ToMarkdownTwoParamTests {

        @Test
        @DisplayName("Should convert PML file to Markdown with custom XSLT")
        void shouldConvertPmlFileToMarkdownWithCustomXslt() {
            // Given
            String pmlFile = "examples/hello-world/prompt1.xml";
            String xsltFile = "pml/pml-to-md.xsl";

            // When
            String result = converter.toMarkdown(pmlFile, xsltFile);

            // Then
            assertThat(result)
                .isNotNull()
                .contains("## Role")
                .contains("## Goal")
                .contains("## Output Format")
                .contains("## Safeguards");
        }

        @Test
        @DisplayName("Should throw exception for null PML file")
        void shouldThrowExceptionForNullPmlFile() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown(null, "pml/pml-to-md.xsl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML file path cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for empty PML file path")
        void shouldThrowExceptionForEmptyPmlFilePath() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown("", "pml/pml-to-md.xsl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML file path cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for whitespace-only PML file path")
        void shouldThrowExceptionForWhitespaceOnlyPmlFilePath() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown("   ", "pml/pml-to-md.xsl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PML file path cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for null XSLT file")
        void shouldThrowExceptionForNullXsltFile() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown("examples/hello-world/prompt1.xml", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XSLT file path cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for empty XSLT file path")
        void shouldThrowExceptionForEmptyXsltFilePath() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown("examples/hello-world/prompt1.xml", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XSLT file path cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for whitespace-only XSLT file path")
        void shouldThrowExceptionForWhitespaceOnlyXsltFilePath() {
            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown("examples/hello-world/prompt1.xml", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XSLT file path cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw RuntimeException for non-existent PML file")
        void shouldThrowRuntimeExceptionForNonExistentPmlFile() {
            // Given
            String nonExistentFile = "non-existent-file.xml";

            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown(nonExistentFile, "pml/pml-to-md.xsl"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to convert PML to Markdown");
        }

        @Test
        @DisplayName("Should throw RuntimeException for non-existent XSLT file")
        void shouldThrowRuntimeExceptionForNonExistentXsltFile() {
            // Given
            String nonExistentXslt = "non-existent-xslt.xsl";

            // When & Then
            assertThatThrownBy(() -> converter.toMarkdown("examples/hello-world/prompt1.xml", nonExistentXslt))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to convert PML to Markdown");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance with default constructor")
        void shouldCreateInstanceWithDefaultConstructor() {
            // When
            PmlConverter instance = new PmlConverter();

            // Then
            assertThat(instance).isNotNull();
        }

        @Test
        @DisplayName("Should create instance with ClasspathResolver parameter")
        void shouldCreateInstanceWithClasspathResolverParameter() {
            // Given
            ClasspathResolver resolver = new ClasspathResolver();

            // When
            PmlConverter instance = new PmlConverter(resolver);

            // Then
            assertThat(instance).isNotNull();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when ClasspathResolver is null")
        void shouldThrowIllegalArgumentExceptionWhenClasspathResolverIsNull() {
            // When & Then
            assertThatThrownBy(() -> new PmlConverter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ClasspathResolver cannot be null");
        }
    }
}
