package info.jab.churrera.cli.service;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.service.handler.SequenceWorkflowHandler;
import info.jab.churrera.cli.service.handler.ParallelWorkflowHandler;
import info.jab.churrera.cli.service.handler.ChildWorkflowHandler;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Background job processor that automatically processes unfinished jobs,
 * launching and tracking Cursor agents using traditional threading.
 *
 * This class orchestrates job processing by delegating to specialized handlers
 * for different workflow types (sequence, parallel, child).
 */
public class JobProcessor {

    private static final Logger logger = LoggerFactory.getLogger(JobProcessor.class);

    private final JobRepository jobRepository;
    private final WorkflowFileService workflowFileService;
    private final SequenceWorkflowHandler sequenceWorkflowHandler;
    private final ParallelWorkflowHandler parallelWorkflowHandler;
    private final ChildWorkflowHandler childWorkflowHandler;

    // Public constructor for dependency injection
    public JobProcessor(JobRepository jobRepository, CLIAgent cliAgent, WorkflowParser workflowParser) {
        this.jobRepository = jobRepository;

        // Initialize services
        this.workflowFileService = new WorkflowFileService(workflowParser);
        TimeoutManager timeoutManager = new TimeoutManager(jobRepository);
        AgentLauncher agentLauncher = new AgentLauncher(cliAgent, jobRepository, workflowFileService);
        PromptProcessor promptProcessor = new PromptProcessor(cliAgent, workflowFileService);
        FallbackExecutor fallbackExecutor = new FallbackExecutor(cliAgent, jobRepository, workflowFileService);
        ResultExtractor resultExtractor = new ResultExtractor(cliAgent, jobRepository);

        // Initialize handlers
        this.sequenceWorkflowHandler = new SequenceWorkflowHandler(jobRepository, cliAgent, agentLauncher, promptProcessor, timeoutManager, fallbackExecutor);
        this.parallelWorkflowHandler = new ParallelWorkflowHandler(jobRepository, cliAgent, agentLauncher, timeoutManager, fallbackExecutor, resultExtractor);
        this.childWorkflowHandler = new ChildWorkflowHandler(jobRepository, cliAgent, agentLauncher, promptProcessor, timeoutManager, fallbackExecutor);
    }

    /**
     * Main processing loop that finds and processes unfinished jobs.
     * This method can be called periodically by a ScheduledExecutorService.
     */
    public void processJobs() {
        try {
            List<Job> unfinishedJobs = jobRepository.findUnfinishedJobs();

            if (unfinishedJobs.isEmpty()) {
                logger.debug("No unfinished jobs found");
                return; // No jobs to process
            }

            logger.info("Found {} unfinished job(s): {}", unfinishedJobs.size(),
                unfinishedJobs.stream().map(Job::jobId).toList());

            for (Job job : unfinishedJobs) {
                try {
                    logger.info("Processing job: {} (cursorAgentId: {}, status: {})",
                        job.jobId(), job.cursorAgentId(), job.status());
                    processJob(job);
                } catch (Exception e) {
                    logger.error("Error processing job {}: {}", job.jobId(), e.getMessage());
                    // Continue with other jobs
                }
            }
        } catch (Exception e) {
            logger.error("Error finding unfinished jobs: {}", e.getMessage());
        }
    }

    /**
     * Process a single job by launching agents and executing prompts.
     */
    private void processJob(Job job) {
        try {
            logger.info("Starting to process job: {} (current status: {})", job.jobId(), job.status());

            // Get job details with prompts
            var jobDetailsOpt = jobRepository.findJobWithDetails(job.jobId());
            if (jobDetailsOpt.isEmpty()) {
                logger.error("Job details not found for job: {}", job.jobId());
                return;
            }

            var jobDetails = jobDetailsOpt.get();
            List<Prompt> prompts = jobDetails.getPrompts();

            if (prompts.isEmpty()) {
                logger.error("No prompts found for job: {}", job.jobId());
                return;
            }

            logger.info("Found {} prompts for job: {} (status before workflow: {})", prompts.size(), job.jobId(), job.status());

            // Process the job workflow
            processJobWorkflow(job, prompts);

            logger.info("Finished processing job: {}", job.jobId());

        } catch (Exception e) {
            logger.error("Error processing job {}: {}", job.jobId(), e.getMessage(), e);
        }
    }

    /**
     * Process a job workflow by launching it if needed and executing prompts.
     * Routes to the appropriate handler based on workflow type.
     */
    private void processJobWorkflow(Job job, List<Prompt> prompts) {
        try {
            logger.info("Starting to parse workflow for job: {} (status: {})", job.jobId(), job.status());

            // Parse workflow to get PML files
            WorkflowData workflowData = workflowFileService.parseWorkflow(job.path());
            logger.info("Workflow parsed successfully for job: {}", job.jobId());

            // Check if this is a child job (from parallel workflow)
            if (job.parentJobId() != null) {
                logger.info("Detected child job: {} (parent: {}, status: {})", job.jobId(), job.parentJobId(), job.status());
                // Child jobs are processed as sequence workflows
                // They inherit the sequence from parent's parallel workflow
                childWorkflowHandler.processWorkflow(job, workflowData, prompts);
                return;
            }

            // Check if this is a parallel workflow
            if (workflowData.isParallelWorkflow()) {
                logger.info("Detected parallel workflow for job: {} (status: {})", job.jobId(), job.status());
                parallelWorkflowHandler.processWorkflow(job, workflowData);
                return;
            }

            // Standard sequence workflow processing
            sequenceWorkflowHandler.processWorkflow(job, prompts, workflowData);

        } catch (Exception e) {
            logger.error("Error processing job workflow {}: {}", job.jobId(), e.getMessage(), e);
        }
    }
}
