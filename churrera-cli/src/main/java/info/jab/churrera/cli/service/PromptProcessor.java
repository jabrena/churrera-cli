package info.jab.churrera.cli.service;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * Service for processing prompts and follow-ups for jobs.
 */
public class PromptProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PromptProcessor.class);

    private final CLIAgent cliAgent;
    private final WorkflowFileService workflowFileService;

    @Inject
    public PromptProcessor(CLIAgent cliAgent, WorkflowFileService workflowFileService) {
        this.cliAgent = cliAgent;
        this.workflowFileService = workflowFileService;
    }

    /**
     * Process remaining prompts that haven't been executed yet.
     *
     * @param job the job to process prompts for
     * @param prompts the list of prompts
     * @param workflowData the workflow data containing prompt information
     */
    public void processRemainingPrompts(Job job, List<Prompt> prompts, WorkflowData workflowData) {
        try {
            List<PromptInfo> updatePrompts = workflowData.getUpdatePrompts();

            for (int i = 0; i < updatePrompts.size() && i < prompts.size(); i++) {
                Prompt prompt = prompts.get(i);
                PromptInfo promptInfo = updatePrompts.get(i);

                // Check if prompt has been processed (status not UNKNOWN)
                if ("UNKNOWN".equals(prompt.status())) {
                    processPrompt(job, prompt, promptInfo);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing remaining prompts for job {}: {}", job.jobId(), e.getMessage());
        }
    }

    /**
     * Process a single prompt by sending it as a follow-up to the agent.
     *
     * @param job the job to process the prompt for
     * @param prompt the prompt to process
     * @param promptInfo the prompt info from workflow
     */
    public void processPrompt(Job job, Prompt prompt, PromptInfo promptInfo) {
        try {
            // Only send the prompt if it hasn't been sent yet
            if ("UNKNOWN".equals(prompt.status())) {
                // Read the prompt file
                String promptContent = workflowFileService.readPromptFile(job.path(), promptInfo.getSrcFile());

                // Get bind value from job result if present (for child jobs from parallel workflows)
                // Only apply if the prompt has bindResultExp attribute
                String bindValue = null;
                if (promptInfo.hasBindResultExp() && job.result() != null) {
                    bindValue = job.result();
                    logger.info("Prompt has bindResultExp, will apply value: {}", bindValue);
                }

                // Send follow-up prompt with type information and optional bind value
                cliAgent.followUpForPrompt(job.cursorAgentId(), promptContent, promptInfo.getType(), bindValue);

                // Update prompt status to SENT
                cliAgent.updatePromptInDatabase(prompt, "SENT");
                cliAgent.updateJobInDatabase(job);

                logger.info("Sent follow-up prompt {} for job {} (type: {})", prompt.promptId(), job.jobId(), promptInfo.getType());

                // Don't block on monitoring - the job status will be checked on next polling cycle
                logger.info("Prompt {} sent, job {} status will be checked on next polling cycle", prompt.promptId(), job.jobId());
            } else if ("SENT".equals(prompt.status())) {
                // Prompt was sent, check if the job has completed
                AgentState currentStatus = cliAgent.getAgentStatus(job.cursorAgentId());

                if (currentStatus.isTerminal()) {
                    // Update prompt status based on job completion
                    cliAgent.updatePromptInDatabase(prompt, currentStatus.isSuccessful() ? "COMPLETED" : "FAILED");
                    cliAgent.updateJobInDatabase(job);
                    logger.info("Prompt {} finished with status: {}", prompt.promptId(), currentStatus);
                } else {
                    logger.info("Prompt {} still processing, job {} is in state: {}", prompt.promptId(), job.jobId(), currentStatus);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing prompt {}: {}", prompt.promptId(), e.getMessage());
            try {
                cliAgent.updatePromptInDatabase(prompt, "ERROR");
                cliAgent.updateJobInDatabase(job);
            } catch (Exception updateError) {
                logger.error("Error updating prompt status: {}", updateError.getMessage());
            }
        }
    }
}

