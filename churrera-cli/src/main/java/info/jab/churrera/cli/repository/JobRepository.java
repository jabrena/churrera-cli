package info.jab.churrera.cli.repository;

import java.util.List;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.model.JobWithDetails;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.util.JobXmlMapper;
import info.jab.churrera.cli.util.PromptXmlMapper;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.Get;
import org.basex.core.cmd.Open;
import org.basex.core.cmd.XQuery;
import org.basex.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import info.jab.churrera.util.PropertyResolver;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

/**
 * Repository for managing jobs in BaseX XML database.
 */
public class JobRepository {

    private static final Logger logger = LoggerFactory.getLogger(JobRepository.class);

    private static final String DATABASE_NAME = "churrera-jobs";
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String JOBS_XML = "jobs.xml";
    private static final String PROMPTS_XML = "prompts.xml";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Context context;
    private final String databasePath;

    public JobRepository(PropertyResolver propertyResolver) throws IOException, BaseXException {
        this.databasePath = propertyResolver.getProperty(APPLICATION_PROPERTIES, "basex.database.path")
                .orElse("/tmp/churrera-data");

        // Ensure the database directory exists
        Path dbPath = Paths.get(databasePath);
        if (!Files.exists(dbPath)) {
            Files.createDirectories(dbPath);
            logger.info("Created database directory: {}", dbPath);
        }

        // Configure BaseX to use the specified database path before creating context
        System.setProperty("org.basex.DBPATH", databasePath);
        this.context = new Context();

        // Initialize the database
        initialize();
    }

    /**
     * Initialize the repository by creating the database directory and BaseX
     * database.
     */
    private void initialize() throws IOException, BaseXException {
        // Database directory is already created in constructor

        // Create or open the database
        try {
            // First try to open existing database
            new Open(DATABASE_NAME).execute(context);
            logger.info("Opened existing database: {} at {}", DATABASE_NAME, databasePath);
        } catch (BaseXException e) {
            // Database doesn't exist, create it
            logger.info("Creating new database: {} at {}", DATABASE_NAME, databasePath);
            // Create database with explicit path
            new CreateDB(DATABASE_NAME, databasePath).execute(context);
            // Create initial XML structures
            String initialJobsXml = "<jobs></jobs>";
            String initialPromptsXml = "<prompts></prompts>";
            new Add(JOBS_XML, initialJobsXml).execute(context);
            new Add(PROMPTS_XML, initialPromptsXml).execute(context);
            logger.info("Created initial XML structures");
        }

        // Ensure the database is properly opened
        try {
            new Open(DATABASE_NAME).execute(context);
        } catch (BaseXException e) {
            logger.error("Failed to open database: {}", DATABASE_NAME, e);
            throw e;
        }

        // Verify the database is working
        try {
            String testQuery = "db:list('churrera-jobs')";
            String result = new XQuery(testQuery).execute(context);

            // If no documents exist, create the initial ones
            if (result.trim().isEmpty()) {
                String initialJobsXml = "<jobs></jobs>";
                String initialPromptsXml = "<prompts></prompts>";
                new Add(JOBS_XML, initialJobsXml).execute(context);
                new Add(PROMPTS_XML, initialPromptsXml).execute(context);
            }
        } catch (BaseXException e) {
            logger.warn("Database test failed", e);
        }
    }

