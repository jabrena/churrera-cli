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
 * Service for validating workflow XML files against the XSD schema.
 * Only supports v2 (pml-workflow.xsd) schema.
 */
public class WorkflowValidator {

    private static final String SCHEMA_URL_PROPERTY = "workflow.schema.url";
    private final PropertyResolver propertyResolver;

    public WorkflowValidator() {
        this.propertyResolver = new PropertyResolver();
    }

    /**
     * Validates a workflow XML file against the XSD schema.
     *
     * @param workflowFile the workflow XML file to validate
     * @return ValidationResult containing validation status and any error messages
     */
    public ValidationResult validate(File workflowFile) {
        List<String> errors = new ArrayList<>();

        try {
            // Load the XSD schema from resources
            Schema schema = loadSchema();

            // Create validator
            Validator validator = schema.newValidator();

            // Create custom error handler to collect validation errors
            ValidationErrorHandler errorHandler = new ValidationErrorHandler();
            validator.setErrorHandler(errorHandler);

            // Validate the XML file
            validator.validate(new StreamSource(workflowFile));

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
     * Validates a workflow XML file against the XSD schema using a file path.
     *
     * @param workflowPath the path to the workflow XML file
     * @return ValidationResult containing validation status and any error messages
     */
    public ValidationResult validate(Path workflowPath) {
        return validate(workflowPath.toFile());
    }

    /**
     * Validates timeout and fallback attributes from a parsed workflow.
     *
     * @param workflowFile the workflow file (used to resolve relative paths for fallback)
     * @param workflowData the parsed workflow data
     * @return list of validation error messages (empty if all valid)
     */
    public List<String> validateTimeoutAndFallback(File workflowFile, WorkflowData workflowData) {
        List<String> errors = new ArrayList<>();

        // Get timeout and fallback from workflow data
        Long timeoutMillis = workflowData.getTimeoutMillis();
        String fallbackSrc = workflowData.getFallbackSrc();

        // Validate timeout format (already validated during parsing, but check consistency)
        // If fallback is specified but timeout is not, that's an error
        if (fallbackSrc != null && !fallbackSrc.trim().isEmpty() && timeoutMillis == null) {
            errors.add("fallback-src is specified but timeout is not. fallback-src requires timeout to be set.");
        }

        // Validate fallback file exists and has valid extension
        if (fallbackSrc != null && !fallbackSrc.trim().isEmpty()) {
            errors.addAll(validateFallbackFile(workflowFile, fallbackSrc));
        }

        // For parallel workflows, also validate nested sequences
        if (workflowData.isParallelWorkflow()) {
            ParallelWorkflowData parallelData = workflowData.getParallelWorkflowData();
            for (SequenceInfo sequence : parallelData.getSequences()) {
                Long seqTimeout = sequence.getTimeoutMillis();
                String seqFallback = sequence.getFallbackSrc();

                // If sequence fallback is specified but timeout is not, that's an error
                if (seqFallback != null && !seqFallback.trim().isEmpty() && seqTimeout == null) {
                    errors.add("Sequence fallback-src is specified but timeout is not. fallback-src requires timeout to be set.");
                }

                // Validate sequence fallback file
                if (seqFallback != null && !seqFallback.trim().isEmpty()) {
                    errors.addAll(validateFallbackFile(workflowFile, seqFallback));
                }
            }
        }

        return errors;
    }

    /**
     * Validates that a fallback file exists and has a valid extension.
     *
     * @param workflowFile the workflow file (used to resolve relative paths)
     * @param fallbackSrc the fallback source file path
     * @return list of validation error messages (empty if valid)
     */
    public List<String> validateFallbackFile(File workflowFile, String fallbackSrc) {
        List<String> errors = new ArrayList<>();

        if (fallbackSrc == null || fallbackSrc.trim().isEmpty()) {
            return errors; // Not specified, skip validation
        }

        // Check file extension
        String lowerFallback = fallbackSrc.toLowerCase();
        if (!lowerFallback.endsWith(".xml") && !lowerFallback.endsWith(".md") && !lowerFallback.endsWith(".txt")) {
            errors.add("Fallback file '" + fallbackSrc + "' must have extension .xml, .md, or .txt");
            return errors; // Don't check existence if extension is invalid
        }

        // Resolve fallback file path relative to workflow file directory
        java.nio.file.Path workflowDir = workflowFile.getParentFile() != null
            ? workflowFile.getParentFile().toPath()
            : java.nio.file.Paths.get(".");
        java.nio.file.Path fallbackPath = workflowDir.resolve(fallbackSrc);
        File fallbackFile = fallbackPath.toFile();

        // Check if file exists
        if (!fallbackFile.exists()) {
            errors.add("Fallback file not found: " + fallbackPath.toAbsolutePath());
        }

        return errors;
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
     * Result of workflow validation containing status and error messages.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
        }

        /**
         * Returns true if the workflow XML is valid according to the schema.
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
    private static class ValidationErrorHandler implements org.xml.sax.ErrorHandler {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void warning(org.xml.sax.SAXParseException exception) {
            errors.add("Warning: " + exception.getMessage() + " at line " + exception.getLineNumber());
        }

        @Override
        public void error(org.xml.sax.SAXParseException exception) {
            errors.add("Error: " + exception.getMessage() + " at line " + exception.getLineNumber());
        }

        @Override
        public void fatalError(org.xml.sax.SAXParseException exception) {
            errors.add("Fatal Error: " + exception.getMessage() + " at line " + exception.getLineNumber());
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
    }
}

