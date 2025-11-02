package info.jab.churrera.cli.commands;

import picocli.CommandLine;

/**
 * Command to display help information.
 */
@CommandLine.Command(name = "help", description = "Display available commands")
public class HelpCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Available commands:");
        System.out.println("  jobs                    - List all jobs");
        System.out.println("  jobs new {path}         - Create a new job with specified path");
        System.out.println("  jobs status {uuid}      - Show details for specific job ID");
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
}
