package info.jab.churrera.cli;

import info.jab.churrera.cli.command.run.RunCommand;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.util.CursorApiKeyResolver;
import info.jab.churrera.util.PropertyResolver;
import info.jab.churrera.workflow.WorkflowValidator;
import info.jab.churrera.workflow.PmlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import info.jab.churrera.cli.util.GitInfo;

import java.io.IOException;
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
@ApplicationScoped
public class ChurreraCLI implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ChurreraCLI.class);

    // Dependencies injected via CDI
    @Inject
    PropertyResolver propertyResolver;
    
    @Inject
    JobRepository jobRepository;
    
    @Inject
    CLIAgent cliAgent;
    
    @Inject
    WorkflowParser workflowParser;
    
    @Inject
    JobProcessor jobProcessor;
    
    @Inject
    WorkflowValidator workflowValidator;
    
    @Inject
    PmlValidator pmlValidator;

    /**
     * Default constructor for CDI.
     */
    public ChurreraCLI() {
    }

    /**
     * Constructor for testing that accepts all dependencies.
     */
    ChurreraCLI(
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
    }


    @Inject
    RunCommand runCommand;
    
    /**
     * Creates and initializes the Run command with all required dependencies.
     */
    RunCommand createRunCmd() {
        logger.debug("JobRepository initialized");
        return runCommand;
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
            // Initialize Quarkus Arc container
            io.quarkus.arc.Arc.initialize();
            
            try {
                // Get ChurreraCLI instance from CDI container
                final ChurreraCLI cli = io.quarkus.arc.Arc.container().instance(ChurreraCLI.class).get();

                // Create CommandLine with root command
                CommandLine commandLine = new CommandLine(cli);

                // Create and register subcommands manually
                final RunCommand runCommand = cli.createRunCmd();

                commandLine.addSubcommand("run", runCommand);

                // Add shutdown hook to ensure proper cleanup
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.trace("Shutdown hook triggered");
                    // Cleanup for RunCommand
                    if (runCommand != null && runCommand.getJobRepository() != null) {
                        runCommand.getJobRepository().close();
                    }
                    // Shutdown Arc container
                    io.quarkus.arc.Arc.shutdown();
                }));

                int exitCode = commandLine.execute(args);

                System.exit(exitCode);
            } finally {
                // Shutdown Arc container
                io.quarkus.arc.Arc.shutdown();
            }

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
