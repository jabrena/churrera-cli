package info.jab.churrera.cli.di;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.util.PropertyResolver;
import info.jab.churrera.util.CursorApiKeyResolver;
import info.jab.churrera.util.PmlConverter;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowValidator;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import java.io.IOException;

/**
 * Dagger module providing core Churrera dependencies.
 */
@Module
public class ChurreraModule {

    @Provides
    @Singleton
    PropertyResolver providePropertyResolver() {
        return new PropertyResolver();
    }

    @Provides
    @Singleton
    CursorApiKeyResolver provideCursorApiKeyResolver() {
        return new CursorApiKeyResolver();
    }

    @Provides
    @Singleton
    String provideApiKey(CursorApiKeyResolver apiKeyResolver) {
        try {
            return apiKeyResolver.resolveApiKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve API key", e);
        }
    }

    @Provides
    @Singleton
    JobRepository provideJobRepository(PropertyResolver propertyResolver) {
        try {
            return new JobRepository(propertyResolver);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create JobRepository", e);
        }
    }

    @Provides
    @Singleton
    WorkflowParser provideWorkflowParser() {
        return new WorkflowParser();
    }

    @Provides
    @Singleton
    WorkflowValidator provideWorkflowValidator() {
        return new WorkflowValidator();
    }

    @Provides
    @Singleton
    PmlValidator providePmlValidator() {
        return new PmlValidator();
    }

    @Provides
    @Singleton
    PmlConverter providePmlConverter() {
        return new PmlConverter();
    }
}
