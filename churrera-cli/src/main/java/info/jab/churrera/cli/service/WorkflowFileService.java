package info.jab.churrera.cli.service;

import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for reading workflow files and prompt files.
 */
public class WorkflowFileService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowFileService.class);

    private final WorkflowParser workflowParser;

    public WorkflowFileService(WorkflowParser workflowParser) {
        this.workflowParser = workflowParser;
    }

    /**
     * Parse the workflow XML file to extract PML file information.
     *
     * @param workflowPath the path to the workflow XML file
     * @return the parsed workflow data
     */
    public WorkflowData parseWorkflow(String workflowPath) {
        try {
            logger.info("Parsing workflow file: {}", workflowPath);
            Path path = Paths.get(workflowPath);
            logger.info("Workflow file path resolved to: {}", path.toAbsolutePath());
            logger.info("Workflow file exists: {}", Files.exists(path));
            WorkflowData result = workflowParser.parse(path);
            logger.info("Workflow parsed successfully: {}", result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse workflow file " + workflowPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Read a prompt file from the same directory as the workflow file.
     *
     * @param workflowPath the path to the workflow file
     * @param promptFileName the name of the prompt file
     * @return the content of the prompt file
     */
    public String readPromptFile(String workflowPath, String promptFileName) {
        try {
            Path workflowDir = Paths.get(workflowPath).getParent();
            Path promptPath = workflowDir.resolve(promptFileName);

            if (!Files.exists(promptPath)) {
                throw new RuntimeException("Prompt file not found: " + promptPath);
            }

            return Files.readString(promptPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read prompt file: " + e.getMessage(), e);
        }
    }

    /**
     * Infers the prompt type from the file extension.
     *
     * @param srcFile the source file path/name
     * @return the inferred type: "pml" for .xml, "markdown" for .md, "text plain" for .txt
     */
    public String inferTypeFromExtension(String srcFile) {
        if (srcFile == null || srcFile.trim().isEmpty()) {
            throw new RuntimeException("Source file cannot be null or empty");
        }

        int lastDotIndex = srcFile.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == srcFile.length() - 1) {
            throw new RuntimeException("Invalid file extension: file '" + srcFile + "' must have a valid extension (.xml, .md, or .txt)");
        }

        String extension = srcFile.substring(lastDotIndex + 1).toLowerCase();

        switch (extension) {
            case "xml":
                return "pml";
            case "md":
                return "markdown";
            case "txt":
                return "text plain";
            default:
                throw new RuntimeException("Unsupported file extension: '" + extension + "' in file '" + srcFile + "'. Supported extensions are: .xml, .md, .txt");
        }
    }
}

