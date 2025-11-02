package info.jab.churrera.util;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Class for converting PML (Prompt Markup Language) XML files to Markdown format
 * using XSLT transformations.
 */
public final class PmlConverter {

    private final ClasspathResolver resolver;

    /**
     * Constructor that creates a default ClasspathResolver.
     */
    public PmlConverter() {
        this.resolver = new ClasspathResolver();
    }

    /**
     * Constructor that accepts a ClasspathResolver for dependency injection and testing.
     *
     * @param resolver the ClasspathResolver to use for loading resources
     * @throws IllegalArgumentException if resolver is null
     */
    public PmlConverter(ClasspathResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("ClasspathResolver cannot be null");
        }
        this.resolver = resolver;
    }

    /**
     * Converts a PML XML file to Markdown format using the specified XSLT transformation.
     *
     * @param pmlFile the path to the PML XML file in classpath resources
     * @param xsltFile the path to the XSLT file in classpath resources
     * @return the converted Markdown content as a String
     * @throws IllegalArgumentException if either parameter is null or empty
     * @throws RuntimeException if the conversion fails
     */
    public String toMarkdown(String pmlFile, String xsltFile) {
        if (pmlFile == null || pmlFile.trim().isEmpty()) {
            throw new IllegalArgumentException("PML file path cannot be null or empty");
        }
        if (xsltFile == null || xsltFile.trim().isEmpty()) {
            throw new IllegalArgumentException("XSLT file path cannot be null or empty");
        }

        try {
            // Load PML XML content from classpath
            String pmlContent = resolver.retrieve(pmlFile);

            // Load XSLT content from classpath
            String xsltContent = resolver.retrieve(xsltFile);

            // Create input streams
            InputStream pmlStream = new ByteArrayInputStream(pmlContent.getBytes(StandardCharsets.UTF_8));
            InputStream xsltStream = new ByteArrayInputStream(xsltContent.getBytes(StandardCharsets.UTF_8));

            // Create transformer
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xsltStream));

            // Perform transformation
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            transformer.transform(new StreamSource(pmlStream), new StreamResult(outputStream));

            // Return result as String
            return outputStream.toString(StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PML to Markdown: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a PML XML file to Markdown format using the default XSLT transformation.
     * Uses the default XSLT file located at "pml/pml-to-md.xsl".
     *
     * @param pmlFile the path to the PML XML file in classpath resources
     * @return the converted Markdown content as a String
     * @throws IllegalArgumentException if pmlFile is null or empty
     * @throws RuntimeException if the conversion fails
     */
    public String toMarkdown(String pmlFile) {
        return toMarkdown(pmlFile, "pml/pml-to-md.xsl");
    }

    /**
     * Converts PML XML content directly to Markdown format using the specified XSLT transformation.
     *
     * @param pmlContent the PML XML content as a String
     * @param xsltFile the path to the XSLT file in classpath resources
     * @return the converted Markdown content as a String
     * @throws IllegalArgumentException if either parameter is null or empty
     * @throws RuntimeException if the conversion fails
     */
    public String toMarkdownFromContent(String pmlContent, String xsltFile) {
        if (pmlContent == null || pmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("PML content cannot be null or empty");
        }
        if (xsltFile == null || xsltFile.trim().isEmpty()) {
            throw new IllegalArgumentException("XSLT file path cannot be null or empty");
        }

        try {
            // Load XSLT content from classpath
            String xsltContent = resolver.retrieve(xsltFile);

            // Create input streams
            InputStream pmlStream = new ByteArrayInputStream(pmlContent.getBytes(StandardCharsets.UTF_8));
            InputStream xsltStream = new ByteArrayInputStream(xsltContent.getBytes(StandardCharsets.UTF_8));

            // Create transformer
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xsltStream));

            // Perform transformation
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            transformer.transform(new StreamSource(pmlStream), new StreamResult(outputStream));

            // Return result as String
            return outputStream.toString(StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PML content to Markdown: " + e.getMessage(), e);
        }
    }

    /**
     * Converts PML XML content directly to Markdown format using the default XSLT transformation.
     * Uses the default XSLT file located at "pml/pml-to-md.xsl".
     *
     * @param pmlContent the PML XML content as a String
     * @return the converted Markdown content as a String
     * @throws IllegalArgumentException if pmlContent is null or empty
     * @throws RuntimeException if the conversion fails
     */
    public String toMarkdownFromContent(String pmlContent) {
        return toMarkdownFromContent(pmlContent, "pml/pml-to-md.xsl");
    }
}
