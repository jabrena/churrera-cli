package info.jab.churrera.workflow;

/**
 * Exception thrown when there's an error parsing a workflow XML file.
 */
public class WorkflowParseException extends Exception {
    public WorkflowParseException(String message) {
        super(message);
    }

    public WorkflowParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

