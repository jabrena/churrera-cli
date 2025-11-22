package info.jab.churrera.cli.command.run;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CompletionCheckerFactoryTest {

    @Mock
    private JobRepository jobRepository;

    private CompletionCheckerFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CompletionCheckerFactory(jobRepository);
    }

    @Test
    void shouldCreateParallelCheckerForParallelWorkflow() {
        CompletionChecker checker = factory.create(WorkflowType.PARALLEL);

        assertThat(checker).isInstanceOf(ParallelWorkflowCompletionChecker.class);
    }

    @Test
    void shouldCreateSimpleCheckerForSequenceWorkflow() {
        CompletionChecker checker = factory.create(WorkflowType.SEQUENCE);

        assertThat(checker).isInstanceOf(SimpleWorkflowCompletionChecker.class);
    }

    @Test
    void shouldDefaultToSimpleCheckerWhenTypeIsNull() {
        CompletionChecker checker = factory.create(null);

        assertThat(checker).isInstanceOf(SimpleWorkflowCompletionChecker.class);
    }
}

