package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.command.cli.TableFormatter;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowType;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service for displaying job information in tables.
 */
public class JobDisplayService {
    private static final Logger logger = LoggerFactory.getLogger(JobDisplayService.class);
    private static final int JOB_ID_PREFIX_LENGTH = 8;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final String UNKNOWN_TYPE = "Unknown";
    private static final String ERROR_STATUS = "ERROR";
    private static final String STARTED_PREFIX = "Started ";

    private final JobRepository jobRepository;
    private final Clock clock;

    public JobDisplayService(JobRepository jobRepository) {
        this(jobRepository, Clock.systemDefaultZone());
    }

    JobDisplayService(JobRepository jobRepository, Clock clock) {
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * Displays a filtered jobs table showing only the specified job and its children (if parallel).
     *
     * @param jobId the job ID to display
     */
    public void displayFilteredJobsTable(String jobId) {
        try {
            // Get the job
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            // Collect jobs to display
            List<Job> jobsToDisplay = collectJobsToDisplay(jobId, job);

            // Display table
            if (jobsToDisplay.isEmpty()) {
                System.out.println("No jobs found.");
                return;
            }

            // Prepare table data
            String[] headers = {"Job ID", "Parent Job", "Type", "Prompts", "Status", "Last update", "Completed"};
            List<String[]> rows = new ArrayList<>();

            for (Job j : jobsToDisplay) {
                try {
                    String[] row = formatJobRow(j);
                    rows.add(row);
                } catch (Exception e) {
                    logger.error("Error retrieving details for job {}: {}", j.jobId(), e.getMessage());
                    // Add row with error indication
                    String parentJobDisplay = j.parentJobId() != null ? shortenId(j.parentJobId()) : "NA";
                    String typeDisplay = j.type() != null ? j.type().toString() : UNKNOWN_TYPE;
                    String[] row = {
                        shortenId(j.jobId()),
                        parentJobDisplay,
                        typeDisplay,
                        ERROR_STATUS,
                        ERROR_STATUS,
                        ERROR_STATUS,
                        ERROR_STATUS
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
     * Collects jobs to display (parent and children if parallel).
     */
    private List<Job> collectJobsToDisplay(String jobId, Job job) throws BaseXException, QueryException {
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

        return jobsToDisplay;
    }

    /**
     * Formats a job row for table display.
     */
    private String[] formatJobRow(Job job) throws BaseXException, QueryException {
        List<Prompt> prompts = jobRepository.findPromptsByJobId(job.jobId());
        int totalPrompts = prompts.size();
        int completedPrompts = countCompletedPrompts(prompts);

        // Get job status
        String status = job.status().toString();

        // Format prompt completion: "completed/total"
        String promptStatus = formatPromptStatus(job, totalPrompts, completedPrompts);

        // Calculate time display
        String timeAgo = formatTimeAgo(job);

        // Format IDs for display (truncate UUIDs to 8 chars)
        String jobIdDisplay = shortenId(job.jobId());
        String parentJobDisplay = job.parentJobId() != null ? shortenId(job.parentJobId()) : "NA";

        // Determine type display
        String typeDisplay = determineTypeDisplay(job);

        // Format last update timestamp as MMddyy HH:mm
        DateTimeFormatter lastUpdateFormatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
        String lastUpdateDisplay = job.lastUpdate() != null ? job.lastUpdate().format(lastUpdateFormatter) : "NA";

        return new String[]{
            jobIdDisplay,
            parentJobDisplay,
            typeDisplay,
            promptStatus,
            status,
            lastUpdateDisplay,
            timeAgo
        };
    }

    /**
     * Counts completed prompts.
     */
    private int countCompletedPrompts(List<Prompt> prompts) {
        int completedPrompts = 0;
        for (Prompt prompt : prompts) {
            if ("COMPLETED".equals(prompt.status()) || "SENT".equals(prompt.status())) {
                completedPrompts++;
            }
        }
        return completedPrompts;
    }

    /**
     * Formats prompt status.
     */
    private String formatPromptStatus(Job job, int totalPrompts, int completedPrompts) {
        if (job.status().isTerminal()) {
            return totalPrompts + "/" + totalPrompts;
        } else {
            return completedPrompts + "/" + totalPrompts;
        }
    }

    /**
     * Formats time ago display.
     */
    private String formatTimeAgo(Job job) {
        if (job.status().isTerminal()) {
            // For terminal jobs, calculate duration from job creation to last update in mm:ss
            Duration duration = Duration.between(job.createdAt(), job.lastUpdate());
            long totalSeconds = duration.getSeconds();
            long minutes = totalSeconds / SECONDS_PER_MINUTE;
            long seconds = totalSeconds % SECONDS_PER_MINUTE;

            if (minutes == 0) {
                return String.format("%02d secs", seconds);
            } else {
                return String.format("%02d:%02d min", minutes, seconds);
            }
        } else {
            // For active jobs, calculate time elapsed since job creation
            Duration duration = Duration.between(job.createdAt(), LocalDateTime.now(clock));
            long secondsElapsed = duration.getSeconds();

            if (secondsElapsed < SECONDS_PER_MINUTE) {
                return STARTED_PREFIX + secondsElapsed + "s ago";
            } else if (secondsElapsed < SECONDS_PER_HOUR) {
                long minutes = secondsElapsed / SECONDS_PER_MINUTE;
                return STARTED_PREFIX + minutes + (minutes == 1 ? " min ago" : " mins ago");
            } else {
                long hours = secondsElapsed / SECONDS_PER_HOUR;
                return STARTED_PREFIX + hours + (hours == 1 ? " hour ago" : " hours ago");
            }
        }
    }

    /**
     * Determines type display string.
     */
    private String determineTypeDisplay(Job job) {
        if (job.type() != null) {
            return job.type().toString();
        } else {
            // Parse workflow file to determine type for legacy jobs
            try {
                WorkflowType parsedType = WorkflowParser.determineWorkflowType(new File(job.path()));
                return parsedType != null ? parsedType.toString() : UNKNOWN_TYPE;
            } catch (Exception e) {
                return UNKNOWN_TYPE;
            }
        }
    }

    /**
     * Shortens a job ID to 8 characters for display.
     * Package-private for testing.
     */
    String shortenId(String id) {
        if (id == null || id.isEmpty()) {
            return "NA";
        }
        return id.length() > JOB_ID_PREFIX_LENGTH ? id.substring(0, JOB_ID_PREFIX_LENGTH) : id;
    }
}

