package info.jab.churrera.cli.command;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.agent.AgentState;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowParser;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.UUID;

/**
 * Command to create a new job.
 */
@CommandLine.Command(
    name = "jobs/new",
    description = "Create a new job"
)
public class NewJobCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NewJobCommand.class);

    private final JobRepository jobRepository;
    private final Scanner scanner;

    public NewJobCommand(JobRepository jobRepository, Scanner scanner) {
        this.jobRepository = jobRepository;
        this.scanner = scanner;
    }

    @Override
    public void run() {
        try {
            logger.info("Enter job path: ");
            String path = scanner.nextLine().trim();

            if (path.isEmpty()) {
                logger.info("Job path cannot be empty.");
                return;
            }

            logger.info("Enter model: ");
            String model = scanner.nextLine().trim();

            if (model.isEmpty()) {
                model = "default-model";
            }

            logger.info("Enter repository: ");
            String repository = scanner.nextLine().trim();

            if (repository.isEmpty()) {
                repository = "default-repository";
            }

            String jobId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            // Determine workflow type
            WorkflowType workflowType = WorkflowParser.determineWorkflowType(new File(path));

            Job job = new Job(jobId, path, null, model, repository, AgentState.UNKNOWN, now, now, null, null, workflowType);
            jobRepository.save(job);

            logger.info("Job created with ID: {}", jobId);
            logger.info("  Path: {}", path);
            logger.info("  Created: {}", now);

        } catch (IOException | QueryException e) {
            logger.error("Error creating job: {}", e.getMessage());
        }
    }
}
