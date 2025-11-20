package info.jab.churrera.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for validating XSL transformations on PML XML files.
 * This class tests that XML files can be successfully processed by XSL transformations
 * and produce expected output.
 */
@DisplayName("XSL Transformation Tests")
class XslTransformationTest {

    private TransformerFactory transformerFactory;
    private ClasspathResolver resolver;

    @BeforeEach
    void setUp() {
        transformerFactory = TransformerFactory.newInstance();
        resolver = new ClasspathResolver();
    }

    @Nested
    @DisplayName("Basic XSL Transformation Tests")
    class BasicTransformationTests {

        @Test
        @DisplayName("Should successfully transform prompt1.xml to markdown")
        void shouldSuccessfullyTransformPrompt1XmlToMarkdown() throws Exception {
            // Given
            String pmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When
            String result = performTransformation(pmlContent, xsltContent);

            // Then
            assertThat(result)
                .isNotNull()
                .isNotEmpty();
            assertThat(result.trim())
                .isNotEmpty();

            // Verify key sections are present in the output
            assertThat(result)
                .contains("## Role")
                .contains("## Goal")
                .contains("## Output Format")
                .contains("## Safeguards");
        }

        @Test
        @DisplayName("Should handle XSL transformation without throwing exceptions")
        void shouldHandleXslTransformationWithoutThrowingExceptions() {
            // Given
            String pmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertThatCode(() -> performTransformation(pmlContent, xsltContent))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should produce consistent transformation results")
        void shouldProduceConsistentTransformationResults() throws Exception {
            // Given
            String pmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When
            String result1 = performTransformation(pmlContent, xsltContent);
            String result2 = performTransformation(pmlContent, xsltContent);

            // Then
            assertThat(result1).isEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("XSL Content Validation Tests")
    class XslContentValidationTests {

        @Test
        @DisplayName("Should verify XSL file contains required templates")
        void shouldVerifyXslFileContainsRequiredTemplates() throws Exception {
            // Given
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertThat(xsltContent)
                .contains("<xsl:template match=\"/prompt\">")
                .contains("<xsl:template match=\"goal\">")
                .contains("<xsl:template match=\"output-format\">")
                .contains("<xsl:template match=\"safeguards\">")
                .contains("<xsl:template match=\"examples\">");
        }

        @Test
        @DisplayName("Should verify XSL file contains required utility templates")
        void shouldVerifyXslFileContainsRequiredUtilityTemplates() throws Exception {
            // Given
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertThat(xsltContent)
                .contains("<xsl:template name=\"trim-code-block\">")
                .contains("<xsl:template name=\"remove-trailing-spaces\">")
                .contains("<xsl:template name=\"preserve-indentation\">");
        }

        @Test
        @DisplayName("Should verify XSL file has correct output method")
        void shouldVerifyXslFileHasCorrectOutputMethod() throws Exception {
            // Given
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertThat(xsltContent)
                .contains("<xsl:output method=\"text\" encoding=\"UTF-8\"/>");
        }
    }

    @Nested
    @DisplayName("Transformation Output Validation Tests")
    class TransformationOutputValidationTests {

        @Test
        @DisplayName("Should verify transformation output contains expected role content")
        void shouldVerifyTransformationOutputContainsExpectedRoleContent() throws Exception {
            // Given
            String pmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When
            String result = performTransformation(pmlContent, xsltContent);

            // Then
            assertThat(result)
                .contains("System Administrator with expertise in Java development");
        }

        @Test
        @DisplayName("Should verify transformation output contains expected goal content")
        void shouldVerifyTransformationOutputContainsExpectedGoalContent() throws Exception {
            // Given
            String pmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When
            String result = performTransformation(pmlContent, xsltContent);

            // Then
            assertThat(result)
                .contains("Update the VM to Java 25")
                .contains("sudo apt install -y openjdk-25-jdk");
        }

        @Test
        @DisplayName("Should verify transformation output contains expected output format items")
        void shouldVerifyTransformationOutputContainsExpectedOutputFormatItems() throws Exception {
            // Given
            String pmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When
            String result = performTransformation(pmlContent, xsltContent);

            // Then
            assertThat(result)
                .contains("- not invest time in planning")
                .contains("- only install the component with the given command")
                .contains("- not explain anything");
        }

        @Test
        @DisplayName("Should verify transformation output contains expected safeguards")
        void shouldVerifyTransformationOutputContainsExpectedSafeguards() throws Exception {
            // Given
            String pmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When
            String result = performTransformation(pmlContent, xsltContent);

            // Then
            assertThat(result)
                .contains("- verify that java is configured for java 25 executing `java -version`")
                .contains("- if the java installation and the verification is successful, then the goal is achieved");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed XML gracefully")
        void shouldHandleMalformedXmlGracefully() {
            // Given
            String malformedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<prompt>\n" +
                    "    <role>Test role</role>\n" +
                    "    <!-- Missing closing tag -->\n" +
                    "    <goal>Test goal\n";
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertThatThrownBy(() -> performTransformation(malformedXml, xsltContent))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should handle malformed XSL gracefully")
        void shouldHandleMalformedXslGracefully() {
            // Given
            String pmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String malformedXsl = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                    "    <!-- Missing closing tag -->\n" +
                    "    <xsl:template match=\"/prompt\">\n" +
                    "        <xsl:value-of select=\"role\"/>\n";

            // When & Then
            assertThatThrownBy(() -> performTransformation(pmlContent, malformedXsl))
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("PmlConverter Integration Tests")
    class PmlConverterIntegrationTests {

        @Test
        @DisplayName("Should work with PmlConverter.toMarkdown method")
        void shouldWorkWithPmlConverterToMarkdownMethod() {
            // Given
            String pmlFile = "examples/hello-world/prompt1.xml";

            // When & Then
            assertThatCode(() -> {
                PmlConverter converter = new PmlConverter();
                String result = converter.toMarkdown(pmlFile);
                assertThat(result)
                    .isNotNull()
                    .isNotEmpty();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should produce same result as direct transformation")
        void shouldProduceSameResultAsDirectTransformation() throws Exception {
            // Given
            String pmlFile = "examples/hello-world/prompt1.xml";
            String pmlContent = resolver.retrieve(pmlFile);
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When
            String directResult = performTransformation(pmlContent, xsltContent);
            PmlConverter converter = new PmlConverter();
            String converterResult = converter.toMarkdown(pmlFile);

            // Then
            assertThat(converterResult).isEqualTo(directResult);
        }
    }

    /**
     * Performs XSL transformation on the given XML content using the provided XSL content.
     *
     * @param xmlContent the XML content to transform
     * @param xslContent the XSL content to use for transformation
     * @return the transformation result as a String
     * @throws Exception if transformation fails
     */
    private String performTransformation(String xmlContent, String xslContent) throws Exception {
        try (InputStream xmlStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
             InputStream xslStream = new ByteArrayInputStream(xslContent.getBytes(StandardCharsets.UTF_8))) {

            Transformer transformer = transformerFactory.newTransformer(new StreamSource(xslStream));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            transformer.transform(new StreamSource(xmlStream), new StreamResult(outputStream));

            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }
}