    /**
     * Find all jobs in the database.
     *
     * @return list of all jobs
     */
    public List<Job> findAll() throws BaseXException, QueryException {
        try {
            // Use BaseX Get command to retrieve the document content
            String xmlContent = new Get(JOBS_XML).execute(context);
            // Parse the XML content
            return JobXmlMapper.fromDocument(xmlContent, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            logger.error("Error using Get command in findAll", e);
            return new ArrayList<>();
        }
    }

    /**
     * Find a job by its ID.
     *
     * @param jobId the job ID to search for
     * @return Optional containing the job if found
     */
    public Optional<Job> findById(String jobId) throws BaseXException, QueryException {
        try {
            // Use BaseX Get command to retrieve the document content
            String xmlContent = new Get(JOBS_XML).execute(context);

            // Parse the XML content and find the specific job
            return JobXmlMapper.fromDocument(xmlContent, DATE_TIME_FORMATTER).stream()
                    .filter(job -> job.jobId().equals(jobId))
                    .findFirst();

        } catch (Exception e) {
            logger.error("Error using Get command in findById for jobId: {}", jobId, e);
            return Optional.empty();
        }
    }

    /**
     * Save a job to the database.
     *
     * @param job the job to save
     */
    public void save(Job job) throws BaseXException, IOException, QueryException {
        // Check if job already exists
        Optional<Job> existingJob = findById(job.jobId());

        if (existingJob.isPresent()) {
            // Update existing job
            logger.debug("Updating existing job: {}", job.jobId());
            String updateQuery = "replace node doc('" + DATABASE_NAME + "/" + JOBS_XML + "')/jobs/job[jobId='" + job.jobId()
                    + "'] " +
                    "with " + JobXmlMapper.toXml(job, DATE_TIME_FORMATTER);
            new XQuery(updateQuery).execute(context);
            logger.info("Updated job: {}", job.jobId());
        } else {
            // Add new job
            logger.debug("Adding new job: {}", job.jobId());
            String insertQuery = "insert node " + JobXmlMapper.toXml(job, DATE_TIME_FORMATTER) + " into doc('" + DATABASE_NAME + "/" + JOBS_XML + "')/jobs";
            new XQuery(insertQuery).execute(context);
            logger.info("Saved new job: {}", job.jobId());
        }
    }

    /**
     * Delete a job by its ID.
     *
     * @param jobId the job ID to delete
     */
    public void deleteById(String jobId) throws BaseXException, QueryException {
        logger.debug("Deleting job: {}", jobId);
        String deleteQuery = "delete node doc('" + DATABASE_NAME + "/" + JOBS_XML + "')/jobs/job[jobId='" + jobId + "']";
        new XQuery(deleteQuery).execute(context);
        logger.info("Deleted job: {}", jobId);
    }

    /**
     * Save a prompt to the database.
     *
     * @param prompt the prompt to save
     */
    public void savePrompt(Prompt prompt) throws BaseXException, IOException, QueryException {
        // Check if prompt already exists
        Optional<Prompt> existingPrompt = findPromptById(prompt.promptId());

        if (existingPrompt.isPresent()) {
            // Update existing prompt
            logger.debug("Updating existing prompt: {}", prompt.promptId());
            String updateQuery = "replace node doc('" + DATABASE_NAME + "/" + PROMPTS_XML + "')/prompts/prompt[promptId='"
                    + prompt.promptId() + "'] " +
                    "with " + PromptXmlMapper.toXml(prompt, DATE_TIME_FORMATTER);
            new XQuery(updateQuery).execute(context);
            logger.info("Updated prompt: {}", prompt.promptId());
        } else {
            // Add new prompt
            logger.debug("Adding new prompt: {}", prompt.promptId());
            String insertQuery = "insert node " + PromptXmlMapper.toXml(prompt, DATE_TIME_FORMATTER) + " into doc('" + DATABASE_NAME
                    + "/" + PROMPTS_XML + "')/prompts";
            new XQuery(insertQuery).execute(context);
            logger.info("Saved new prompt: {}", prompt.promptId());
        }
    }

    /**
     * Find a prompt by its ID.
     *
     * @param promptId the prompt ID to search for
     * @return Optional containing the prompt if found
     */
    public Optional<Prompt> findPromptById(String promptId) throws BaseXException, QueryException {
        try {
            String xmlContent = new Get(PROMPTS_XML).execute(context);

            // Parse the XML content and find the specific prompt
            return PromptXmlMapper.fromDocument(xmlContent, DATE_TIME_FORMATTER).stream()
                    .filter(prompt -> prompt.promptId().equals(promptId))
                    .findFirst();

        } catch (Exception e) {
            logger.error("Error finding prompt by ID: {}", promptId, e);
            return Optional.empty();
        }
    }

    /**
     * Find all prompts for a specific job.
     *
     * @param jobId the job ID to search for
     * @return list of prompts for the job
     */
    public List<Prompt> findPromptsByJobId(String jobId) throws BaseXException, QueryException {
        try {
            String xmlContent = new Get(PROMPTS_XML).execute(context);
            List<Prompt> allPrompts = PromptXmlMapper.fromDocument(xmlContent, DATE_TIME_FORMATTER);
            List<Prompt> jobPrompts = new ArrayList<>();

            for (Prompt prompt : allPrompts) {
                if (prompt.jobId().equals(jobId)) {
                    jobPrompts.add(prompt);
                }
            }

            return jobPrompts;

        } catch (Exception e) {
            logger.error("Error finding prompts by job ID: {}", jobId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Find job with all related prompts.
     *
     * @param jobId the job ID to search for
     * @return JobWithDetails containing job and prompts
     */
    public Optional<JobWithDetails> findJobWithDetails(String jobId) throws BaseXException, QueryException {
        Optional<Job> job = findById(jobId);
        if (job.isEmpty()) {
            return Optional.empty();
        }

        List<Prompt> prompts = findPromptsByJobId(jobId);

        return Optional.of(new JobWithDetails(job.get(), prompts));
    }

    /**
     * Find all jobs that are not in terminal state.
     *
     * @return list of unfinished jobs
     */
    public List<Job> findUnfinishedJobs() throws BaseXException, QueryException {
        try {
            String xmlContent = new Get(JOBS_XML).execute(context);
            List<Job> allJobs = JobXmlMapper.fromDocument(xmlContent, DATE_TIME_FORMATTER);
            List<Job> unfinishedJobs = new ArrayList<>();

            for (Job job : allJobs) {
                if (job.cursorAgentId() == null || !job.status().isTerminal()) {
                    unfinishedJobs.add(job);
                }
            }

            return unfinishedJobs;

        } catch (Exception e) {
            logger.error("Error finding unfinished jobs", e);
            return new ArrayList<>();
        }
    }

    /**
     * Delete all prompts for a specific job.
     *
     * @param jobId the job ID to delete prompts for
     */
    public void deletePromptsByJobId(String jobId) throws BaseXException, QueryException {
        try {
            String xmlContent = new Get(PROMPTS_XML).execute(context);
            List<Prompt> allPrompts = PromptXmlMapper.fromDocument(xmlContent, DATE_TIME_FORMATTER);

            for (Prompt prompt : allPrompts) {
                if (prompt.jobId().equals(jobId)) {
                    String deleteQuery = "delete node doc('" + DATABASE_NAME
                            + "/" + PROMPTS_XML + "')/prompts/prompt[promptId='" + prompt.promptId() + "']";
                    new XQuery(deleteQuery).execute(context);
                }
            }

        } catch (Exception e) {
            logger.error("Error deleting prompts by job ID: {}", jobId, e);
        }
    }

    /**
     * Find all jobs by parent job ID.
     *
     * @param parentJobId the parent job ID to search for
     * @return list of child jobs
     */
    public List<Job> findJobsByParentId(String parentJobId) throws BaseXException, QueryException {
        try {
            String xmlContent = new Get(JOBS_XML).execute(context);
            List<Job> allJobs = JobXmlMapper.fromDocument(xmlContent, DATE_TIME_FORMATTER);
            List<Job> childJobs = new ArrayList<>();

            for (Job job : allJobs) {
                if (job.parentJobId() != null && job.parentJobId().equals(parentJobId)) {
                    childJobs.add(job);
                }
            }

            return childJobs;

        } catch (Exception e) {
            logger.error("Error finding jobs by parent ID: {}", parentJobId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Close the repository and clean up resources.
     */
    public void close() {
        if (context != null) {
            logger.debug("Closing repository");
            context.close();
            logger.info("Repository closed");
        }
    }

}
