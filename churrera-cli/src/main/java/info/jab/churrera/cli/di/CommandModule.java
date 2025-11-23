package info.jab.churrera.cli.di;

import info.jab.churrera.cli.command.run.*;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.util.PropertyResolver;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowValidator;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Dagger module providing command-related dependencies.
 */
@Module
public class CommandModule {

    @Provides
    @Singleton
    @Named("pollingIntervalSeconds")
    int providePollingIntervalSeconds(PropertyResolver propertyResolver) {
        return propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds")
                .map(Integer::parseInt)
                .orElseThrow(() -> new RuntimeException("Required property 'cli.polling.interval.seconds' not found in application.properties"));
    }

    @Provides
    @Singleton
    JobCreationService provideJobCreationService(
            JobRepository jobRepository,
            WorkflowValidator workflowValidator,
            WorkflowParser workflowParser,
            PmlValidator pmlValidator,
            CLIAgent cliAgent) {
        return new JobCreationService(jobRepository, workflowValidator, workflowParser, pmlValidator, cliAgent);
    }

    @Provides
    @Singleton
    JobDisplayService provideJobDisplayService(JobRepository jobRepository) {
        return new JobDisplayService(jobRepository);
    }

    @Provides
    @Singleton
    JobDeletionService provideJobDeletionService(JobRepository jobRepository, CLIAgent cliAgent) {
        return new JobDeletionService(jobRepository, cliAgent);
    }

    @Provides
    @Singleton
    JobLogDisplayService provideJobLogDisplayService(JobRepository jobRepository, CLIAgent cliAgent) {
        return new JobLogDisplayService(jobRepository, cliAgent);
    }

    @Provides
    @Singleton
    CompletionCheckerFactory provideCompletionCheckerFactory(JobRepository jobRepository) {
        return new CompletionCheckerFactory(jobRepository);
    }
}
