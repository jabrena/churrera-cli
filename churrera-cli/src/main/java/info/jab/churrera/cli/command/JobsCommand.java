package info.jab.churrera.cli.command;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.cli.command.TableFormatter;
import info.jab.churrera.cli.model.AgentState;
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
                        List<Prompt> prompts = jobRepository.findPromptsByJobId(job.jobId());
                        int totalPrompts = prompts.size();
                        int completedPrompts = 0;

                        // Count completed prompts
                        for (Prompt prompt : prompts) {
                            if ("COMPLETED".equals(prompt.status()) || "SENT".equals(prompt.status())) {
                                completedPrompts++;
                            }
                        }

                        // Get job status (simpler: just show the status from the database)
                        String status = job.status().toString();

                        // Format prompt completion: "completed/total"
                        // If job is in a terminal state, all prompts should be considered completed
                        String promptStatus;
                        if (job.status().isTerminal()) {
                            promptStatus = totalPrompts + "/" + totalPrompts;
                        } else {
                            promptStatus = completedPrompts + "/" + totalPrompts;
                        }

                        // Calculate time display
                        String timeAgo;
                        if (job.status().isTerminal()) {
                            // For terminal jobs, calculate duration from job creation to last update in mm:ss
                            Duration duration = Duration.between(job.createdAt(), job.lastUpdate());
                            long totalSeconds = duration.getSeconds();
                            long minutes = totalSeconds / 60;
                            long seconds = totalSeconds % 60;

                            if (minutes == 0) {
                                timeAgo = String.format("%02d secs", seconds);
                            } else {
                                timeAgo = String.format("%02d:%02d min", minutes, seconds);
                            }
                        } else {
                            // For active jobs, calculate time elapsed since job creation
                            Duration duration = Duration.between(job.createdAt(), LocalDateTime.now());
                            long secondsElapsed = duration.getSeconds();

                            if (secondsElapsed < 60) {
                                timeAgo = "Started " + secondsElapsed + "s ago";
                            } else if (secondsElapsed < 3600) {
                                long minutes = secondsElapsed / 60;
                                timeAgo = "Started " + minutes + (minutes == 1 ? " min ago" : " mins ago");
                            } else {
                                long hours = secondsElapsed / 3600;
                                timeAgo = "Started " + hours + (hours == 1 ? " hour ago" : " hours ago");
                            }
                        }

                        // Format IDs for display (truncate UUIDs to 8 chars)
                        String jobIdDisplay = shortenId(job.jobId());
                        String parentJobDisplay = job.parentJobId() != null ? shortenId(job.parentJobId()) : "NA";

                        // Determine type display
                        String typeDisplay;
                        if (job.type() != null) {
                            typeDisplay = job.type().toString();
                        } else {
                            // Parse workflow file to determine type for legacy jobs
                            try {
                                WorkflowType parsedType = WorkflowParser.determineWorkflowType(new File(job.path()));
                                typeDisplay = parsedType != null ? parsedType.toString() : "Unknown";
                            } catch (Exception e) {
                                typeDisplay = "Unknown";
                            }
                        }

                        logger.debug("Job {}: type={}, status={}, prompts={}, timeAgo={}, parent={}",
                            job.jobId(), typeDisplay, status, promptStatus, timeAgo, parentJobDisplay);

                        // Format last update timestamp as MMddyy HH:mm
                        DateTimeFormatter lastUpdateFormatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
                        String lastUpdateDisplay = job.lastUpdate() != null ? job.lastUpdate().format(lastUpdateFormatter) : "NA";

                        String[] row = {
                            jobIdDisplay,
                            parentJobDisplay,
                            typeDisplay,
                            promptStatus,
                            status,
                            lastUpdateDisplay,
                            timeAgo
                        };
                        rows.add(row);

                    } catch (Exception e) {
                        logger.error("Error retrieving details for job {}: {}", job.jobId(), e.getMessage());
                        // Add row with error indication
                        String parentJobDisplay = job.parentJobId() != null ? shortenId(job.parentJobId()) : "NA";
                        String typeDisplay = job.type() != null ? job.type().toString() : "Unknown";
                        String[] row = {
                            shortenId(job.jobId()),
                            parentJobDisplay,
                            typeDisplay,
                            "ERROR",
                            "ERROR",
                            "ERROR",
                            "ERROR"
                        };
                        rows.add(row);
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
}
