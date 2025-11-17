package info.jab.churrera.cli.repository;

import java.util.List;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.model.JobWithDetails;
import info.jab.churrera.workflow.WorkflowType;
import info.jab.churrera.cli.model.AgentState;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.StaticOptions;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.Get;
import org.basex.core.cmd.Open;
import org.basex.core.cmd.XQuery;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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
    private static final String COLLECTION_NAME = "jobs";
    private static final String PROMPTS_COLLECTION = "prompts";
    private static final String APPLICATION_PROPERTIES = "application.properties";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Context context;
    private final String databasePath;
    private boolean initialized = false;

    public JobRepository(PropertyResolver propertyResolver) {
        this.databasePath = propertyResolver.getProperty(APPLICATION_PROPERTIES, "basex.database.path")
                .orElse("/tmp/churrera-data");

        // Ensure the database directory exists
        try {
            Path dbPath = Paths.get(databasePath);
            if (!Files.exists(dbPath)) {
                Files.createDirectories(dbPath);
                logger.info("Created database directory: {}", dbPath);
            }
        } catch (IOException e) {
            logger.error("Failed to create database directory: {}", databasePath, e);
        }

        // Configure BaseX to use the specified database path before creating context
        System.setProperty("org.basex.DBPATH", databasePath);
        this.context = new Context();
    }

    /**
     * Initialize the repository by creating the database directory and BaseX
     * database.
     */
    public void initialize() throws IOException, BaseXException {
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
            new Add("jobs.xml", initialJobsXml).execute(context);
            new Add("prompts.xml", initialPromptsXml).execute(context);
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
                new Add("jobs.xml", initialJobsXml).execute(context);
                new Add("prompts.xml", initialPromptsXml).execute(context);
            }
        } catch (BaseXException e) {
            logger.warn("Database test failed", e);
        }

        initialized = true;
    }

    /**
     * Find all jobs in the database.
     *
     * @return list of all jobs
     */
    public List<Job> findAll() throws BaseXException, QueryException {
        ensureInitialized();

        // Get the entire document and parse it
        String query = "doc('" + DATABASE_NAME + "/jobs.xml')";

        try (QueryProcessor qp = new QueryProcessor(query, context)) {
            Iter iter = qp.iter();
            List<Job> jobs = new ArrayList<>();

            for (Item item; (item = iter.next()) != null;) {
                try {
                    String xmlContent = item.toString();

                    // Check if we got a BaseX internal reference
                    if (xmlContent.contains("db:get-pre")) {
                        // Use the alternative method with BaseX Get command
                        return findAllAlternative();
                    }

                    // Parse the entire document and extract jobs
                    jobs.addAll(parseJobsFromDocument(xmlContent));
                } catch (Exception e) {
                    logger.error("Error parsing document", e);
                    logger.debug("XML content: {}", item.toString());
                }
            }
            return jobs;
        }
    }

    /**
     * Alternative method to find all jobs using BaseX Get command.
     */
    private List<Job> findAllAlternative() throws BaseXException, QueryException {
        List<Job> jobs = new ArrayList<>();

        try {
            // Use BaseX Get command to retrieve the document content
            String xmlContent = new Get("jobs.xml").execute(context);

            // Parse the XML content
            jobs.addAll(parseJobsFromDocument(xmlContent));

        } catch (Exception e) {
            logger.error("Error using Get command in findAll", e);
        }

        return jobs;
    }

    /**
     * Find a job by its ID.
     *
     * @param jobId the job ID to search for
     * @return Optional containing the job if found
     */
    public Optional<Job> findById(String jobId) throws BaseXException, QueryException {
        ensureInitialized();

        // Use the same approach as findAll - get the complete document and find the job
        return findByIdAlternative(jobId);
    }

    /**
     * Alternative method to find a job by ID using BaseX Get command.
     */
    private Optional<Job> findByIdAlternative(String jobId) throws BaseXException, QueryException {
        try {
            // Use BaseX Get command to retrieve the document content
            String xmlContent = new Get("jobs.xml").execute(context);

            // Parse the XML content and find the specific job
            List<Job> jobs = parseJobsFromDocument(xmlContent);

            // Find the job with the matching ID
            for (Job job : jobs) {
                if (job.jobId().equals(jobId)) {
                    return Optional.of(job);
                }
            }

            return Optional.empty();

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
        ensureInitialized();

        // Check if job already exists
        Optional<Job> existingJob = findById(job.jobId());

        if (existingJob.isPresent()) {
            // Update existing job
            logger.debug("Updating existing job: {}", job.jobId());
            String updateQuery = "replace node doc('" + DATABASE_NAME + "/jobs.xml')/jobs/job[jobId='" + job.jobId()
                    + "'] " +
                    "with " + jobToXml(job);
            new XQuery(updateQuery).execute(context);
            logger.info("Updated job: {}", job.jobId());
        } else {
            // Add new job
            logger.debug("Adding new job: {}", job.jobId());
            String insertQuery = "insert node " + jobToXml(job) + " into doc('" + DATABASE_NAME + "/jobs.xml')/jobs";
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
        ensureInitialized();

        logger.debug("Deleting job: {}", jobId);
        String deleteQuery = "delete node doc('" + DATABASE_NAME + "/jobs.xml')/jobs/job[jobId='" + jobId + "']";
        new XQuery(deleteQuery).execute(context);
        logger.info("Deleted job: {}", jobId);
    }

    /**
     * Save a prompt to the database.
     *
     * @param prompt the prompt to save
     */
    public void savePrompt(Prompt prompt) throws BaseXException, IOException, QueryException {
        ensureInitialized();

        // Check if prompt already exists
        Optional<Prompt> existingPrompt = findPromptById(prompt.promptId());

        if (existingPrompt.isPresent()) {
            // Update existing prompt
            logger.debug("Updating existing prompt: {}", prompt.promptId());
            String updateQuery = "replace node doc('" + DATABASE_NAME + "/prompts.xml')/prompts/prompt[promptId='"
                    + prompt.promptId() + "'] " +
                    "with " + promptToXml(prompt);
            new XQuery(updateQuery).execute(context);
            logger.info("Updated prompt: {}", prompt.promptId());
        } else {
            // Add new prompt
            logger.debug("Adding new prompt: {}", prompt.promptId());
            String insertQuery = "insert node " + promptToXml(prompt) + " into doc('" + DATABASE_NAME
                    + "/prompts.xml')/prompts";
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
        ensureInitialized();

        try {
            String xmlContent = new Get("prompts.xml").execute(context);
            List<Prompt> prompts = parsePromptsFromDocument(xmlContent);

            for (Prompt prompt : prompts) {
                if (prompt.promptId().equals(promptId)) {
                    return Optional.of(prompt);
                }
            }

            return Optional.empty();

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
        ensureInitialized();

        try {
            String xmlContent = new Get("prompts.xml").execute(context);
            List<Prompt> allPrompts = parsePromptsFromDocument(xmlContent);
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
        ensureInitialized();

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
        ensureInitialized();

        try {
            String xmlContent = new Get("jobs.xml").execute(context);
            List<Job> allJobs = parseJobsFromDocument(xmlContent);
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
        ensureInitialized();

        try {
            String xmlContent = new Get("prompts.xml").execute(context);
            List<Prompt> allPrompts = parsePromptsFromDocument(xmlContent);

            for (Prompt prompt : allPrompts) {
                if (prompt.jobId().equals(jobId)) {
                    String deleteQuery = "delete node doc('" + DATABASE_NAME
                            + "/prompts.xml')/prompts/prompt[promptId='" + prompt.promptId() + "']";
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
        ensureInitialized();

        try {
            String xmlContent = new Get("jobs.xml").execute(context);
            List<Job> allJobs = parseJobsFromDocument(xmlContent);
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

    private void ensureInitialized() {
        if (!initialized) {
            logger.error("Repository not initialized. Call initialize() first.");
            throw new IllegalStateException("Repository not initialized. Call initialize() first.");
        }
    }

    private String jobToXml(Job job) {
        return String.format(
                "<job>" +
                        "<jobId>%s</jobId>" +
                        "<path>%s</path>" +
                        "<cursorAgentId>%s</cursorAgentId>" +
                        "<model>%s</model>" +
                        "<repository>%s</repository>" +
                        "<status>%s</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "<parentJobId>%s</parentJobId>" +
                        "<result>%s</result>" +
                        "<type>%s</type>" +
                        "<timeoutMillis>%s</timeoutMillis>" +
                        "<workflowStartTime>%s</workflowStartTime>" +
                        "<fallbackSrc>%s</fallbackSrc>" +
                        "<fallbackExecuted>%s</fallbackExecuted>" +
                        "</job>",
                escapeXml(job.jobId()),
                escapeXml(job.path()),
                job.cursorAgentId() != null ? escapeXml(job.cursorAgentId()) : "null",
                escapeXml(job.model()),
                escapeXml(job.repository()),
                escapeXml(job.status().toString()),
                job.createdAt().format(DATE_TIME_FORMATTER),
                job.lastUpdate().format(DATE_TIME_FORMATTER),
                job.parentJobId() != null ? escapeXml(job.parentJobId()) : "null",
                job.result() != null ? escapeXml(job.result()) : "null",
                job.type() != null ? job.type().toString() : "null",
                job.timeoutMillis() != null ? String.valueOf(job.timeoutMillis()) : "null",
                job.workflowStartTime() != null ? job.workflowStartTime().format(DATE_TIME_FORMATTER) : "null",
                job.fallbackSrc() != null ? escapeXml(job.fallbackSrc()) : "null",
                job.fallbackExecuted() != null ? String.valueOf(job.fallbackExecuted()) : "null");
    }

    private Job parseJobFromXml(String xml) {
        // Simple XML parsing - in a real implementation, you might want to use a proper
        // XML parser
        String jobId = extractXmlValue(xml, "jobId");
        String path = extractXmlValue(xml, "path");
        String cursorAgentId = extractXmlValue(xml, "cursorAgentId");
        if ("null".equals(cursorAgentId)) {
            cursorAgentId = null;
        }
        String model = extractXmlValue(xml, "model");
        String repository = extractXmlValue(xml, "repository");
        String statusStr = extractXmlValue(xml, "status");
        LocalDateTime createdAt = LocalDateTime.parse(extractXmlValue(xml, "createdAt"), DATE_TIME_FORMATTER);
        LocalDateTime lastUpdate = LocalDateTime.parse(extractXmlValue(xml, "lastUpdate"), DATE_TIME_FORMATTER);

        // Parse new fields with null handling
        String parentJobId = extractXmlValueOptional(xml, "parentJobId");
        if ("null".equals(parentJobId)) {
            parentJobId = null;
        }
        String result = extractXmlValueOptional(xml, "result");
        if ("null".equals(result)) {
            result = null;
        }
        String typeStr = extractXmlValueOptional(xml, "type");
        WorkflowType type = null;
        if (typeStr != null && !"null".equals(typeStr)) {
            try {
                type = WorkflowType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                // If type cannot be parsed, leave it as null (for legacy jobs)
                logger.warn("Invalid workflow type '{}' for job {}, defaulting to null", typeStr, jobId);
            }
        }

        // Parse AgentState from string
        AgentState status = parseAgentState(statusStr);

        // Parse new timeout and fallback fields (nullable)
        String timeoutMillisStr = extractXmlValueOptional(xml, "timeoutMillis");
        Long timeoutMillis = null;
        if (timeoutMillisStr != null && !"null".equals(timeoutMillisStr)) {
            try {
                timeoutMillis = Long.parseLong(timeoutMillisStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid timeoutMillis '{}' for job {}, defaulting to null", timeoutMillisStr, jobId);
            }
        }

        String workflowStartTimeStr = extractXmlValueOptional(xml, "workflowStartTime");
        LocalDateTime workflowStartTime = null;
        if (workflowStartTimeStr != null && !"null".equals(workflowStartTimeStr)) {
            try {
                workflowStartTime = LocalDateTime.parse(workflowStartTimeStr, DATE_TIME_FORMATTER);
            } catch (Exception e) {
                logger.warn("Invalid workflowStartTime '{}' for job {}, defaulting to null", workflowStartTimeStr, jobId);
            }
        }

        String fallbackSrc = extractXmlValueOptional(xml, "fallbackSrc");
        if (fallbackSrc != null && "null".equals(fallbackSrc)) {
            fallbackSrc = null;
        }

        String fallbackExecutedStr = extractXmlValueOptional(xml, "fallbackExecuted");
        Boolean fallbackExecuted = null;
        if (fallbackExecutedStr != null && !"null".equals(fallbackExecutedStr)) {
            try {
                fallbackExecuted = Boolean.parseBoolean(fallbackExecutedStr);
            } catch (Exception e) {
                logger.warn("Invalid fallbackExecuted '{}' for job {}, defaulting to null", fallbackExecutedStr, jobId);
            }
        }

        return new Job(jobId, path, cursorAgentId, model, repository, status, createdAt, lastUpdate, parentJobId,
                result, type, timeoutMillis, workflowStartTime, fallbackSrc, fallbackExecuted);
    }

    private List<Job> parseJobsFromDocument(String documentXml) {
        List<Job> jobs = new ArrayList<>();

        // Find all job elements in the document
        String jobStartTag = "<job>";
        String jobEndTag = "</job>";

        int startIndex = 0;
        while (true) {
            int jobStart = documentXml.indexOf(jobStartTag, startIndex);
            if (jobStart == -1) {
                break; // No more jobs found
            }

            int jobEnd = documentXml.indexOf(jobEndTag, jobStart);
            if (jobEnd == -1) {
                break; // Malformed XML
            }

            // Extract the job XML
            String jobXml = documentXml.substring(jobStart, jobEnd + jobEndTag.length());

            try {
                jobs.add(parseJobFromXml(jobXml));
            } catch (Exception e) {
                logger.error("Error parsing individual job", e);
                logger.debug("Job XML: {}", jobXml);
            }

            startIndex = jobEnd + jobEndTag.length();
        }

        return jobs;
    }

    private String extractXmlValue(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int startIndex = xml.indexOf(startTag);
        if (startIndex == -1) {
            throw new IllegalArgumentException("Start tag not found: " + startTag);
        }
        int start = startIndex + startTag.length();
        int end = xml.indexOf(endTag, start);
        if (end == -1) {
            throw new IllegalArgumentException("End tag not found: " + endTag);
        }
        return xml.substring(start, end);
    }

    private String extractXmlValueOptional(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int startIndex = xml.indexOf(startTag);
        if (startIndex == -1) {
            return null; // Tag not found, return null
        }
        int start = startIndex + startTag.length();
        int end = xml.indexOf(endTag, start);
        if (end == -1) {
            return null; // End tag not found, return null
        }
        return xml.substring(start, end);
    }

    private String promptToXml(Prompt prompt) {
        return String.format(
                "<prompt>" +
                        "<promptId>%s</promptId>" +
                        "<jobId>%s</jobId>" +
                        "<pmlFile>%s</pmlFile>" +
                        "<status>%s</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "</prompt>",
                escapeXml(prompt.promptId()),
                escapeXml(prompt.jobId()),
                escapeXml(prompt.pmlFile()),
                escapeXml(prompt.status()),
                prompt.createdAt().format(DATE_TIME_FORMATTER),
                prompt.lastUpdate().format(DATE_TIME_FORMATTER));
    }

    private Prompt parsePromptFromXml(String xml) {
        String promptId = extractXmlValue(xml, "promptId");
        String jobId = extractXmlValue(xml, "jobId");
        String pmlFile = extractXmlValue(xml, "pmlFile");
        String status = extractXmlValue(xml, "status");
        LocalDateTime createdAt = LocalDateTime.parse(extractXmlValue(xml, "createdAt"), DATE_TIME_FORMATTER);
        LocalDateTime lastUpdate = LocalDateTime.parse(extractXmlValue(xml, "lastUpdate"), DATE_TIME_FORMATTER);

        return new Prompt(promptId, jobId, pmlFile, status, createdAt, lastUpdate);
    }

    private List<Prompt> parsePromptsFromDocument(String documentXml) {
        List<Prompt> prompts = new ArrayList<>();

        String promptStartTag = "<prompt>";
        String promptEndTag = "</prompt>";

        int startIndex = 0;
        while (true) {
            int promptStart = documentXml.indexOf(promptStartTag, startIndex);
            if (promptStart == -1) {
                break;
            }

            int promptEnd = documentXml.indexOf(promptEndTag, promptStart);
            if (promptEnd == -1) {
                break;
            }

            String promptXml = documentXml.substring(promptStart, promptEnd + promptEndTag.length());

            try {
                prompts.add(parsePromptFromXml(promptXml));
            } catch (Exception e) {
                logger.error("Error parsing individual prompt", e);
                logger.debug("Prompt XML: {}", promptXml);
            }

            startIndex = promptEnd + promptEndTag.length();
        }

        return prompts;
    }

    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Parses a status string into an AgentState.
     *
     * @param statusStr the status string to parse
     * @return AgentState instance, defaults to CREATING if unknown
     */
    private AgentState parseAgentState(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return AgentState.CREATING();
        }

        String upperStatus = statusStr.toUpperCase().trim();

        return switch (upperStatus) {
            case "CREATING" -> AgentState.CREATING();
            case "RUNNING" -> AgentState.RUNNING();
            case "FINISHED" -> AgentState.FINISHED();
            case "ERROR" -> AgentState.ERROR();
            case "EXPIRED" -> AgentState.EXPIRED();
            default -> AgentState.CREATING();
        };
    }

}
