package info.jab.churrera.workflow;

import info.jab.churrera.util.PropertyResolver;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for validating PML XML files against the XSD schema.
 */
public class PmlValidator {

    private static final String SCHEMA_URL_PROPERTY = "pml.schema.url";
    private static final String AT_LINE_SUFFIX = " at line ";
    private final PropertyResolver propertyResolver;

    public PmlValidator() {
        this.propertyResolver = new PropertyResolver();
    }

    /**
     * Validates a PML XML file against the XSD schema.
     *
     * @param pmlFile the PML XML file to validate
     * @return ValidationResult containing validation status and any error messages
     */
    public ValidationResult validate(File pmlFile) {
        List<String> errors = new ArrayList<>();

        try {
            // Check if file exists
            if (!pmlFile.exists()) {
                errors.add("PML file does not exist: " + pmlFile.getAbsolutePath());
                return new ValidationResult(false, errors);
            }

            // Load the XSD schema from external URL
            Schema schema = loadSchema();

            // Create validator
            Validator validator = schema.newValidator();

            // Create custom error handler to collect validation errors
            ValidationErrorHandler errorHandler = new ValidationErrorHandler();
            validator.setErrorHandler(errorHandler);

            // Validate the XML file
            validator.validate(new StreamSource(pmlFile));

            // Check if there were any validation errors
            if (errorHandler.hasErrors()) {
                errors.addAll(errorHandler.getErrors());
            }

        } catch (SAXException e) {
            errors.add("XML parsing error: " + e.getMessage());
        } catch (IOException e) {
            errors.add("File I/O error: " + e.getMessage());
        } catch (Exception e) {
            errors.add("Unexpected error during validation: " + e.getMessage());
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates a PML XML file against the XSD schema using a file path.
     *
     * @param pmlPath the path to the PML XML file
     * @return ValidationResult containing validation status and any error messages
     */
    public ValidationResult validate(Path pmlPath) {
        return validate(pmlPath.toFile());
    }

    /**
     * Creates a secure SchemaFactory with external entity access disabled
     * to prevent XXE (XML External Entity) attacks.
     *
     * @return a configured SchemaFactory with security features enabled
     * @throws SAXException if the factory cannot be configured
     */
    private static SchemaFactory createSecureSchemaFactory() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // Enable secure processing feature
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // Disable external DTD access
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

        // Disable external schema access
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        return factory;
    }

    /**
     * Loads the XSD schema from the external URL.
     *
     * @return the loaded Schema object
     * @throws SAXException if there's an error parsing the schema
     * @throws IOException if there's an error reading the schema file
     */
    private Schema loadSchema() throws SAXException, IOException {
        SchemaFactory factory = createSecureSchemaFactory();
        String schemaUrl = getSchemaUrl();
        return factory.newSchema(java.net.URI.create(schemaUrl).toURL());
    }

    /**
     * Gets the schema URL from properties.
     *
     * @return the schema URL to use for validation
     * @throws IllegalStateException if the schema URL property is not configured
     */
    private String getSchemaUrl() {
        return propertyResolver.getProperty("application.properties", SCHEMA_URL_PROPERTY)
                .orElseThrow(() -> new IllegalStateException("Schema URL not configured. Please set property: " + SCHEMA_URL_PROPERTY));
    }

    /**
     * Result of PML validation containing status and error messages.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
        }

        /**
         * Returns true if the PML XML is valid according to the schema.
         *
         * @return true if valid, false otherwise
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Returns the list of validation error messages.
         *
         * @return list of error messages (empty if valid)
         */
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        /**
         * Returns a formatted string containing all error messages.
         *
         * @return formatted error string
         */
        public String getFormattedErrors() {
            if (errors.isEmpty()) {
                return "No validation errors.";
            }

            StringBuilder sb = new StringBuilder("Validation errors:\n");
            for (int i = 0; i < errors.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Custom error handler to collect validation errors.
     */
    static class ValidationErrorHandler implements org.xml.sax.ErrorHandler {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void warning(org.xml.sax.SAXParseException exception) {
            errors.add("Warning: " + exception.getMessage() + AT_LINE_SUFFIX + exception.getLineNumber());
        }

        @Override
        public void error(org.xml.sax.SAXParseException exception) {
            errors.add("Error: " + exception.getMessage() + AT_LINE_SUFFIX + exception.getLineNumber());
        }

        @Override
        public void fatalError(org.xml.sax.SAXParseException exception) {
            errors.add("Fatal Error: " + exception.getMessage() + AT_LINE_SUFFIX + exception.getLineNumber());
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
    }
}

