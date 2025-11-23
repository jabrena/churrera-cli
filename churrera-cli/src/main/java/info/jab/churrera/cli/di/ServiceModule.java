package info.jab.churrera.cli.di;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.*;
import info.jab.churrera.cli.service.handler.ChildWorkflowHandler;
import info.jab.churrera.cli.service.handler.ParallelWorkflowHandler;
import info.jab.churrera.cli.service.handler.SequenceWorkflowHandler;
import info.jab.cursor.client.CursorAgentGeneralEndpoints;
import info.jab.cursor.client.CursorAgentInformation;
import info.jab.cursor.client.CursorAgentManagement;
import info.jab.churrera.util.PmlConverter;
import info.jab.churrera.workflow.WorkflowParser;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Dagger module providing service layer dependencies.
 */
@Module
public class ServiceModule {

    @Provides
    @Singleton
    WorkflowFileService provideWorkflowFileService(WorkflowParser workflowParser) {
        return new WorkflowFileService(workflowParser);
    }

    @Provides
    @Singleton
    TimeoutManager provideTimeoutManager(JobRepository jobRepository) {
        return new TimeoutManager(jobRepository);
    }

    @Provides
    @Singleton
    CLIAgent provideCLIAgent(
            JobRepository jobRepository,
            CursorAgentManagement cursorAgentManagement,
            CursorAgentInformation cursorAgentInformation,
            CursorAgentGeneralEndpoints cursorAgentGeneralEndpoints,
            PmlConverter pmlConverter) {
        return new CLIAgent(
                jobRepository,
                cursorAgentManagement,
                cursorAgentInformation,
                cursorAgentGeneralEndpoints,
                pmlConverter);
    }

    @Provides
    @Singleton
    AgentLauncher provideAgentLauncher(
            CLIAgent cliAgent,
            JobRepository jobRepository,
            WorkflowFileService workflowFileService) {
        return new AgentLauncher(cliAgent, jobRepository, workflowFileService);
    }

    @Provides
    @Singleton
    PromptProcessor providePromptProcessor(
            CLIAgent cliAgent,
            WorkflowFileService workflowFileService) {
        return new PromptProcessor(cliAgent, workflowFileService);
    }

    @Provides
    @Singleton
    FallbackExecutor provideFallbackExecutor(
            CLIAgent cliAgent,
            JobRepository jobRepository,
            WorkflowFileService workflowFileService) {
        return new FallbackExecutor(cliAgent, jobRepository, workflowFileService);
    }

    @Provides
    @Singleton
    ResultExtractor provideResultExtractor(
            CLIAgent cliAgent,
            JobRepository jobRepository) {
        return new ResultExtractor(cliAgent, jobRepository);
    }

    @Provides
    @Singleton
    SequenceWorkflowHandler provideSequenceWorkflowHandler(
            JobRepository jobRepository,
            CLIAgent cliAgent,
            AgentLauncher agentLauncher,
            PromptProcessor promptProcessor,
            TimeoutManager timeoutManager,
            FallbackExecutor fallbackExecutor) {
        return new SequenceWorkflowHandler(
                jobRepository,
                cliAgent,
                agentLauncher,
                promptProcessor,
                timeoutManager,
                fallbackExecutor);
    }

    @Provides
    @Singleton
    ParallelWorkflowHandler provideParallelWorkflowHandler(
            JobRepository jobRepository,
            CLIAgent cliAgent,
            AgentLauncher agentLauncher,
            TimeoutManager timeoutManager,
            FallbackExecutor fallbackExecutor,
            ResultExtractor resultExtractor) {
        return new ParallelWorkflowHandler(
                jobRepository,
                cliAgent,
                agentLauncher,
                timeoutManager,
                fallbackExecutor,
                resultExtractor);
    }

    @Provides
    @Singleton
    ChildWorkflowHandler provideChildWorkflowHandler(
            JobRepository jobRepository,
            CLIAgent cliAgent,
            AgentLauncher agentLauncher,
            PromptProcessor promptProcessor,
            TimeoutManager timeoutManager,
            FallbackExecutor fallbackExecutor) {
        return new ChildWorkflowHandler(
                jobRepository,
                cliAgent,
                agentLauncher,
                promptProcessor,
                timeoutManager,
                fallbackExecutor);
    }

    @Provides
    @Singleton
    JobProcessor provideJobProcessor(
            JobRepository jobRepository,
            WorkflowFileService workflowFileService,
            SequenceWorkflowHandler sequenceWorkflowHandler,
            ParallelWorkflowHandler parallelWorkflowHandler,
            ChildWorkflowHandler childWorkflowHandler) {
        return new JobProcessor(
                jobRepository,
                workflowFileService,
                sequenceWorkflowHandler,
                parallelWorkflowHandler,
                childWorkflowHandler);
    }
}
