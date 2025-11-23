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
     * Constructor with dependency injection.
     */
    public RunCommand(JobRepository jobRepository, JobProcessor jobProcessor,
                     WorkflowValidator workflowValidator, WorkflowParser workflowParser,
                     PmlValidator pmlValidator, int pollingIntervalSeconds, CLIAgent cliAgent) {
        this.jobRepository = jobRepository;
        this.jobProcessor = jobProcessor;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
        this.cliAgent = cliAgent;

        // Initialize services
        this.jobCreationService = new JobCreationService(jobRepository, workflowValidator,
            workflowParser, pmlValidator, cliAgent);
        this.jobDisplayService = new JobDisplayService(jobRepository);
        this.jobDeletionService = new JobDeletionService(jobRepository, cliAgent);
        this.jobLogDisplayService = new JobLogDisplayService(jobRepository, cliAgent);
        this.completionCheckerFactory = new CompletionCheckerFactory(jobRepository);
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
            logger.info("Job created with ID: {}, starting blocking execution", jobId);
            System.out.println("Job registered");
            System.out.println();

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

            System.out.println("\nThanks for using Churrera! ✨");
            return determineExitCode(execResult);

        } catch (Exception e) {
            logger.error("Error running workflow: {}", e.getMessage(), e);
            System.err.println("Error running workflow: " + e.getMessage());
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
            logger.error("--workflow option was used but no value provided");
            System.err.println("Error: --workflow option requires a value (path to workflow XML file)");
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
     * Prints error messages to stderr.
     */
    private void printErrors(List<String> errors) {
        for (String error : errors) {
            System.err.println(error);
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
            System.out.println("Retrieving available models...");
            System.out.println();

            List<String> models = cliAgent.getModels();

            if (models == null || models.isEmpty()) {
                System.out.println("No models available.");
                logger.warn("No models returned from API");
                return;
            }

            System.out.println("Available models:");
            System.out.println();
            for (int i = 0; i < models.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + models.get(i));
            }
            System.out.println();
            System.out.println("Total: " + models.size() + " model(s)");
            logger.info("Retrieved {} models successfully", models.size());

        } catch (Exception e) {
            logger.error("Error retrieving models: {}", e.getMessage(), e);
            System.err.println("Error retrieving models: " + e.getMessage());
        }
    }

    /**
     * Retrieves and displays available repositories from the Cursor API.
     */
    private void retrieveAndDisplayRepositories() {
        try {
            logger.info("Retrieving available repositories from Cursor API");
            System.out.println("Retrieving available repositories...");
            System.out.println();

            List<String> repositories = cliAgent.getRepositories();

            if (repositories == null || repositories.isEmpty()) {
                System.out.println("No repositories available.");
                logger.warn("No repositories returned from API");
                return;
            }

            System.out.println("Available repositories:");
            System.out.println();
            for (int i = 0; i < repositories.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + repositories.get(i));
            }
            System.out.println();
            System.out.println("Total: " + repositories.size() + " repository(ies)");
            logger.info("Retrieved {} repositories successfully", repositories.size());

        } catch (Exception e) {
            logger.error("Error retrieving repositories: {}", e.getMessage(), e);
            System.err.println("Error retrieving repositories: " + e.getMessage());
        }
    }
}

