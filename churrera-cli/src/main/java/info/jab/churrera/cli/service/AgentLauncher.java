package info.jab.churrera.cli.service;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Service for launching Cursor agents for jobs.
 */
public class AgentLauncher {

    private static final Logger logger = LoggerFactory.getLogger(AgentLauncher.class);

    private final CLIAgent cliAgent;
    private final JobRepository jobRepository;
    private final WorkflowFileService workflowFileService;

    public AgentLauncher(CLIAgent cliAgent, JobRepository jobRepository, 
                        WorkflowFileService workflowFileService) {
        this.cliAgent = cliAgent;
        this.jobRepository = jobRepository;
        this.workflowFileService = workflowFileService;
    }

    /**
     * Launch a new agent with the first prompt.
     *
     * @param job the job to launch
     * @param workflowData the workflow data containing prompt information
     */
    public void launchJobAgent(Job job, WorkflowData workflowData) {
        try {
            // Get launch prompt info
            PromptInfo launchPrompt = workflowData.getLaunchPrompt();

            // Read the launch prompt file
            String promptContent = workflowFileService.readPromptFile(job.path(), launchPrompt.getSrcFile());

            // Get bind value from job result if present (for child jobs from parallel workflows)
            // Only apply if the prompt has bindResultExp attribute
            String bindValue = null;
            if (launchPrompt.hasBindResultExp() && job.result() != null) {
                bindValue = job.result();
                logger.info("Prompt has bindResultExp, will apply value: {}", bindValue);
            }

            // Determine PR flag: parallel workflow parent jobs should not create PR (only generate list),
            // sequence workflows (standard and child jobs) should create PR)
            boolean createPr = !workflowData.isParallelWorkflow();
            logger.info("Launching agent with PR flag: {} (workflow type: {})", createPr,
                workflowData.isParallelWorkflow() ? "parallel" : "sequence");

            // Launch the agent with type information, optional bind value, and PR flag
            String cursorAgentId = cliAgent.launchAgentForJob(job, promptContent, launchPrompt.getType(), bindValue, createPr);

            // Update job in database with cursorAgentId and CREATING status
            Job updatedJob = job.withCursorAgentId(cursorAgentId);
            cliAgent.updateJobCursorIdInDatabase(updatedJob, cursorAgentId, AgentState.creating());

            // Reset workflowStartTime to now when launching (even if it already exists, to start fresh)
            if (job.timeoutMillis() != null) {
                updatedJob = updatedJob.withWorkflowStartTime(LocalDateTime.now());
                jobRepository.save(updatedJob);
                logger.info("Set workflowStartTime for job {} with timeout {}ms (workflow start time reset to now)",
                    job.jobId(), job.timeoutMillis());
            }

            logger.info("Launched job {} with Cursor ID: {} (type: {})", job.jobId(), cursorAgentId, launchPrompt.getType());

        } catch (Exception e) {
            logger.error("Error launching job {}: {}", job.jobId(), e.getMessage());
            // Mark job as failed if launch fails
            try {
                cliAgent.updateJobStatusInDatabase(job, AgentState.error());
            } catch (Exception updateError) {
                logger.error("Error updating job status to FAILED: {}", updateError.getMessage());
            }
        }
    }
}

