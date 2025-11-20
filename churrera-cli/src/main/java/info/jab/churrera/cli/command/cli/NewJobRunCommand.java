package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowValidator;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.WorkflowParseException;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.SequenceInfo;
import info.jab.churrera.cli.model.AgentState;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Command to create a new job with a specified path.
 */
@CommandLine.Command(
    name = "jobs new",
    description = "Create new job with specified path"
)
public class NewJobRunCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NewJobRunCommand.class);

    private final JobRepository jobRepository;
    private final String jobPath;
    private final WorkflowValidator workflowValidator;
    private final WorkflowParser workflowParser;
    private final PmlValidator pmlValidator;

    /**
     * Constructor with dependency injection for testing.
     *
     * @param jobRepository the job repository
     * @param jobPath the path to the workflow file
     * @param workflowValidator the workflow validator (can be null, will be created if null)
     * @param workflowParser the workflow parser (can be null, will be created if null)
     * @param pmlValidator the PML validator (can be null, will be created if null)
     */
    public NewJobRunCommand(JobRepository jobRepository, String jobPath,
                           WorkflowValidator workflowValidator,
                           WorkflowParser workflowParser,
                           PmlValidator pmlValidator) {
        this.jobRepository = jobRepository;
        this.jobPath = jobPath;
        this.workflowValidator = workflowValidator != null ? workflowValidator : new WorkflowValidator();
        this.workflowParser = workflowParser != null ? workflowParser : new WorkflowParser();
        this.pmlValidator = pmlValidator != null ? pmlValidator : new PmlValidator();
    }

    @Override
    public void run() {
        logger.info("Creating new job from workflow file: {}", jobPath);
        try {
            // Resolve the workflow file path
            Path workflowPath = Paths.get(jobPath);
            File workflowFile = workflowPath.toFile();

            if (!workflowFile.exists()) {
                logger.error("Workflow file does not exist: {}", jobPath);
                System.err.println("Error: Workflow file does not exist: " + jobPath);
                return;
            }

            logger.debug("Workflow file found: {}", workflowFile.getAbsolutePath());

            // Validate the workflow XML against the XSD schema
            logger.debug("Validating workflow file against XSD schema");
            WorkflowValidator.ValidationResult validationResult = workflowValidator.validate(workflowFile);

            if (!validationResult.isValid()) {
                logger.error("Workflow validation failed: {}", validationResult.getFormattedErrors());
                System.err.println("Workflow validation failed:");
                System.err.println(validationResult.getFormattedErrors());
                return;
            }

            logger.debug("Workflow validation passed");

            // Parse the workflow to extract agent and prompt information
            logger.debug("Parsing workflow file");
            WorkflowData workflowData = workflowParser.parse(workflowFile);

            // Validate timeout and fallback attributes
            logger.debug("Validating timeout and fallback attributes");
            List<String> timeoutFallbackErrors = workflowValidator.validateTimeoutAndFallback(workflowFile, workflowData);
            if (!timeoutFallbackErrors.isEmpty()) {
                logger.error("Timeout/fallback validation failed: {} errors found", timeoutFallbackErrors.size());
                System.err.println("Timeout/fallback validation failed:");
                for (int i = 0; i < timeoutFallbackErrors.size(); i++) {
                    System.err.println("  " + (i + 1) + ". " + timeoutFallbackErrors.get(i));
                }
                return;
            }

            logger.debug("Timeout/fallback validation passed");

            // Validate all PML files referenced in the workflow
            logger.debug("Validating PML files referenced in workflow");
            List<String> pmlValidationErrors = validatePmlFiles(workflowFile, workflowData);
            if (!pmlValidationErrors.isEmpty()) {
                logger.error("PML validation failed: {} errors found", pmlValidationErrors.size());
                System.err.println("PML validation failed:");
                for (int i = 0; i < pmlValidationErrors.size(); i++) {
                    System.err.println("  " + (i + 1) + ". " + pmlValidationErrors.get(i));
                }
                return;
            }

            logger.debug("PML validation passed");

            // Generate IDs and timestamps
            String jobId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            logger.debug("Generated jobId: {}", jobId);

            // Get model and repository from workflow
            String model = workflowData.getModel();
            String repository = workflowData.getRepository();

            // Provide default values for required fields if not specified in workflow
            if (model == null || model.trim().isEmpty()) {
                logger.debug("Model not specified in workflow, using default");
                model = "default-model";
            }
            if (repository == null || repository.trim().isEmpty()) {
                logger.debug("Repository not specified in workflow, using default");
                repository = "default-repository";
            }

            logger.debug("Using model: {}, repository: {}", model, repository);

            // Determine workflow type
            WorkflowType workflowType = WorkflowParser.determineWorkflowType(workflowFile);
            logger.debug("Workflow type determined: {}", workflowType);

            // Extract timeout and fallback from workflow data
            Long timeoutMillis = workflowData.getTimeoutMillis();
            String fallbackSrc = workflowData.getFallbackSrc();
            logger.debug("Extracted timeout: {}ms, fallback: {}", timeoutMillis, fallbackSrc);

            // Create and save the job with all required fields
            Job job = new Job(
                jobId,
                jobPath,
                null, // cursorAgentId is initially null until launched in Cursor
                model,
                repository,
                AgentState.CREATING(),
                now,
                now,
                null, // parentJobId is null for top-level jobs
                null, // result is null initially
                workflowType, // workflow type
                timeoutMillis, // timeout in milliseconds (null if not specified)
                null, // workflowStartTime is null initially, set when launched if timeout is set
                fallbackSrc, // fallback source file path (null if not specified)
                null // fallbackExecuted is null initially (false when not executed)
            );
            logger.info("Saving job with jobId: {}", jobId);
            jobRepository.save(job);
            logger.debug("Job saved successfully");

            // Create and save prompts (all prompts including the first one)
            List<PromptInfo> allPrompts = new ArrayList<>();
            allPrompts.add(workflowData.getLaunchPrompt());
            allPrompts.addAll(workflowData.getUpdatePrompts());

            logger.debug("Creating {} prompts for job", allPrompts.size());
            for (PromptInfo promptInfo : allPrompts) {
                String promptId = UUID.randomUUID().toString();
                Prompt prompt = new Prompt(
                    promptId,
                    jobId,
                    promptInfo.getSrcFile(),
                    "UNKNOWN",
                    now,
                    now
                );
                logger.debug("Saving prompt: {} (PML: {})", promptId, promptInfo.getSrcFile());
                jobRepository.savePrompt(prompt);
            }

            logger.info("Job created successfully with jobId: {}, {} prompts created", jobId, allPrompts.size());
            // Display success message
            System.out.println("Job created successfully!");

        } catch (WorkflowParseException e) {
            logger.error("Error parsing workflow: {}", jobPath, e);
            System.err.println("Error parsing workflow: " + e.getMessage());
        } catch (QueryException | IOException e) {
            logger.error("Error creating job: {}", jobPath, e);
            System.err.println("Error creating job: " + e.getMessage());
        }
    }

    /**
     * Validates all PML files (.xml) referenced in the workflow.
     *
     * @param workflowFile the workflow file (used to resolve relative paths)
     * @param workflowData the parsed workflow data
     * @return list of validation error messages (empty if all valid)
     */
    private List<String> validatePmlFiles(File workflowFile, WorkflowData workflowData) {
        List<String> allErrors = new ArrayList<>();
        Path workflowDir = workflowFile.getParentFile() != null
            ? workflowFile.getParentFile().toPath()
            : Paths.get(".");

        // Collect all prompts from the workflow
        List<PromptInfo> allPrompts = collectAllPrompts(workflowData);

        // Validate each PML file (.xml extension)
        for (PromptInfo promptInfo : allPrompts) {
            String srcFile = promptInfo.getSrcFile();

            // Only validate files with .xml extension (PML files)
            if (srcFile != null && srcFile.toLowerCase().endsWith(".xml")) {
                Path pmlPath = workflowDir.resolve(srcFile);
                File pmlFile = pmlPath.toFile();

                logger.debug("Validating PML file: {}", pmlFile.getAbsolutePath());
                PmlValidator.ValidationResult result = pmlValidator.validate(pmlFile);

                if (!result.isValid()) {
                    allErrors.add("PML file '" + srcFile + "':");
                    for (String error : result.getErrors()) {
                        allErrors.add("  - " + error);
                    }
                }
            }
        }

        return allErrors;
    }

    /**
     * Collects all prompts from the workflow, including parallel workflow prompts.
     *
     * @param workflowData the workflow data
     * @return list of all prompts in the workflow
     */
    private List<PromptInfo> collectAllPrompts(WorkflowData workflowData) {
        List<PromptInfo> allPrompts = new ArrayList<>();

        // Add launch prompt
        allPrompts.add(workflowData.getLaunchPrompt());

        // Add update prompts
        allPrompts.addAll(workflowData.getUpdatePrompts());

        // If parallel workflow, add parallel prompt and all sequence prompts
        if (workflowData.isParallelWorkflow()) {
            ParallelWorkflowData parallelData = workflowData.getParallelWorkflowData();
            allPrompts.add(parallelData.getParallelPrompt());

            for (SequenceInfo sequence : parallelData.getSequences()) {
                allPrompts.addAll(sequence.getPrompts());
            }
        }

        return allPrompts;
    }
}
