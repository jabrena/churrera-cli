package info.jab.churrera.cli;

import info.jab.churrera.cli.command.ChurreraCLICommand;
import info.jab.churrera.cli.command.RunCommand;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.util.CursorApiKeyResolver;
import info.jab.churrera.util.PropertyResolver;
import info.jab.churrera.util.PmlConverter;
import info.jab.churrera.workflow.WorkflowValidator;
import info.jab.churrera.workflow.PmlValidator;
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

/**
 * Main CLI application for churrera.
 * Root command that provides subcommands for managing jobs stored in BaseX.
 */
@CommandLine.Command(
    name = "churrera",
    mixinStandardHelpOptions = true,
    subcommands = {ChurreraCLICommand.class, RunCommand.class}
)
public class ChurreraCLI implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ChurreraCLI.class);

    public ChurreraCLI() {
        // Empty constructor for picocli
    }

    /**
     * Creates and initializes the CLI command with all required dependencies.
     * This method is used by the CLI subcommand to get initialized components.
     */
    static ChurreraCLICommand createCLICommand() throws IOException, BaseXException {
        // Validate API key at startup
        CursorApiKeyResolver apiKeyResolver = new CursorApiKeyResolver();
        String apiKey = apiKeyResolver.resolveApiKey();
        logger.info("CURSOR_API_KEY validated");
        System.out.println("✓ CURSOR_API_KEY validated");

        logger.info("Initializing Churrera CLI");

        PropertyResolver propertyResolver = new PropertyResolver();
        JobRepository jobRepository = new JobRepository(propertyResolver);
        logger.debug("JobRepository initialized");

        // Create CLIAgent with dependencies
        String apiBaseUrl = "https://api.cursor.com";
        CLIAgent cliAgent = new CLIAgent(
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
        JobProcessor jobProcessor = new JobProcessor(jobRepository, cliAgent, workflowParser, pollingIntervalSeconds);
        Scanner scanner = new Scanner(System.in);

        // Initialize the repository
        jobRepository.initialize();
        logger.info("Churrera CLI initialized successfully");

        return new ChurreraCLICommand(jobRepository, jobProcessor, propertyResolver, scanner, cliAgent);
    }

    /**
     * Creates and initializes the Run command with all required dependencies.
     * This method is used by the Run subcommand to get initialized components.
     */
    static RunCommand createRunCommand() throws IOException, BaseXException {
        // Validate API key at startup
        CursorApiKeyResolver apiKeyResolver = new CursorApiKeyResolver();
        String apiKey = apiKeyResolver.resolveApiKey();
        logger.info("CURSOR_API_KEY validated");
        System.out.println("✓ CURSOR_API_KEY validated");
        System.out.println();

        logger.info("Initializing Churrera Run command");

        PropertyResolver propertyResolver = new PropertyResolver();
        JobRepository jobRepository = new JobRepository(propertyResolver);
        logger.debug("JobRepository initialized");

        // Create CLIAgent with dependencies
        String apiBaseUrl = "https://api.cursor.com";
        CLIAgent cliAgent = new CLIAgent(
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
        JobProcessor jobProcessor = new JobProcessor(jobRepository, cliAgent, workflowParser, pollingIntervalSeconds);

        // Create validators
        WorkflowValidator workflowValidator = new WorkflowValidator();
        PmlValidator pmlValidator = new PmlValidator();

        // Initialize the repository
        jobRepository.initialize();
        logger.info("Churrera Run command initialized successfully");

        return new RunCommand(jobRepository, jobProcessor, workflowValidator, workflowParser, pmlValidator, pollingIntervalSeconds);
    }

    @Override
    public void run() {
        // Root command without subcommand - show custom message
        System.out.println("Please specify a command. Use 'churrera --help' for available commands.");
    }

    /**
     * Main entry point for the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Print banner first in all cases
        printBanner();

        try {
            ChurreraCLI cli = new ChurreraCLI();

            // Store references for shutdown hook
            final ChurreraCLICommand[] cliCommandRef = new ChurreraCLICommand[1];
            final RunCommand[] runCommandRef = new RunCommand[1];

            // Set up factory for creating subcommands
            CommandLine.IFactory factory = new CommandLine.IFactory() {
                @Override
                public <T> T create(Class<T> cls) throws Exception {
                    if (cls == ChurreraCLICommand.class) {
                        @SuppressWarnings("unchecked")
                        T cmd = (T) createCLICommand();
                        cliCommandRef[0] = (ChurreraCLICommand) cmd; // Store reference for shutdown hook
                        return cmd;
                    }
                    if (cls == RunCommand.class) {
                        @SuppressWarnings("unchecked")
                        T cmd = (T) createRunCommand();
                        runCommandRef[0] = (RunCommand) cmd; // Store reference for shutdown hook
                        return cmd;
                    }
                    // Default: try to instantiate using default constructor
                    return cls.getDeclaredConstructor().newInstance();
                }
            };
            CommandLine commandLine = new CommandLine(cli, factory);

            // Add shutdown hook to ensure proper cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown hook triggered");
                if (cliCommandRef[0] != null) {
                    if (cliCommandRef[0].getScheduledExecutor() != null) {
                        cliCommandRef[0].getScheduledExecutor().shutdownNow();
                    }
                    if (cliCommandRef[0].getJobRepository() != null) {
                        cliCommandRef[0].getJobRepository().close();
                    }
                }
                // Cleanup for RunCommand
                if (runCommandRef[0] != null) {
                    if (runCommandRef[0].getJobRepository() != null) {
                        runCommandRef[0].getJobRepository().close();
                    }
                }
            }));

            logger.info("Starting Churrera CLI application");
            int exitCode = commandLine.execute(args);

            logger.info("Churrera CLI application terminated normally");
            System.exit(exitCode);

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
