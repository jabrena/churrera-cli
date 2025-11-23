package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to run a workflow file in a blocking manner with continuous status updates.
 */
@CommandLine.Command(
    name = "run",
    description = "Run a workflow file in blocking mode with status updates",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true
)
public class RunCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(RunCommand.class);

    @CommandLine.Option(
        names = "--workflow",
        description = "Path to the workflow XML file"
    )
    private String workflowPath;

    @CommandLine.Option(
        names = "--delete-on-completion",
        description = "Delete the job and all child jobs when the workflow completes"
    )
    private boolean deleteOnCompletion;

    @CommandLine.Option(
        names = "--delete-on-success-completion",
        description = "Delete the job and all child jobs only when the workflow completes successfully"
    )
    private boolean deleteOnSuccessCompletion;

    @CommandLine.Option(
        names = "--retrieve-models",
        description = "Retrieve and display available models from Cursor API"
    )
    private boolean retrieveModels;

    @CommandLine.Option(
        names = "--retrieve-repositories",
        description = "Retrieve and display available repositories from Cursor API"
    )
    private boolean retrieveRepositories;

    @CommandLine.Option(
        names = "--show-logs",
        description = "Display agent conversation logs before deletion"
    )
    private boolean showLogs;

    @CommandLine.Option(
        names = "--polling-interval",
        description = "Polling interval in seconds (overrides value from application.properties)"
    )
    private Integer pollingIntervalOverride;

    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    private final int pollingIntervalSeconds;
    private final CLIAgent cliAgent;

    // Services
    private final JobCreationService jobCreationService;
    private final JobDisplayService jobDisplayService;
    private final JobDeletionService jobDeletionService;
    private final JobLogDisplayService jobLogDisplayService;
    private final CompletionCheckerFactory completionCheckerFactory;

    /**
     * Constructor with Dagger dependency injection.
     */
    @Inject
    public RunCommand(
            JobRepository jobRepository,
            JobProcessor jobProcessor,
            @Named("pollingIntervalSeconds") int pollingIntervalSeconds,
            CLIAgent cliAgent,
            JobCreationService jobCreationService,
            JobDisplayService jobDisplayService,
            JobDeletionService jobDeletionService,
            JobLogDisplayService jobLogDisplayService,
            CompletionCheckerFactory completionCheckerFactory) {
        this.jobRepository = jobRepository;
        this.jobProcessor = jobProcessor;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
        this.cliAgent = cliAgent;
        this.jobCreationService = jobCreationService;
        this.jobDisplayService = jobDisplayService;
        this.jobDeletionService = jobDeletionService;
        this.jobLogDisplayService = jobLogDisplayService;
        this.completionCheckerFactory = completionCheckerFactory;
    }

    /**
     * Gets the effective polling interval in seconds.
     * Returns the command-line option value if provided, otherwise returns the value from application.properties.
     *
     * @return the polling interval in seconds
     */
    private int getEffectivePollingIntervalSeconds() {
        return pollingIntervalOverride != null ? pollingIntervalOverride : pollingIntervalSeconds;
    }

    @Override
    public Integer call() {
        // Handle early return options
        Integer earlyReturn = handleEarlyReturnOptions();
        if (earlyReturn != null) {
            return earlyReturn;
        }

        // Validate workflow path
        if (!validateWorkflowPath()) {
            return 1;
        }

        logger.info("Running workflow file in blocking mode: {}", workflowPath);
        logPollingInterval();

        try {
            // Create job
            JobCreationResult creationResult = jobCreationService.createJob(workflowPath);
            if (!creationResult.isSuccess()) {
                logger.error("Failed to create job");
                printErrors(creationResult.getErrors());
                return 1;
            }

            String jobId = creationResult.getJobId();
            logger.debug("Job created with ID: {}, starting blocking execution", jobId);
            logger.info("Job registered");

            // Create polling service with effective polling interval
            int effectivePollingInterval = getEffectivePollingIntervalSeconds();
            JobPollingService pollingService = new JobPollingService(jobProcessor, jobRepository,
                jobDisplayService, completionCheckerFactory, effectivePollingInterval);

            // Execute blocking polling loop
            ExecutionResult execResult = pollingService.executePollingLoop(jobId);

            // Display logs before deletion if requested
            if (showLogs && execResult.getFinalStatus() != null) {
                Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

                // Display logs for main job
                jobLogDisplayService.displayLogsForJob(job);

                // Display logs for child jobs if any
                for (Job childJob : execResult.getChildJobs()) {
                    jobLogDisplayService.displayLogsForJob(childJob);
                }
            }

            // Handle job deletion if needed
            if (execResult.getFinalStatus() != null) {
                Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
                jobDeletionService.handleDeletion(jobId, job, execResult.getChildJobs(),
                    deleteOnCompletion, deleteOnSuccessCompletion);
            }

            logger.info("Thanks for using Churrera! ✨");
            return determineExitCode(execResult);

        } catch (Exception e) {
            logger.error("Error running workflow: {}", e.getMessage(), e);
            return 1;
        } finally {
            // Cleanup is handled by shutdown hook in ChurreraCLI
        }
    }

    /**
     * Handles early return options like --retrieve-models and --retrieve-repositories.
     *
     * @return exit code if early return should occur, null otherwise
     */
    private Integer handleEarlyReturnOptions() {
        if (retrieveModels) {
            retrieveAndDisplayModels();
            return 0;
        }

        if (retrieveRepositories) {
            retrieveAndDisplayRepositories();
            return 0;
        }

        return null;
    }

    /**
     * Validates the workflow path option.
     *
     * @return true if valid, false otherwise
     */
    private boolean validateWorkflowPath() {
        if (workflowPath != null && workflowPath.trim().isEmpty()) {
            logger.error("--workflow option requires a value (path to workflow XML file)");
            return false;
        }
        return true;
    }

    /**
     * Logs the polling interval being used.
     */
    private void logPollingInterval() {
        if (pollingIntervalOverride != null) {
            logger.info("Using polling interval from command-line option: {} seconds (overriding application.properties value: {} seconds)",
                pollingIntervalOverride, pollingIntervalSeconds);
        } else {
            logger.info("Using polling interval from application.properties: {} seconds", pollingIntervalSeconds);
        }
    }

    /**
     * Determines the exit code based on execution result.
     *
     * @param result the execution result
     * @return exit code (0 for success, 1 for failure)
     */
    private int determineExitCode(ExecutionResult result) {
        if (result.isInterrupted()) {
            logger.warn("Job execution was interrupted by signal");
            return 1;
        }

        AgentState finalStatus = result.getFinalStatus();
        if (finalStatus == null) {
            logger.error("Job completed but final status is unknown");
            return 1;
        }

        if (finalStatus.isSuccessful()) {
            logger.info("Job completed successfully with status: {}", finalStatus);
            return 0;
        } else if (finalStatus.isFailed()) {
            logger.info("Job completed with failure status: {}", finalStatus);
            return 1;
        } else {
            logger.warn("Job completed with unexpected terminal status: {}", finalStatus);
            return 1;
        }
    }

    /**
     * Prints error messages to logger.
     */
    private void printErrors(List<String> errors) {
        for (String error : errors) {
            logger.error(error);
        }
    }

    /**
     * Gets the JobRepository for cleanup purposes.
     *
     * @return the JobRepository instance
     */
    public JobRepository getJobRepository() {
        return jobRepository;
    }

    /**
     * Retrieves and displays available models from the Cursor API.
     */
    private void retrieveAndDisplayModels() {
        try {
            logger.info("Retrieving available models from Cursor API");

            List<String> models = cliAgent.getModels();

            if (models == null || models.isEmpty()) {
                logger.warn("No models returned from API");
                return;
            }

            StringBuilder modelsList = new StringBuilder("Available models:\n");
            for (int i = 0; i < models.size(); i++) {
                modelsList.append("  ").append(i + 1).append(". ").append(models.get(i)).append("\n");
            }
            modelsList.append("Total: ").append(models.size()).append(" model(s)");
            logger.info("Retrieved {} models successfully:\n{}", models.size(), modelsList);

        } catch (Exception e) {
            logger.error("Error retrieving models: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves and displays available repositories from the Cursor API.
     */
    private void retrieveAndDisplayRepositories() {
        try {
            logger.info("Retrieving available repositories from Cursor API");

            List<String> repositories = cliAgent.getRepositories();

            if (repositories == null || repositories.isEmpty()) {
                logger.warn("No repositories returned from API");
                return;
            }

            StringBuilder reposList = new StringBuilder("Available repositories:\n");
            for (int i = 0; i < repositories.size(); i++) {
                reposList.append("  ").append(i + 1).append(". ").append(repositories.get(i)).append("\n");
            }
            reposList.append("Total: ").append(repositories.size()).append(" repository(ies)");
            logger.info("Retrieved {} repositories successfully:\n{}", repositories.size(), reposList);

        } catch (Exception e) {
            logger.error("Error retrieving repositories: {}", e.getMessage(), e);
        }
    }
}

