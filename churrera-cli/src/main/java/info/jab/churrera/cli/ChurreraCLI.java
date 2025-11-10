package info.jab.churrera.cli;

import info.jab.churrera.cli.commands.DeleteJobCommand;
import info.jab.churrera.cli.commands.HelpCommand;
import info.jab.churrera.cli.commands.JobLogsCommand;
import info.jab.churrera.cli.commands.JobStatusCommand;
import info.jab.churrera.cli.commands.JobsCommand;
import info.jab.churrera.cli.commands.JobsPrCommand;
import info.jab.churrera.cli.commands.NewJobRunCommand;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowValidator;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.util.CursorApiKeyResolver;
import info.jab.churrera.util.PropertyResolver;
import info.jab.churrera.util.PmlConverter;
import info.jab.cursor.CursorAgentManagementImpl;
import info.jab.cursor.CursorAgentInformationImpl;
import org.basex.core.BaseXException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import info.jab.churrera.cli.util.GitInfo;
import com.diogonunes.jcolor.Attribute;
import static com.diogonunes.jcolor.Ansi.colorize;
import com.github.lalyos.jfiglet.FigletFont;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main CLI application for churrera.
 * Provides an interactive REPL for managing jobs stored in BaseX.
 */
@CommandLine.Command(
    name = "churrera",
    description = "Churrera CLI",
    version = "0.1.0"
)
public class ChurreraCLI implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ChurreraCLI.class);

    private static final Pattern JOB_STATUS_PATTERN = Pattern.compile("^jobs\\s+status\\s+(.+)$");
    private static final Pattern JOB_LOGS_PATTERN = Pattern.compile("^jobs\\s+logs\\s+(.+)$");
    private static final Pattern JOB_NEW_PATTERN = Pattern.compile("^jobs\\s+new\\s+(.+)$");
    private static final Pattern JOB_DELETE_PATTERN = Pattern.compile("^jobs\\s+delete\\s+(.+)$");
    private static final Pattern JOB_PR_PATTERN = Pattern.compile("^jobs\\s+pr\\s+(.+)$");

    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    private final PropertyResolver propertyResolver;
    private final Scanner scanner;
    private final CLIAgent cliAgent;
    private ScheduledExecutorService scheduledExecutor;

    public ChurreraCLI() throws IOException, BaseXException {
        printBanner();

        // Validate API key at startup
        CursorApiKeyResolver apiKeyResolver = new CursorApiKeyResolver();
        String apiKey = apiKeyResolver.resolveApiKey();
        logger.info("CURSOR_API_KEY validated");
        System.out.println("✓ CURSOR_API_KEY validated");

        logger.info("Initializing Churrera CLI");

        this.propertyResolver = new PropertyResolver();
        this.jobRepository = new JobRepository(propertyResolver);
        logger.debug("JobRepository initialized");

        // Create CLIAgent with dependencies
        String apiBaseUrl = "https://api.cursor.com";
        this.cliAgent = new CLIAgent(
            jobRepository,
            new CursorAgentManagementImpl(apiKey, apiBaseUrl),
            new CursorAgentInformationImpl(apiKey, apiBaseUrl),
            new PmlConverter(),
            propertyResolver
        );

        // Create WorkflowParser
        WorkflowParser workflowParser = new WorkflowParser();

        // Read polling interval from properties
        int pollingIntervalSeconds = propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds")
                .map(Integer::parseInt)
                .orElseThrow(() -> new RuntimeException("Required property 'cli.polling.interval.seconds' not found in application.properties"));
        this.jobProcessor = new JobProcessor(jobRepository, cliAgent, workflowParser, pollingIntervalSeconds);
        this.scanner = new Scanner(System.in);

        // Initialize the repository
        jobRepository.initialize();
        logger.info("Churrera CLI initialized successfully");
    }

    // Package-private constructor for testing
    ChurreraCLI(JobRepository jobRepository, JobProcessor jobProcessor, PropertyResolver propertyResolver, Scanner scanner, CLIAgent cliAgent) {
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
        if (input.equals("jobs")) {
            new JobsCommand(jobRepository).run();
            return;
        }

        if (input.equals("help")) {
            new HelpCommand().run();
            return;
        }

        if (input.equals("clear")) {
            clearScreen();
            return;
        }

        // Check for jobs new {path} pattern
        Matcher newMatcher = JOB_NEW_PATTERN.matcher(input);
        if (newMatcher.matches()) {
            String jobPath = newMatcher.group(1).trim();
            new NewJobRunCommand(jobRepository, jobPath, new WorkflowValidator(), new WorkflowParser(), new PmlValidator()).run();
            return;
        }

        // Check for jobs delete {uuid} pattern
        Matcher deleteMatcher = JOB_DELETE_PATTERN.matcher(input);
        if (deleteMatcher.matches()) {
            String jobId = deleteMatcher.group(1).trim();
            new DeleteJobCommand(jobRepository, cliAgent, jobId).run();
            return;
        }

        // Check for jobs status {uuid} pattern
        Matcher statusMatcher = JOB_STATUS_PATTERN.matcher(input);
        if (statusMatcher.matches()) {
            String jobId = statusMatcher.group(1).trim();
            new JobStatusCommand(jobRepository, cliAgent, jobId).run();
            return;
        }

        // Check for jobs logs {uuid} pattern
        Matcher logsMatcher = JOB_LOGS_PATTERN.matcher(input);
        if (logsMatcher.matches()) {
            String jobId = logsMatcher.group(1).trim();
            new JobLogsCommand(jobRepository, cliAgent, jobId).run();
            return;
        }

        // Check for jobs pr {uuid} pattern
        Matcher prMatcher = JOB_PR_PATTERN.matcher(input);
        if (prMatcher.matches()) {
            String jobId = prMatcher.group(1).trim();
            new JobsPrCommand(jobRepository, jobId).run();
            return;
        }

        // If no command matches, show error
        logger.debug("Unknown command received: {}", input);
        System.out.println("Unknown command: " + input);
        System.out.println("Type 'help' for available commands.");
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
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    /**
     * Main entry point for the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            ChurreraCLI cli = new ChurreraCLI();

            // Add shutdown hook to ensure proper cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown hook triggered");
                System.out.println("\nShutting down...");
                System.out.println("\n✨ Thanks for using Churrera!");
                if (cli.scheduledExecutor != null) {
                    cli.scheduledExecutor.shutdownNow();
                }
                cli.jobRepository.close();
            }));

            logger.info("Starting Churrera CLI application");
            cli.run();
            logger.info("Churrera CLI application terminated normally");

        } catch (Exception e) {
            logger.error("Failed to start Churrera CLI: {}", e.getMessage(), e);
            System.err.println("Failed to start Churrera CLI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printBanner() {
        try {
            logger.debug("Printing application banner");
            System.out.println();
            String asciiArt = FigletFont.convertOneLine("Churrera CLI");
            System.out.println(colorize(asciiArt, Attribute.GREEN_TEXT()));
            new GitInfo().print();
        } catch (IOException e) {
            logger.error("Error printing banner: {}", e.getMessage(), e);
            System.out.println("Error printing banner: " + e.getMessage());
        }
    }
}
