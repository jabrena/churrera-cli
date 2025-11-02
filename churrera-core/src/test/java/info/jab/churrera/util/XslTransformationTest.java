package info.jab.churrera.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.EnabledIf;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

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
            assertNotNull(result, "Transformation result should not be null");
            assertFalse(result.trim().isEmpty(), "Transformation result should not be empty");

            // Verify key sections are present in the output
            assertTrue(result.contains("## Role"), "Result should contain Role section");
            assertTrue(result.contains("## Goal"), "Result should contain Goal section");
            assertTrue(result.contains("## Output Format"), "Result should contain Output Format section");
            assertTrue(result.contains("## Safeguards"), "Result should contain Safeguards section");
        }

        @Test
        @DisplayName("Should handle XSL transformation without throwing exceptions")
        void shouldHandleXslTransformationWithoutThrowingExceptions() {
            // Given
            String pmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertDoesNotThrow(() -> performTransformation(pmlContent, xsltContent),
                "XSL transformation should complete without exceptions");
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
            assertEquals(result1, result2, "Multiple transformations should produce identical results");
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
            assertTrue(xsltContent.contains("<xsl:template match=\"/prompt\">"),
                "XSL should contain root prompt template");
            assertTrue(xsltContent.contains("<xsl:template match=\"goal\">"),
                "XSL should contain goal template");
            assertTrue(xsltContent.contains("<xsl:template match=\"output-format\">"),
                "XSL should contain output-format template");
            assertTrue(xsltContent.contains("<xsl:template match=\"safeguards\">"),
                "XSL should contain safeguards template");
            assertTrue(xsltContent.contains("<xsl:template match=\"examples\">"),
                "XSL should contain examples template");
        }

        @Test
        @DisplayName("Should verify XSL file contains required utility templates")
        void shouldVerifyXslFileContainsRequiredUtilityTemplates() throws Exception {
            // Given
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertTrue(xsltContent.contains("<xsl:template name=\"trim-code-block\">"),
                "XSL should contain trim-code-block utility template");
            assertTrue(xsltContent.contains("<xsl:template name=\"remove-trailing-spaces\">"),
                "XSL should contain remove-trailing-spaces utility template");
            assertTrue(xsltContent.contains("<xsl:template name=\"preserve-indentation\">"),
                "XSL should contain preserve-indentation utility template");
        }

        @Test
        @DisplayName("Should verify XSL file has correct output method")
        void shouldVerifyXslFileHasCorrectOutputMethod() throws Exception {
            // Given
            String xsltContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertTrue(xsltContent.contains("<xsl:output method=\"text\" encoding=\"UTF-8\"/>"),
                "XSL should specify text output method with UTF-8 encoding");
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
            assertTrue(result.contains("System Administrator with expertise in Java development"),
                "Result should contain the role description");
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
            assertTrue(result.contains("Update the VM to Java 25"),
                "Result should contain the goal description");
            assertTrue(result.contains("sudo apt install -y openjdk-25-jdk"),
                "Result should contain the command");
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
            assertTrue(result.contains("- not invest time in planning"),
                "Result should contain first output format item");
            assertTrue(result.contains("- only install the component with the given command"),
                "Result should contain second output format item");
            assertTrue(result.contains("- not explain anything"),
                "Result should contain third output format item");
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
            assertTrue(result.contains("- verify that java is configured for java 25 executing `java -version`"),
                "Result should contain first safeguard");
            assertTrue(result.contains("- if the java installation and the verification is successful, then the goal is achieved"),
                "Result should contain second safeguard");
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
            assertThrows(Exception.class, () -> performTransformation(malformedXml, xsltContent),
                "Malformed XML should cause transformation to fail");
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
            assertThrows(Exception.class, () -> performTransformation(pmlContent, malformedXsl),
                "Malformed XSL should cause transformation to fail");
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
            assertDoesNotThrow(() -> {
                PmlConverter converter = new PmlConverter();
                String result = converter.toMarkdown(pmlFile);
                assertNotNull(result, "PmlConverter result should not be null");
                assertFalse(result.trim().isEmpty(), "PmlConverter result should not be empty");
            }, "PmlConverter.toMarkdown should work without exceptions");
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
            assertEquals(directResult, converterResult,
                "PmlConverter should produce same result as direct transformation");
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
