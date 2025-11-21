package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.command.cli.TableFormatter;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Command to list all jobs.
 */
@CommandLine.Command(
    name = "jobs",
    description = "List all jobs"
)
public class JobsCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(JobsCommand.class);

    private static final int JOB_ID_PREFIX_LENGTH = 8;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final String UNKNOWN_TYPE = "Unknown";
    private static final String ERROR_STATUS = "ERROR";
    private static final String STARTED_PREFIX = "Started ";

    private final JobRepository jobRepository;

    public JobsCommand(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    private String shortenId(String id) {
        if (id == null || id.isEmpty()) {
            return "NA";
        }
        return id.length() > JOB_ID_PREFIX_LENGTH ? id.substring(0, JOB_ID_PREFIX_LENGTH) : id;
    }

    @Override
    public void run() {
        try {
            List<Job> jobs = jobRepository.findAll();

            if (jobs.isEmpty()) {
                System.out.println("No jobs found.");
                logger.info("No jobs found.");
            } else {
                // Prepare table data
                String[] headers = {"Job ID", "Parent Job", "Type", "Prompts", "Status", "Last update", "Completed"};
                List<String[]> rows = new ArrayList<>();

                for (Job job : jobs) {
                    try {
                        String[] row = formatJobRow(job);
                        rows.add(row);
                    } catch (Exception e) {
                        logger.error("Error retrieving details for job {}: {}", job.jobId(), e.getMessage());
                        String[] errorRow = createErrorRow(job);
                        rows.add(errorRow);
                    }
                }

                // Display table to console and log to file
                String tableOutput = TableFormatter.formatTable(headers, rows);
                System.out.println(tableOutput);
                logger.info("Jobs table displayed:\n{}", tableOutput);
            }
        } catch (BaseXException | QueryException e) {
            String errorMessage = "Error listing jobs: " + e.getMessage();
            System.out.println(errorMessage);
            logger.error("Error listing jobs: {}", e.getMessage());
        }
    }

    private String[] formatJobRow(Job job) throws BaseXException, QueryException {
        List<Prompt> prompts = jobRepository.findPromptsByJobId(job.jobId());
        int totalPrompts = prompts.size();
        int completedPrompts = countCompletedPrompts(prompts);
        String status = job.status().toString();
        String promptStatus = formatPromptStatus(job, totalPrompts, completedPrompts);
        String timeAgo = formatTimeAgo(job);
        String jobIdDisplay = shortenId(job.jobId());
        String parentJobDisplay = job.parentJobId() != null ? shortenId(job.parentJobId()) : "NA";
        String typeDisplay = determineTypeDisplay(job);
        String lastUpdateDisplay = formatLastUpdate(job);

        logger.debug("Job {}: type={}, status={}, prompts={}, timeAgo={}, parent={}",
            job.jobId(), typeDisplay, status, promptStatus, timeAgo, parentJobDisplay);

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

    private int countCompletedPrompts(List<Prompt> prompts) {
        int completedPrompts = 0;
        for (Prompt prompt : prompts) {
            if ("COMPLETED".equals(prompt.status()) || "SENT".equals(prompt.status())) {
                completedPrompts++;
            }
        }
        return completedPrompts;
    }

    private String formatPromptStatus(Job job, int totalPrompts, int completedPrompts) {
        if (job.status().isTerminal()) {
            return totalPrompts + "/" + totalPrompts;
        } else {
            return completedPrompts + "/" + totalPrompts;
        }
    }

    private String formatTimeAgo(Job job) {
        if (job.status().isTerminal()) {
            return formatTerminalTimeAgo(job);
        } else {
            return formatActiveTimeAgo(job);
        }
    }

    private String formatTerminalTimeAgo(Job job) {
        Duration duration = Duration.between(job.createdAt(), job.lastUpdate());
        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / SECONDS_PER_MINUTE;
        long seconds = totalSeconds % SECONDS_PER_MINUTE;

        if (minutes == 0) {
            return String.format("%02d secs", seconds);
        } else {
            return String.format("%02d:%02d min", minutes, seconds);
        }
    }

    private String formatActiveTimeAgo(Job job) {
        Duration duration = Duration.between(job.createdAt(), LocalDateTime.now());
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

    private String determineTypeDisplay(Job job) {
        if (job.type() != null) {
            return job.type().toString();
        }
        try {
            WorkflowType parsedType = WorkflowParser.determineWorkflowType(new File(job.path()));
            return parsedType != null ? parsedType.toString() : UNKNOWN_TYPE;
        } catch (Exception e) {
            return UNKNOWN_TYPE;
        }
    }

    private String formatLastUpdate(Job job) {
        DateTimeFormatter lastUpdateFormatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
        return job.lastUpdate() != null ? job.lastUpdate().format(lastUpdateFormatter) : "NA";
    }

    private String[] createErrorRow(Job job) {
        String parentJobDisplay = job.parentJobId() != null ? shortenId(job.parentJobId()) : "NA";
        String typeDisplay = job.type() != null ? job.type().toString() : UNKNOWN_TYPE;
        return new String[]{
            shortenId(job.jobId()),
            parentJobDisplay,
            typeDisplay,
            ERROR_STATUS,
            ERROR_STATUS,
            ERROR_STATUS,
            ERROR_STATUS
        };
    }
}
