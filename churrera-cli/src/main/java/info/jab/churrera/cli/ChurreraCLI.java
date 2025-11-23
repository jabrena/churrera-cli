package info.jab.churrera.cli;

import info.jab.churrera.cli.command.run.RunCommand;
import info.jab.churrera.cli.di.ChurreraComponent;
import info.jab.churrera.cli.di.DaggerChurreraComponent;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.util.PropertyResolver;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowValidator;
import info.jab.churrera.workflow.PmlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import info.jab.churrera.cli.util.GitInfo;

import javax.inject.Inject;
import java.util.function.Supplier;

/**
 * Main CLI application for churrera.
 * Root command that provides subcommands for managing jobs stored in BaseX.
 */
@CommandLine.Command(
    name = "churrera",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true
)
public class ChurreraCLI implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ChurreraCLI.class);

    // Dependencies
    final PropertyResolver propertyResolver;
    final JobRepository jobRepository;
    final CLIAgent cliAgent;
    private final WorkflowParser workflowParser;
    final JobProcessor jobProcessor;
    final WorkflowValidator workflowValidator;
    final PmlValidator pmlValidator;

    /**
     * Constructor with Dagger dependency injection.
     */
    @Inject
    public ChurreraCLI(
            PropertyResolver propertyResolver,
            JobRepository jobRepository,
            CLIAgent cliAgent,
            WorkflowParser workflowParser,
            JobProcessor jobProcessor,
            WorkflowValidator workflowValidator,
            PmlValidator pmlValidator) {
        this.propertyResolver = propertyResolver;
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
        this.workflowParser = workflowParser;
        this.jobProcessor = jobProcessor;
        this.workflowValidator = workflowValidator;
        this.pmlValidator = pmlValidator;
        logger.debug("ChurreraCLI initialized with Dagger");
    }

    /**
     * Constructor for testing that accepts all dependencies.
     * Package-private for testing purposes.
     */
    ChurreraCLI(
            PropertyResolver propertyResolver,
            JobRepository jobRepository,
            CLIAgent cliAgent,
            WorkflowParser workflowParser,
            JobProcessor jobProcessor,
            WorkflowValidator workflowValidator,
            PmlValidator pmlValidator,
            boolean testConstructor) {
        this.propertyResolver = propertyResolver;
        this.jobRepository = jobRepository;
        this.cliAgent = cliAgent;
        this.workflowParser = workflowParser;
        this.jobProcessor = jobProcessor;
        this.workflowValidator = workflowValidator;
        this.pmlValidator = pmlValidator;
    }

    @Override
    public void run() {
        // Root command without subcommand - show custom message
        logger.info("Please specify a command. Use 'churrera --help' for available commands.");
    }

    /**
     * Main entry point for the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Print banner first in all cases
        printBanner(GitInfo::new);

        try {
            // Create Dagger component
            ChurreraComponent component = DaggerChurreraComponent.builder()
                    .build();

            // Get ChurreraCLI instance from Dagger
            final ChurreraCLI cli = component.churreraCLI();

            // Create CommandLine with root command
            CommandLine commandLine = new CommandLine(cli);

            // Get RunCommand from Dagger and register as subcommand
            final RunCommand runCommand = component.runCommand();

            commandLine.addSubcommand("run", runCommand);

            // Add shutdown hook to ensure proper cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.trace("Shutdown hook triggered");
                // Cleanup for RunCommand
                if (runCommand != null && runCommand.getJobRepository() != null) {
                    runCommand.getJobRepository().close();
                }
            }));

            int exitCode = commandLine.execute(args);

            System.exit(exitCode);

        } catch (Exception e) {
            logger.error("Failed to start Churrera CLI: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Prints the application banner.
     * Package-private method for testing with injected GitInfo supplier.
     *
     * @param gitInfoSupplier Supplier for creating GitInfo instance
     */
    static void printBanner(Supplier<GitInfo> gitInfoSupplier) {
        try {
            logger.info("Running Churrera");
            gitInfoSupplier.get().print();
        } catch (RuntimeException e) {
            logger.error("Error printing banner: {}", e.getMessage(), e);
        }
    }

}
