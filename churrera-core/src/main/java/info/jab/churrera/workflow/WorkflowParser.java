package info.jab.churrera.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for parsing workflow XML files and extracting agent and prompt information.
 */
public class WorkflowParser {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowParser.class);

    /**
     * Parses a workflow XML file and extracts the sequence or parallel elements.
     * Supports both v2 (sequence/prompt) and v3 (parallel) schemas.
     *
     * @param workflowFile the workflow XML file to parse
     * @return WorkflowData containing parsed workflow information
     * @throws WorkflowParseException if there's an error parsing the workflow
     */
    public WorkflowData parse(File workflowFile) throws WorkflowParseException {
        try {
            logger.info("Starting to parse workflow file: {}", workflowFile.getAbsolutePath());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable namespace awareness for simpler element access
            factory.setNamespaceAware(false);
            logger.info("Created document builder factory");

            DocumentBuilder builder = factory.newDocumentBuilder();
            // Set entity resolver to prevent external entity resolution (e.g., XSD schema)
            builder.setEntityResolver((publicId, systemId) -> {
                logger.debug("Ignoring external entity: publicId={}, systemId={}", publicId, systemId);
                // Return empty input source to prevent network lookups
                return new org.xml.sax.InputSource(new java.io.StringReader(""));
            });
            logger.info("Created document builder with entity resolver");

            Document document = builder.parse(workflowFile);
            logger.info("Parsed XML document");

            // Get the root element
            Element root = document.getDocumentElement();
            String rootName = root.getNodeName();
            logger.info("Root element: {}", rootName);

            if (!"pml-workflow".equals(rootName)) {
                throw new WorkflowParseException("Root element must be 'pml-workflow'");
            }

            // Check for parallel workflow first
            NodeList parallelList = root.getElementsByTagName("parallel");
            if (parallelList.getLength() > 0) {
                logger.info("Found parallel workflow");
                WorkflowData result = parseParallelWorkflow((Element) parallelList.item(0));
                logger.info("Successfully parsed parallel workflow");
                return result;
            }

            // Check for v2 schema (sequence element)
            NodeList sequenceList = root.getElementsByTagName("sequence");
            logger.info("Found {} sequence elements", sequenceList.getLength());

            if (sequenceList.getLength() == 0) {
                throw new WorkflowParseException("No 'sequence' or 'parallel' element found in workflow.");
            }

            WorkflowData result = parseV2Workflow(sequenceList.item(0));
            logger.info("Successfully parsed workflow: launch={}, model={}, repository={}, updates={}",
                result.getLaunchPrompt().getSrcFile(), result.getModel(), result.getRepository(), result.getUpdatePrompts().size());

            return result;

        } catch (ParserConfigurationException e) {
            logger.error("Error configuring XML parser: {}", e.getMessage(), e);
            throw new WorkflowParseException("Error configuring XML parser: " + e.getMessage(), e);
        } catch (SAXException e) {
            logger.error("Error parsing XML: {}", e.getMessage(), e);
            throw new WorkflowParseException("Error parsing XML: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error reading file: {}", e.getMessage(), e);
            throw new WorkflowParseException("Error reading file: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof WorkflowParseException) {
                throw e;
            }
            logger.error("Error parsing workflow XML: {}", e.getMessage(), e);
            throw new WorkflowParseException("Error parsing workflow XML: " + e.getMessage(), e);
        }
    }

    /**
     * Infers the prompt type from the file extension in the src attribute.
     *
     * @param srcFile the source file path/name
     * @return the inferred type: "pml" for .xml, "markdown" for .md, "text plain" for .txt
     * @throws RuntimeException if the file extension is not one of the supported extensions
     */
    private static String inferTypeFromExtension(String srcFile) {
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

    /**
     * Parse v2 workflow format (sequence/prompt).
     */
    private WorkflowData parseV2Workflow(org.w3c.dom.Node sequenceNode) throws WorkflowParseException {
        Element sequence = (Element) sequenceNode;

        // Extract model and repository from sequence attributes
        String model = sequence.getAttribute("model");
        String repository = sequence.getAttribute("repository");

        // Parse all prompt elements
        NodeList promptList = sequence.getElementsByTagName("prompt");
        if (promptList.getLength() == 0) {
            throw new WorkflowParseException("No 'prompt' elements found in sequence");
        }

        List<PromptInfo> allPrompts = new ArrayList<>();
        for (int i = 0; i < promptList.getLength(); i++) {
            Element prompt = (Element) promptList.item(i);
            String srcFile = prompt.getAttribute("src");
            if (srcFile == null || srcFile.trim().isEmpty()) {
                throw new WorkflowParseException("Prompt at index " + i + " missing required 'src' attribute");
            }
            String type = inferTypeFromExtension(srcFile);
            String bindResultExp = prompt.getAttribute("bindResultExp");
            allPrompts.add(new PromptInfo(srcFile, type, bindResultExp));
        }

        // First prompt is the launch prompt, rest are update prompts
        PromptInfo launchPrompt = allPrompts.get(0);
        List<PromptInfo> updatePrompts = allPrompts.subList(1, allPrompts.size());

        return new WorkflowData(launchPrompt, model, repository, updatePrompts, null);
    }

    /**
     * Parse parallel workflow format.
     */
    private WorkflowData parseParallelWorkflow(Element parallelElement) throws WorkflowParseException {
        // Extract attributes from parallel element
        String srcFile = parallelElement.getAttribute("src");
        if (srcFile == null || srcFile.trim().isEmpty()) {
            throw new WorkflowParseException("Parallel element missing required 'src' attribute");
        }

        String type = inferTypeFromExtension(srcFile);
        String bindResultType = parallelElement.getAttribute("bindResultType");

        // Create the parallel prompt info
        PromptInfo parallelPrompt = new PromptInfo(srcFile, type, null);

        // Parse nested sequence elements
        NodeList sequenceList = parallelElement.getElementsByTagName("sequence");
        if (sequenceList.getLength() == 0) {
            throw new WorkflowParseException("Parallel element must contain at least one sequence element");
        }

        List<SequenceInfo> sequences = new ArrayList<>();
        for (int i = 0; i < sequenceList.getLength(); i++) {
            Element sequenceElement = (Element) sequenceList.item(i);
            SequenceInfo sequenceInfo = parseSequenceInfo(sequenceElement);
            sequences.add(sequenceInfo);
        }

        // For parallel workflow, take model and repository from the first sequence
        SequenceInfo firstSequence = sequences.get(0);
        String model = firstSequence.getModel();
        String repository = firstSequence.getRepository();

        // Create ParallelWorkflowData
        ParallelWorkflowData parallelData = new ParallelWorkflowData(parallelPrompt, bindResultType, sequences);

        return new WorkflowData(parallelPrompt, model, repository, new ArrayList<>(), parallelData);
    }

    /**
     * Parse a sequence element into SequenceInfo.
     */
    private SequenceInfo parseSequenceInfo(Element sequenceElement) throws WorkflowParseException {
        String model = sequenceElement.getAttribute("model");
        String repository = sequenceElement.getAttribute("repository");

        NodeList promptList = sequenceElement.getElementsByTagName("prompt");
        List<PromptInfo> prompts = new ArrayList<>();

        for (int i = 0; i < promptList.getLength(); i++) {
            Element prompt = (Element) promptList.item(i);
            String srcFile = prompt.getAttribute("src");
            if (srcFile == null || srcFile.trim().isEmpty()) {
                throw new WorkflowParseException("Prompt missing required 'src' attribute");
            }
            String type = inferTypeFromExtension(srcFile);
            String bindResultExp = prompt.getAttribute("bindResultExp");
            prompts.add(new PromptInfo(srcFile, type, bindResultExp));
        }

        return new SequenceInfo(model, repository, prompts);
    }

    /**
     * Parses a workflow XML file using a file path.
     *
     * @param workflowPath the path to the workflow XML file
     * @return WorkflowData containing parsed sequence and prompt information
     * @throws WorkflowParseException if there's an error parsing the workflow
     */
    public WorkflowData parse(Path workflowPath) throws WorkflowParseException {
        return parse(workflowPath.toFile());
    }

    /**
     * Determines the workflow type from a workflow XML file without full parsing.
     *
     * @param workflowFile the workflow XML file to check
     * @return WorkflowType (SEQUENCE or PARALLEL), or null if unable to determine
     */
    public static WorkflowType determineWorkflowType(File workflowFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> {
                return new org.xml.sax.InputSource(new java.io.StringReader(""));
            });

            Document document = builder.parse(workflowFile);
            Element root = document.getDocumentElement();

            if (!"pml-workflow".equals(root.getNodeName())) {
                return null;
            }

            // Check for parallel workflow first
            NodeList parallelList = root.getElementsByTagName("parallel");
            if (parallelList.getLength() > 0) {
                return WorkflowType.PARALLEL;
            }

            // Check for sequence workflow
            NodeList sequenceList = root.getElementsByTagName("sequence");
            if (sequenceList.getLength() > 0) {
                return WorkflowType.SEQUENCE;
            }

            // Neither found
            return null;

        } catch (Exception e) {
            logger.warn("Unable to determine workflow type for file {}: {}",
                workflowFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

}

