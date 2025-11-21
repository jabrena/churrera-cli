package info.jab.churrera.cli.command.cli;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowValidator;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.util.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI subcommand that provides an interactive REPL for managing jobs.
 */
@CommandLine.Command(
    name = "cli",
    description = "Start interactive REPL mode for managing jobs",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true
)
public class CliCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CliCommand.class);

    private static final Pattern JOB_STATUS_PATTERN = Pattern.compile("^jobs\\s+status\\s+(\\S+)$");
    private static final Pattern JOB_LOGS_PATTERN = Pattern.compile("^jobs\\s+logs\\s+(\\S+)$");
    private static final Pattern JOB_NEW_PATTERN = Pattern.compile("^jobs\\s+new\\s+([^\n]{1,500})$");
    private static final Pattern JOB_DELETE_PATTERN = Pattern.compile("^jobs\\s+delete\\s+(\\S+)$");
    private static final Pattern JOB_PR_PATTERN = Pattern.compile("^jobs\\s+pr\\s+(\\S+)$");
    
    private static final int CLEAR_SCREEN_FALLBACK_LINES = 50;

    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    private final PropertyResolver propertyResolver;
    private final Scanner scanner;
    private final CLIAgent cliAgent;
    private ScheduledExecutorService scheduledExecutor;

    public CliCommand(JobRepository jobRepository, JobProcessor jobProcessor,
                              PropertyResolver propertyResolver, Scanner scanner, CLIAgent cliAgent) {
        this.jobRepository = jobRepository;
        this.jobProcessor = jobProcessor;
        this.propertyResolver = propertyResolver;
        this.scanner = scanner;
        this.cliAgent = cliAgent;
    }

    @Override
    public void run() {
        logger.info("Starting Churrera CLI REPL");
        System.out.println("Type 'help' for available commands, 'quit' to finish the application");
        System.out.println();

        // Read polling interval from properties
        int pollingIntervalSeconds = propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds")
                .map(Integer::parseInt)
                .orElseThrow(() -> new RuntimeException("Required property 'cli.polling.interval.seconds' not found in application.properties"));

        // Start background processor using ScheduledExecutorService
        scheduledExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("job-processor");
            return t;
        });

        logger.info("Starting job processor with polling interval: {} seconds", pollingIntervalSeconds);

        // Schedule job processing at fixed intervals
        scheduledExecutor.scheduleWithFixedDelay(
            () -> {
                try {
                    jobProcessor.processJobs();
                } catch (Exception e) {
                    logger.error("Unexpected error in job processor: {}", e.getMessage(), e);
                }
            },
            0, // Initial delay: start immediately
            pollingIntervalSeconds,
            TimeUnit.SECONDS
        );

        String prompt = propertyResolver.getProperty("application.properties", "cli.prompt")
                    .orElseThrow(() -> new RuntimeException("Required property 'cli.prompt' not found in application.properties"))
                    .trim();

        try {
            while (true) {
                System.out.print(prompt);
                System.out.flush(); // Ensure prompt is visible/flushed
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                if (isExitCommand(input)) {
                    logger.info("Exit command received, shutting down");
                    System.out.println("Goodbye!");
                    System.out.println("\nShutting down...");
                    System.out.println("\nThanks for using Churrera! ✨");
                    break;
                }

                try {
                    executeCommand(input);
                } catch (Exception e) {
                    logger.error("Error executing command: {}", e.getMessage(), e);
                    System.err.println("Error executing command: " + e.getMessage());
                }
            }
        } finally {
            // Shutdown the scheduled executor gracefully
            if (scheduledExecutor != null) {
                scheduledExecutor.shutdown();
                try {
                    if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduledExecutor.shutdownNow();
                        if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                            logger.error("Job processor did not terminate");
                            System.err.println("Job processor did not terminate");
                        }
                    }
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while shutting down job processor: {}", e.getMessage());
                    scheduledExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void executeCommand(String input) throws Exception {
        logger.debug("Executing command: {}", input);

        // Handle exact command matches first
        if (handleExactCommands(input)) {
            return;
        }

        // Handle pattern-based commands
        if (handlePatternCommands(input)) {
            return;
        }

        // If no command matches, show error
        logger.debug("Unknown command received: {}", input);
        System.out.println("Unknown command: " + input);
        System.out.println("Type 'help' for available commands.");
    }

    private boolean handleExactCommands(String input) throws Exception {
        if (input.equals("jobs")) {
            new JobsCommand(jobRepository).run();
            return true;
        }
        if (input.equals("help")) {
            printHelp();
            return true;
        }
        if (input.equals("clear")) {
            clearScreen();
            return true;
        }
        return false;
    }

    private boolean handlePatternCommands(String input) throws Exception {
        Matcher newMatcher = JOB_NEW_PATTERN.matcher(input);
        if (newMatcher.matches()) {
            String jobPath = newMatcher.group(1).trim();
            new NewJobRunCommand(jobRepository, jobPath, new WorkflowValidator(), new WorkflowParser(), new PmlValidator()).run();
            return true;
        }

        Matcher deleteMatcher = JOB_DELETE_PATTERN.matcher(input);
        if (deleteMatcher.matches()) {
            String jobId = deleteMatcher.group(1).trim();
            new DeleteJobCommand(jobRepository, cliAgent, jobId).run();
            return true;
        }

        Matcher statusMatcher = JOB_STATUS_PATTERN.matcher(input);
        if (statusMatcher.matches()) {
            String jobId = statusMatcher.group(1).trim();
            new JobStatusCommand(jobRepository, cliAgent, jobId).run();
            return true;
        }

        Matcher logsMatcher = JOB_LOGS_PATTERN.matcher(input);
        if (logsMatcher.matches()) {
            String jobId = logsMatcher.group(1).trim();
            new JobLogsCommand(jobRepository, cliAgent, jobId).run();
            return true;
        }

        Matcher prMatcher = JOB_PR_PATTERN.matcher(input);
        if (prMatcher.matches()) {
            String jobId = prMatcher.group(1).trim();
            new JobsPrCommand(jobRepository, jobId).run();
            return true;
        }

        return false;
    }

    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  jobs                    - List all jobs");
        System.out.println("  jobs new {path}         - Create a new job with specified path");
        System.out.println("  jobs status {uuid}     - Show details for specific job ID");
        System.out.println("  jobs logs {uuid}        - Show logs for specific job ID");
        System.out.println("  jobs pr {uuid}          - Show the PR link for a finished job");
        System.out.println("  jobs delete {uuid}      - Delete a job by UUID (cascade deletes child jobs)");
        System.out.println("  help                    - Display this help message");
        System.out.println("  clear                   - Clear the terminal screen");
        System.out.println("  quit                    - Exit the application");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  jobs");
        System.out.println("  jobs new churrera-cli/src/test/resources/examples/hello-world/workflow-hello-world.xml");
        System.out.println("  jobs status a90e4050");
        System.out.println("  jobs logs a90e4050");
        System.out.println("  jobs pr a90e4050");
        System.out.println("  jobs delete a90e4050");
    }

    private boolean isExitCommand(String input) {
        return input.equalsIgnoreCase("quit");
    }

    private void clearScreen() {
        try {
            // Clear screen for Unix/Linux/macOS
            System.out.print("\033[H\033[2J");
            System.out.flush();
        } catch (Exception e) {
            // Fallback: print newlines to clear most of the screen
            for (int i = 0; i < CLEAR_SCREEN_FALLBACK_LINES; i++) {
                System.out.println();
            }
        }
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public JobRepository getJobRepository() {
        return jobRepository;
    }
}

