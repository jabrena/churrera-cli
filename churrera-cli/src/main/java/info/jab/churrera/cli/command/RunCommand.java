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
import info.jab.churrera.agent.AgentState;
import info.jab.churrera.cli.service.JobProcessor;
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
    mixinStandardHelpOptions = true
)
public class RunCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RunCommand.class);
    private static final int JOB_ID_PREFIX_LENGTH = 8;

    @CommandLine.Parameters(
        index = "0",
        description = "Path to the workflow XML file"
    )
    private String workflowPath;

    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    private final WorkflowValidator workflowValidator;
    private final WorkflowParser workflowParser;
    private final PmlValidator pmlValidator;
    private final int pollingIntervalSeconds;

    /**
     * Constructor with dependency injection.
     */
    public RunCommand(JobRepository jobRepository, JobProcessor jobProcessor,
                     WorkflowValidator workflowValidator, WorkflowParser workflowParser,
                     PmlValidator pmlValidator, int pollingIntervalSeconds) {
        this.jobRepository = jobRepository;
        this.jobProcessor = jobProcessor;
        this.workflowValidator = workflowValidator;
        this.workflowParser = workflowParser;
        this.pmlValidator = pmlValidator;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    @Override
    public void run() {
        logger.info("Running workflow file in blocking mode: {}", workflowPath);

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
                        break;
                    }
                }

                // Sleep for polling interval
                try {
                    Thread.sleep(pollingIntervalSeconds * 1000L);
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

            // Create and save the job with all required fields
            Job job = new Job(
                jobId,
                jobPath,
                null, // cursorAgentId is initially null until launched in Cursor
                model,
                repository,
                AgentState.UNKNOWN,
                now,
                now,
                null, // parentJobId is null for top-level jobs
                null, // result is null initially
                workflowType // workflow type
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

                        timeAgo = String.format("%02d:%02d min", minutes, seconds);
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
}

