package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.workflow.WorkflowType;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Service for polling job execution status.
 */
public class JobPollingService {
    private static final Logger logger = LoggerFactory.getLogger(JobPollingService.class);

    private final JobProcessor jobProcessor;
    private final JobRepository jobRepository;
    private final JobDisplayService displayService;
    private final CompletionCheckerFactory completionCheckerFactory;
    private final int pollingIntervalSeconds;
    private final Sleeper sleeper;

    public JobPollingService(JobProcessor jobProcessor, JobRepository jobRepository,
                            JobDisplayService displayService,
                            CompletionCheckerFactory completionCheckerFactory,
                            int pollingIntervalSeconds) {
        this(jobProcessor, jobRepository, displayService, completionCheckerFactory,
            pollingIntervalSeconds, Thread::sleep);
    }

    JobPollingService(JobProcessor jobProcessor, JobRepository jobRepository,
                      JobDisplayService displayService,
                      CompletionCheckerFactory completionCheckerFactory,
                      int pollingIntervalSeconds,
                      Sleeper sleeper) {
        this.jobProcessor = Objects.requireNonNull(jobProcessor, "jobProcessor cannot be null");
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository cannot be null");
        this.displayService = Objects.requireNonNull(displayService, "displayService cannot be null");
        this.completionCheckerFactory = Objects.requireNonNull(completionCheckerFactory, "completionCheckerFactory cannot be null");
        this.pollingIntervalSeconds = pollingIntervalSeconds;
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper cannot be null");
    }

    /**
     * Executes the blocking polling loop until job completion.
     *
     * @param jobId the job ID to monitor
     * @return execution result with final status and interruption flag
     */
    public ExecutionResult executePollingLoop(String jobId) {
        AgentState finalStatus = null;
        boolean interrupted = false;

        while (true) {
            // Process the job
            jobProcessor.processJobs();

            // Display filtered jobs table
            displayService.displayFilteredJobsTable(jobId);

            // Check if job is terminal
            Job job;
            try {
                job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            } catch (BaseXException | QueryException e) {
                throw new RuntimeException("Error retrieving job " + jobId + ": " + e.getMessage(), e);
            }

            // Check completion based on workflow type
            CompletionChecker checker = completionCheckerFactory.create(job.type() != null ? job.type() : WorkflowType.SEQUENCE);
            CompletionCheckResult completionResult = checker.checkCompletion(job, jobId);
            if (completionResult.isCompleted()) {
                finalStatus = completionResult.getFinalStatus();
                // Store child jobs for deletion handling
                return new ExecutionResult(finalStatus, false, completionResult.getChildJobs());
            }

            // Sleep for polling interval
            if (sleepWithInterruptCheck()) {
                interrupted = true;
                break;
            }
        }

        return new ExecutionResult(finalStatus, interrupted, List.of());
    }

    /**
     * Sleeps for the polling interval and checks for interruption.
     *
     * @return true if interrupted, false otherwise
     */
    private boolean sleepWithInterruptCheck() {
        try {
            sleeper.sleep(pollingIntervalSeconds * 1000L);
            return false;
        } catch (InterruptedException e) {
            logger.warn("Polling interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return true;
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}

