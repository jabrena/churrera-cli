package info.jab.churrera.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validating XML files against XSD schemas and ensuring they are well-formed.
 * This class tests both the structural validity and schema compliance of PML XML files.
 */
@DisplayName("XML Validation Tests")
class XmlValidationTest {

    private DocumentBuilderFactory documentBuilderFactory;
    private SchemaFactory schemaFactory;
    private ClasspathResolver resolver;

    @BeforeEach
    void setUp() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        resolver = new ClasspathResolver();
    }

    @Nested
    @DisplayName("XML Well-formedness Tests")
    class WellFormednessTests {

        @Test
        @DisplayName("Should validate that prompt1.xml is well-formed")
        void shouldValidatePrompt1XmlIsWellFormed() throws Exception {
            // Given
            String xmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");

            // When & Then
            assertDoesNotThrow(() -> parseXml(xmlContent),
                "prompt1.xml should be well-formed XML");
        }

        @Test
        @DisplayName("Should validate that pml-to-md.xsl is well-formed")
        void shouldValidatePmlToMdXslIsWellFormed() throws Exception {
            // Given
            String xmlContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertDoesNotThrow(() -> parseXml(xmlContent),
                "pml-to-md.xsl should be well-formed XML");
        }

        @Test
        @DisplayName("Should throw exception for malformed XML")
        void shouldThrowExceptionForMalformedXml() {
            // Given
            String malformedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<prompt>\n" +
                    "    <role>Test role</role>\n" +
                    "    <!-- Missing closing tag -->\n" +
                    "    <goal>Test goal\n";

            // When & Then
            assertThrows(Exception.class, () -> parseXml(malformedXml),
                "Malformed XML should throw an exception");
        }
    }

    @Nested
    @DisplayName("XSD Schema Validation Tests")
    class SchemaValidationTests {

        @Test
        @DisplayName("Should validate prompt1.xml against PML XSD schema")
        void shouldValidatePrompt1XmlAgainstPmlSchema() throws Exception {
            // Given
            String xmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            String schemaUrl = "https://jabrena.github.io/pml/schemas/0.1.0/pml.xsd";

            // When & Then
            assertDoesNotThrow(() -> validateAgainstSchema(xmlContent, schemaUrl),
                "prompt1.xml should be valid against PML XSD schema");
        }

        @Test
        @DisplayName("Should validate XSL file against XSLT schema")
        void shouldValidateXslAgainstXsltSchema() throws Exception {
            // Given
            String xmlContent = resolver.retrieve("pml/pml-to-md.xsl");
            String schemaUrl = "http://www.w3.org/1999/XSL/Transform";

            // When & Then
            assertDoesNotThrow(() -> validateXslFile(xmlContent),
                "pml-to-md.xsl should be valid XSLT");
        }

        @Test
        @DisplayName("Should detect invalid XML structure against schema")
        void shouldDetectInvalidXmlStructureAgainstSchema() {
            // Given
            String invalidXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<prompt xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "        xsi:noNamespaceSchemaLocation=\"https://jabrena.github.io/pml/schemas/0.2.0/pml.xsd\">\n" +
                    "    <invalid-element>This should not be here</invalid-element>\n" +
                    "</prompt>";
            String schemaUrl = "https://jabrena.github.io/pml/schemas/0.1.0/pml.xsd";

            // When & Then
            assertThrows(SAXException.class, () -> validateAgainstSchema(invalidXml, schemaUrl),
                "Invalid XML structure should be detected by schema validation");
        }
    }

    @Nested
    @DisplayName("XML Content Structure Tests")
    class ContentStructureTests {

        @Test
        @DisplayName("Should verify prompt1.xml has required elements")
        void shouldVerifyPrompt1XmlHasRequiredElements() throws Exception {
            // Given
            String xmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            Document document = parseXml(xmlContent);

            // When & Then
            assertNotNull(document.getElementsByTagName("role").item(0),
                "prompt1.xml should contain a role element");
            assertNotNull(document.getElementsByTagName("goal").item(0),
                "prompt1.xml should contain a goal element");
            assertNotNull(document.getElementsByTagName("output-format").item(0),
                "prompt1.xml should contain an output-format element");
            assertNotNull(document.getElementsByTagName("safeguards").item(0),
                "prompt1.xml should contain a safeguards element");
        }

        @Test
        @DisplayName("Should verify XSL file has required XSLT elements")
        void shouldVerifyXslFileHasRequiredXsltElements() throws Exception {
            // Given
            String xmlContent = resolver.retrieve("pml/pml-to-md.xsl");
            Document document = parseXml(xmlContent);

            // When & Then
            assertNotNull(document.getElementsByTagName("xsl:stylesheet").item(0),
                "XSL file should contain xsl:stylesheet element");
            assertNotNull(document.getElementsByTagName("xsl:template").item(0),
                "XSL file should contain xsl:template elements");
        }
    }

    @Nested
    @DisplayName("XML Namespace Tests")
    class NamespaceTests {

        @Test
        @DisplayName("Should verify prompt1.xml has correct namespace declarations")
        void shouldVerifyPrompt1XmlHasCorrectNamespaceDeclarations() throws Exception {
            // Given
            String xmlContent = resolver.retrieve("examples/hello-world/prompt1.xml");
            Document document = parseXml(xmlContent);

            // When & Then
            assertTrue(xmlContent.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""),
                "prompt1.xml should declare xsi namespace");
            assertTrue(xmlContent.contains("xsi:noNamespaceSchemaLocation=\"https://jabrena.github.io/pml/schemas/0.2.0/pml.xsd\""),
                "prompt1.xml should reference PML schema");
        }

        @Test
        @DisplayName("Should verify XSL file has correct namespace declarations")
        void shouldVerifyXslFileHasCorrectNamespaceDeclarations() throws Exception {
            // Given
            String xmlContent = resolver.retrieve("pml/pml-to-md.xsl");

            // When & Then
            assertTrue(xmlContent.contains("xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\""),
                "XSL file should declare xsl namespace");
            assertTrue(xmlContent.contains("xmlns:xi=\"http://www.w3.org/2001/XInclude\""),
                "XSL file should declare xi namespace for XInclude");
        }
    }

    /**
     * Parses XML content and returns a Document object.
     *
     * @param xmlContent the XML content to parse
     * @return the parsed Document
     * @throws Exception if parsing fails
     */
    private Document parseXml(String xmlContent) throws Exception {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        try (InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8))) {
            return builder.parse(inputStream);
        }
    }

    /**
     * Validates XML content against a remote XSD schema.
     *
     * @param xmlContent the XML content to validate
     * @param schemaUrl the URL of the XSD schema
     * @throws Exception if validation fails
     */
    private void validateAgainstSchema(String xmlContent, String schemaUrl) throws Exception {
        Schema schema = schemaFactory.newSchema(new StreamSource(schemaUrl));
        Validator validator = schema.newValidator();

        try (InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8))) {
            Source source = new StreamSource(inputStream);
            validator.validate(source);
        }
    }

    /**
     * Validates that an XSL file is valid XSLT by attempting to create a transformer.
     *
     * @param xslContent the XSL content to validate
     * @throws Exception if validation fails
     */
    private void validateXslFile(String xslContent) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        try (InputStream inputStream = new ByteArrayInputStream(xslContent.getBytes(StandardCharsets.UTF_8))) {
            Source source = new StreamSource(inputStream);
            factory.newTransformer(source);
        }
    }
}
