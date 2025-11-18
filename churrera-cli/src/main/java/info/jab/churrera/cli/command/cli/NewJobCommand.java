package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.cli.service.CLIAgent;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Command to create a new job.
 */
@CommandLine.Command(
    name = "jobs/new",
    description = "Create a new job"
)
public class NewJobCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NewJobCommand.class);

    private final JobRepository jobRepository;
    private final Scanner scanner;
    private final CLIAgent cliAgent;

    public NewJobCommand(JobRepository jobRepository, Scanner scanner, CLIAgent cliAgent) {
        this.jobRepository = jobRepository;
        this.scanner = scanner;
        this.cliAgent = cliAgent;
    }

    @Override
    public void run() {
        try {
            logger.info("Enter job path: ");
            String path = scanner.nextLine().trim();

            if (path.isEmpty()) {
                logger.info("Job path cannot be empty.");
                return;
            }

            logger.info("Enter model: ");
            String model = scanner.nextLine().trim();

            if (model.isEmpty()) {
                model = "default-model";
            }

            // Validate model against available models from API
            logger.debug("Validating model against available models");
            List<String> modelValidationErrors = validateModel(model);
            if (!modelValidationErrors.isEmpty()) {
                logger.error("Model validation failed: {} errors found", modelValidationErrors.size());
                System.err.println("Model validation failed:");
                for (int i = 0; i < modelValidationErrors.size(); i++) {
                    System.err.println("  " + (i + 1) + ". " + modelValidationErrors.get(i));
                }
                return;
            }

            logger.debug("Model validation passed");

            logger.info("Enter repository: ");
            String repository = scanner.nextLine().trim();

            if (repository.isEmpty()) {
                repository = "default-repository";
            }

            String jobId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            // Determine workflow type
            WorkflowType workflowType = WorkflowParser.determineWorkflowType(new File(path));

            Job job = new Job(jobId, path, null, model, repository, AgentState.CREATING(), now, now, null, null, workflowType, null, null, null, null);
            jobRepository.save(job);

            logger.info("Job created with ID: {}", jobId);
            logger.info("  Path: {}", path);
            logger.info("  Created: {}", now);

        } catch (IOException | QueryException e) {
            logger.error("Error creating job: {}", e.getMessage());
        }
    }

    /**
     * Validates the model specified by the user against available models from the Cursor API.
     *
     * @param model the model name from the user input (should not be null or empty at this point)
     * @return list of validation error messages (empty if model is valid)
     */
    private List<String> validateModel(String model) {
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
}
