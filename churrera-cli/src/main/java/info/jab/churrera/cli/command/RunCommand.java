package info.jab.churrera.cli.command;

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
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.cli.service.CLIAgent;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Command to run a workflow file in a blocking manner with continuous status updates.
 */
@CommandLine.Command(
    name = "run",
    description = "Run a workflow file in blocking mode with status updates",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true
)
public class RunCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RunCommand.class);
    private static final int JOB_ID_PREFIX_LENGTH = 8;

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
        names = "--polling-interval",
        description = "Polling interval in seconds (overrides value from application.properties)"
    )
    private Integer pollingIntervalOverride;

    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    private final WorkflowValidator workflowValidator;
    private final WorkflowParser workflowParser;
    private final PmlValidator pmlValidator;
    private int pollingIntervalSeconds;
    private final CLIAgent cliAgent;

    /**
     * Constructor with dependency injection.
     */
    public RunCommand(JobRepository jobRepository, JobProcessor jobProcessor,
                     WorkflowValidator workflowValidator, WorkflowParser workflowParser,
                     PmlValidator pmlValidator, int pollingIntervalSeconds, CLIAgent cliAgent) {
        this.jobRepository = jobRepository;
        this.jobProcessor = jobProcessor;
        this.workflowValidator = workflowValidator;
        this.workflowParser = workflowParser;
        this.pmlValidator = pmlValidator;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
        this.cliAgent = cliAgent;
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
    public void run() {
        // Handle --retrieve-models option
        if (retrieveModels) {
            retrieveAndDisplayModels();
            return;
        }

        // Handle --retrieve-repositories option
        if (retrieveRepositories) {
            retrieveAndDisplayRepositories();
            return;
        }

        // Validate that if --workflow is used, it must have a value
        if (workflowPath != null && workflowPath.trim().isEmpty()) {
            logger.error("--workflow option was used but no value provided");
            System.err.println("Error: --workflow option requires a value (path to workflow XML file)");
            return;
        }

        logger.info("Running workflow file in blocking mode: {}", workflowPath);

        // Log polling interval being used
        if (pollingIntervalOverride != null) {
            logger.info("Using polling interval from command-line option: {} seconds (overriding application.properties value: {} seconds)",
                pollingIntervalOverride, pollingIntervalSeconds);
        } else {
            logger.info("Using polling interval from application.properties: {} seconds", pollingIntervalSeconds);
        }

        try {
            // Create the job (reusing logic from NewJobRunCommand)
            String jobId = createJob(workflowPath);
            if (jobId == null) {
                logger.error("Failed to create job");
                System.err.println("Error: Failed to create job");
                return;
            }

            logger.info("Job created with ID: {}, starting blocking execution", jobId);
            System.out.println("Job registered");
            System.out.println();

            // Blocking polling loop
            final String finalJobId = jobId; // Make effectively final for lambda
            while (true) {
                // Process the job
                jobProcessor.processJobs();

                // Display filtered jobs table
                displayFilteredJobsTable(finalJobId);

                // Check if job is terminal
                Job job = jobRepository.findById(finalJobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + finalJobId));

                // For parallel workflows, check if parent and all children are terminal
                if (job.type() == WorkflowType.PARALLEL) {
                    // Check if all child jobs are also terminal
                    List<Job> allJobs = jobRepository.findAll();
                    List<Job> childJobs = new ArrayList<>();
                    for (Job j : allJobs) {
                        if (finalJobId.equals(j.parentJobId())) {
                            childJobs.add(j);
                        }
                    }

                    // Parent must be terminal
                    if (job.status().isTerminal()) {
                        // If no child jobs exist, parent completion is sufficient
                        if (childJobs.isEmpty()) {
                            logger.info("Parent job {} reached terminal state with no child jobs", finalJobId);
                            System.out.println("\nJob completed with status: " + job.status());
                            break;
                        }

                        // Check if all child jobs are terminal
                        boolean allChildrenTerminal = true;
                        int activeChildren = 0;
                        for (Job childJob : childJobs) {
                            if (!childJob.status().isTerminal()) {
                                allChildrenTerminal = false;
                                activeChildren++;
                            }
                        }

                        if (allChildrenTerminal) {
                            logger.info("Parent job {} and all {} child jobs reached terminal state", finalJobId, childJobs.size());
                            System.out.println("\nJob completed with status: " + job.status());
                            System.out.println("All " + childJobs.size() + " child jobs completed.");

                            // Delete job and all child jobs if --delete-on-completion is set (regardless of status)
                            if (deleteOnCompletion) {
                                deleteJobAndChildren(finalJobId);
                            }
                            // Delete job and all child jobs if --delete-on-success-completion is set (only on success)
                            else if (deleteOnSuccessCompletion && isJobAndChildrenSuccessful(finalJobId, childJobs)) {
                                deleteJobAndChildren(finalJobId);
                            }
                            break;
                        } else {
                            logger.debug("Parent job {} is terminal but {} of {} child jobs are still active",
                                finalJobId, activeChildren, childJobs.size());
                        }

                    }
                } else {
                    // For non-parallel workflows, just check if the job is terminal
                    if (job.status().isTerminal()) {
                        logger.info("Job {} reached terminal state: {}", finalJobId, job.status());
                        System.out.println("\nJob completed with status: " + job.status());

                        // Delete job if --delete-on-completion is set (regardless of status)
                        if (deleteOnCompletion) {
                            deleteJobAndChildren(finalJobId);
                        }
                        // Delete job if --delete-on-success-completion is set (only on success)
                        else if (deleteOnSuccessCompletion && job.status().isSuccessful()) {
                            deleteJobAndChildren(finalJobId);
                        }
                        break;
                    }
                }

                // Sleep for polling interval
                try {
                    int effectiveInterval = getEffectivePollingIntervalSeconds();
                    Thread.sleep(effectiveInterval * 1000L);
                } catch (InterruptedException e) {
                    logger.warn("Polling interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            System.out.println("\nThanks for using Churrera! ✨");

        } catch (Exception e) {
            logger.error("Error running workflow: {}", e.getMessage(), e);
            System.err.println("Error running workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup is handled by shutdown hook in ChurreraCLI
        }
    }

    /**
     * Creates a new job from the workflow file (reusing logic from NewJobRunCommand).
     *
     * @param jobPath the path to the workflow file
     * @return the created job ID, or null if creation failed
     */
    private String createJob(String jobPath) {
        logger.info("Creating new job from workflow file: {}", jobPath);
        try {
            // Resolve the workflow file path
            Path workflowPath = Paths.get(jobPath);
            File workflowFile = workflowPath.toFile();

            if (!workflowFile.exists()) {
                logger.error("Workflow file does not exist: {}", jobPath);
                System.err.println("Error: Workflow file does not exist: " + jobPath);
                return null;
            }

            logger.debug("Workflow file found: {}", workflowFile.getAbsolutePath());

            // Validate the workflow XML against the XSD schema
            logger.debug("Validating workflow file against XSD schema");
            WorkflowValidator.ValidationResult validationResult = workflowValidator.validate(workflowFile);

            if (!validationResult.isValid()) {
                logger.error("Workflow validation failed: {}", validationResult.getFormattedErrors());
                System.err.println("Workflow validation failed:");
                System.err.println(validationResult.getFormattedErrors());
                return null;
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
                return null;
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
                return null;
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
            return jobId;

        } catch (WorkflowParseException e) {
            logger.error("Error parsing workflow: {}", jobPath, e);
            System.err.println("Error parsing workflow: " + e.getMessage());
            return null;
        } catch (BaseXException e) {
            logger.error("Error creating job: {}", jobPath, e);
            System.err.println("Error creating job: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IOException | QueryException e) {
            logger.error("Error creating job: {}", jobPath, e);
            System.err.println("Error creating job: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Displays a filtered jobs table showing only the specified job and its children (if parallel).
     *
     * @param jobId the job ID to display
     */
    private void displayFilteredJobsTable(String jobId) {
        try {
            // Get the job
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            // Collect jobs to display
            List<Job> jobsToDisplay = new ArrayList<>();
            jobsToDisplay.add(job);

            // If parallel workflow, add child jobs
            if (job.type() == WorkflowType.PARALLEL) {
                List<Job> allJobs = jobRepository.findAll();
                for (Job j : allJobs) {
                    if (jobId.equals(j.parentJobId())) {
                        jobsToDisplay.add(j);
                    }
                }
            }

            // Display table
            if (jobsToDisplay.isEmpty()) {
                System.out.println("No jobs found.");
                return;
            }

            // Prepare table data (reusing logic from JobsCommand)
            String[] headers = {"Job ID", "Parent Job", "Type", "Prompts", "Status", "Last update", "Completed"};
            List<String[]> rows = new ArrayList<>();

            for (Job j : jobsToDisplay) {
                try {
                    List<Prompt> prompts = jobRepository.findPromptsByJobId(j.jobId());
                    int totalPrompts = prompts.size();
                    int completedPrompts = 0;

                    // Count completed prompts
                    for (Prompt prompt : prompts) {
                        if ("COMPLETED".equals(prompt.status()) || "SENT".equals(prompt.status())) {
                            completedPrompts++;
                        }
                    }

                    // Get job status
                    String status = j.status().toString();

                    // Format prompt completion: "completed/total"
                    String promptStatus;
                    if (j.status().isTerminal()) {
                        promptStatus = totalPrompts + "/" + totalPrompts;
                    } else {
                        promptStatus = completedPrompts + "/" + totalPrompts;
                    }

                    // Calculate time display
                    String timeAgo;
                    if (j.status().isTerminal()) {
                        // For terminal jobs, calculate duration from job creation to last update in mm:ss
                        Duration duration = Duration.between(j.createdAt(), j.lastUpdate());
                        long totalSeconds = duration.getSeconds();
                        long minutes = totalSeconds / 60;
                        long seconds = totalSeconds % 60;

                        if (minutes == 0) {
                            timeAgo = String.format("%02d secs", seconds);
                        } else {
                            timeAgo = String.format("%02d:%02d min", minutes, seconds);
                        }
                    } else {
                        // For active jobs, calculate time elapsed since job creation
                        Duration duration = Duration.between(j.createdAt(), LocalDateTime.now());
                        long secondsElapsed = duration.getSeconds();

                        if (secondsElapsed < 60) {
                            timeAgo = "Started " + secondsElapsed + "s ago";
                        } else if (secondsElapsed < 3600) {
                            long minutes = secondsElapsed / 60;
                            timeAgo = "Started " + minutes + (minutes == 1 ? " min ago" : " mins ago");
                        } else {
                            long hours = secondsElapsed / 3600;
                            timeAgo = "Started " + hours + (hours == 1 ? " hour ago" : " hours ago");
                        }
                    }

                    // Format IDs for display (truncate UUIDs to 8 chars)
                    String jobIdDisplay = shortenId(j.jobId());
                    String parentJobDisplay = j.parentJobId() != null ? shortenId(j.parentJobId()) : "NA";

                    // Determine type display
                    String typeDisplay;
                    if (j.type() != null) {
                        typeDisplay = j.type().toString();
                    } else {
                        // Parse workflow file to determine type for legacy jobs
                        try {
                            WorkflowType parsedType = WorkflowParser.determineWorkflowType(new File(j.path()));
                            typeDisplay = parsedType != null ? parsedType.toString() : "Unknown";
                        } catch (Exception e) {
                            typeDisplay = "Unknown";
                        }
                    }

                    // Format last update timestamp as MMddyy HH:mm
                    DateTimeFormatter lastUpdateFormatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
                    String lastUpdateDisplay = j.lastUpdate() != null ? j.lastUpdate().format(lastUpdateFormatter) : "NA";

                    String[] row = {
                        jobIdDisplay,
                        parentJobDisplay,
                        typeDisplay,
                        promptStatus,
                        status,
                        lastUpdateDisplay,
                        timeAgo
                    };
                    rows.add(row);

                } catch (Exception e) {
                    logger.error("Error retrieving details for job {}: {}", j.jobId(), e.getMessage());
                    // Add row with error indication
                    String parentJobDisplay = j.parentJobId() != null ? shortenId(j.parentJobId()) : "NA";
                    String typeDisplay = j.type() != null ? j.type().toString() : "Unknown";
                    String[] row = {
                        shortenId(j.jobId()),
                        parentJobDisplay,
                        typeDisplay,
                        "ERROR",
                        "ERROR",
                        "ERROR",
                        "ERROR"
                    };
                    rows.add(row);
                }
            }

            // Display table to console
            String tableOutput = TableFormatter.formatTable(headers, rows);
            System.out.println(tableOutput);
            logger.debug("Filtered jobs table displayed:\n{}", tableOutput);

        } catch (BaseXException | QueryException e) {
            String errorMessage = "Error displaying jobs: " + e.getMessage();
            System.out.println(errorMessage);
            logger.error("Error displaying jobs: {}", e.getMessage());
        }
    }

    /**
     * Shortens a job ID to 8 characters for display.
     */
    private String shortenId(String id) {
        if (id == null || id.isEmpty()) {
            return "NA";
        }
        return id.length() > JOB_ID_PREFIX_LENGTH ? id.substring(0, JOB_ID_PREFIX_LENGTH) : id;
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

    /**
     * Gets the JobRepository for cleanup purposes.
     *
     * @return the JobRepository instance
     */
    public JobRepository getJobRepository() {
        return jobRepository;
    }

    /**
     * Check if a job and all its child jobs completed successfully.
     *
     * @param jobId the parent job ID
     * @param childJobs the list of child jobs
     * @return true if the parent job and all child jobs are successful, false otherwise
     */
    private boolean isJobAndChildrenSuccessful(String jobId, List<Job> childJobs) {
        try {
            // Check parent job status
            Job parentJob = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            if (!parentJob.status().isSuccessful()) {
                logger.info("Parent job {} did not complete successfully (status: {}), skipping deletion",
                    jobId, parentJob.status());
                return false;
            }

            // Check all child jobs are successful
            for (Job childJob : childJobs) {
                if (!childJob.status().isSuccessful()) {
                    logger.info("Child job {} did not complete successfully (status: {}), skipping deletion",
                        childJob.jobId(), childJob.status());
                    return false;
                }
            }

            logger.info("Parent job {} and all {} child jobs completed successfully", jobId, childJobs.size());
            return true;
        } catch (Exception e) {
            logger.error("Error checking job success status for {}: {}", jobId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete a job and all its child jobs recursively.
     *
     * @param jobId the job ID to delete
     */
    private void deleteJobAndChildren(String jobId) {
        try {
            String reason = deleteOnCompletion ? "--delete-on-completion" : "--delete-on-success-completion";
            logger.info("Deleting job {} and all child jobs ({} enabled)", jobId, reason);
            System.out.println("Deleting job and all child jobs...");

            // Delete child jobs recursively first
            deleteChildJobsRecursively(jobId);

            // Delete the parent job
            var jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isPresent()) {
                deleteJob(jobOpt.get());
                System.out.println("Job and all child jobs deleted successfully");
            }
        } catch (Exception e) {
            logger.error("Error deleting job {}: {}", jobId, e.getMessage(), e);
            System.err.println("Error deleting job: " + e.getMessage());
        }
    }

    /**
     * Recursively delete all child jobs of a parent job.
     *
     * @param parentJobId the parent job ID
     */
    private void deleteChildJobsRecursively(String parentJobId) throws BaseXException, QueryException {
        List<Job> childJobs = jobRepository.findJobsByParentId(parentJobId);

        for (Job childJob : childJobs) {
            // First delete this child's children (depth-first)
            deleteChildJobsRecursively(childJob.jobId());

            // Then delete this child job
            deleteJob(childJob);
            logger.info("Deleted child job: {}", childJob.jobId());
        }
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    /**
     * Delete a single job including its Cursor agent and prompts.
     *
     * @param job the job to delete
     */
    private void deleteJob(Job job) throws BaseXException, QueryException {
        // Delete Cursor agent if it exists
        if (job.cursorAgentId() != null) {
            try {
                cliAgent.deleteAgent(job.cursorAgentId());
                logger.info("Deleted Cursor agent for job {}: {}", job.jobId(), job.cursorAgentId());
            } catch (Exception e) {
                logger.error("Failed to delete Cursor agent {} for job {}: {}",
                        job.cursorAgentId(), job.jobId(), e.getMessage());
                System.err.println("  ⚠️  Failed to delete Cursor agent for job " + job.jobId() + ": " + e.getMessage());
                // Continue with database deletion even if Cursor API fails
            }
        }

        // Delete all prompts from database
        jobRepository.deletePromptsByJobId(job.jobId());

        // Delete the job from database
        jobRepository.deleteById(job.jobId());
    }
}

