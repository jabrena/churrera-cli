package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.SequenceInfo;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.WorkflowParseException;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.workflow.WorkflowValidator;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for creating jobs from workflow files.
 */
public class JobCreationService {
    private static final Logger logger = LoggerFactory.getLogger(JobCreationService.class);

    private final JobRepository jobRepository;
    private final WorkflowValidator workflowValidator;
    private final WorkflowParser workflowParser;
    private final PmlValidator pmlValidator;
    private final CLIAgent cliAgent;

    public JobCreationService(JobRepository jobRepository, WorkflowValidator workflowValidator,
                             WorkflowParser workflowParser, PmlValidator pmlValidator, CLIAgent cliAgent) {
        this.jobRepository = jobRepository;
        this.workflowValidator = workflowValidator;
        this.workflowParser = workflowParser;
        this.pmlValidator = pmlValidator;
        this.cliAgent = cliAgent;
    }

    /**
     * Creates a new job from the workflow file.
     *
     * @param jobPath the path to the workflow file
     * @return job creation result
     */
    public JobCreationResult createJob(String jobPath) {
        logger.info("Creating new job from workflow file: {}", jobPath);
        try {
            // Resolve the workflow file path
            Path workflowPath = Paths.get(jobPath);
            File workflowFile = workflowPath.toFile();

            if (!workflowFile.exists()) {
                logger.error("Workflow file does not exist: {}", jobPath);
                return JobCreationResult.failure(List.of("Error: Workflow file does not exist: " + jobPath));
            }

            logger.debug("Workflow file found: {}", workflowFile.getAbsolutePath());

            // Validate the workflow XML against the XSD schema
            logger.debug("Validating workflow file against XSD schema");
            WorkflowValidator.ValidationResult validationResult = workflowValidator.validate(workflowFile);

            if (!validationResult.isValid()) {
                logger.error("Workflow validation failed: {}", validationResult.getFormattedErrors());
                return JobCreationResult.failure(List.of("Workflow validation failed:\n" + validationResult.getFormattedErrors()));
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
                return JobCreationResult.failure(formatErrors("Timeout/fallback validation failed:", timeoutFallbackErrors));
            }

            logger.debug("Timeout/fallback validation passed");

            // Validate all PML files referenced in the workflow
            logger.debug("Validating PML files referenced in workflow");
            List<String> pmlValidationErrors = validatePmlFiles(workflowFile, workflowData);
            if (!pmlValidationErrors.isEmpty()) {
                logger.error("PML validation failed: {} errors found", pmlValidationErrors.size());
                return JobCreationResult.failure(formatErrors("PML validation failed:", pmlValidationErrors));
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

            // Validate model against available models from API
            logger.debug("Validating model against available models");
            List<String> modelValidationErrors = validateModel(model);
            if (!modelValidationErrors.isEmpty()) {
                logger.error("Model validation failed: {} errors found", modelValidationErrors.size());
                return JobCreationResult.failure(formatErrors("Model validation failed:", modelValidationErrors));
            }

            logger.debug("Model validation passed");

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
            return JobCreationResult.success(jobId);

        } catch (WorkflowParseException e) {
            logger.error("Error parsing workflow: {}", jobPath, e);
            return JobCreationResult.failure(List.of("Error parsing workflow: " + e.getMessage()));
        } catch (QueryException | IOException e) {
            logger.error("Error creating job: {}", jobPath, e);
            return JobCreationResult.failure(List.of("Error creating job: " + e.getMessage()));
        }
    }

    /**
     * Validates all PML files (.xml) referenced in the workflow.
     * Package-private for testing.
     */
    List<String> validatePmlFiles(File workflowFile, WorkflowData workflowData) {
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
     * Validates the model specified in the workflow against available models from the Cursor API.
     * Package-private for testing.
     */
    List<String> validateModel(String model) {
        List<String> errors = new ArrayList<>();

        // Model should never be null or empty at this point (defaults are set before validation)
        if (model == null || model.trim().isEmpty()) {
            errors.add("Model is null or empty");
            return errors;
        }

        try {
            logger.debug("Retrieving available models from Cursor API");
            List<String> availableModels = cliAgent.getModels();

            if (availableModels == null || availableModels.isEmpty()) {
                logger.warn("No models returned from API - cannot validate model '{}'", model);
                errors.add("Unable to validate model '" + model + "': No models available from Cursor API");
                return errors;
            }

            logger.debug("Available models: {}", availableModels);
            String trimmedModel = model.trim();

            if (!availableModels.contains(trimmedModel)) {
                errors.add("Model '" + trimmedModel + "' is not available or invalid.");
                errors.add("Available models are:");
                for (String availableModel : availableModels) {
                    errors.add("  - " + availableModel);
                }
                logger.error("Model '{}' not found in available models: {}", trimmedModel, availableModels);
            } else {
                logger.debug("Model '{}' is valid", trimmedModel);
            }

        } catch (Exception e) {
            logger.error("Error validating model '{}': {}", model, e.getMessage(), e);
            errors.add("Error validating model '" + model + "': " + e.getMessage());
        }

        return errors;
    }

    /**
     * Collects all prompts from the workflow, including parallel workflow prompts.
     * Package-private for testing.
     */
    List<PromptInfo> collectAllPrompts(WorkflowData workflowData) {
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

    /**
     * Formats error messages with numbering.
     */
    private List<String> formatErrors(String header, List<String> errors) {
        List<String> formatted = new ArrayList<>();
        formatted.add(header);
        for (int i = 0; i < errors.size(); i++) {
            formatted.add("  " + (i + 1) + ". " + errors.get(i));
        }
        return formatted;
    }
}

