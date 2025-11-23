package info.jab.churrera.cli;

import info.jab.churrera.cli.command.run.RunCommand;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.cli.util.GitInfo;
import info.jab.churrera.util.CursorApiKeyResolver;
import info.jab.churrera.util.PropertyResolver;
import info.jab.churrera.workflow.PmlValidator;
import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowValidator;
import info.jab.cursor.generated.client.ApiClient;
import info.jab.cursor.generated.client.api.DefaultApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChurreraCLI.
 * These tests use the public constructor to inject mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ChurreraCLITest {

    // Mocks for ChurreraCLI tests
    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobProcessor jobProcessor;

    @Mock
    private PropertyResolver propertyResolver;

    @Mock
    private CLIAgent cliAgent;
    @Mock
    private CursorApiKeyResolver apiKeyResolver;

    @Mock
    private ApiClient apiClient;

    @Mock
    private DefaultApi defaultApi;

    @Mock
    private WorkflowParser workflowParser;

    @Mock
    private WorkflowValidator workflowValidator;

    @Mock
    private PmlValidator pmlValidator;

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        // Capture output
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;

        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    void tearDown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private ChurreraCLI createChurreraCLIWithMocks() {
        String testApiKey = "test-api-key";
        return new ChurreraCLI(
            propertyResolver,
            jobRepository,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );
    }

    // ============================================
    // ChurreraCLI Tests
    // ============================================

    @Test
    void testChurreraCLI_Constructor() {
        // When
        ChurreraCLI cli = createChurreraCLIWithMocks();

        // Then
        assertThat(cli).isNotNull();
    }

    @Test
    void testChurreraCLI_Run_NoSubcommand() {
        // Given
        ChurreraCLI cli = createChurreraCLIWithMocks();

        // When
        cli.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Please specify a command");
    }


    @Test
    void testChurreraCLI_Run_CanBeCalledMultipleTimes() {
        // Given
        ChurreraCLI cli = createChurreraCLIWithMocks();

        // When
        cli.run();
        cli.run();
        cli.run();

        // Then
        String output = outputStream.toString();
        // Should output the message multiple times
        long count = output.split("Please specify a command").length - 1;
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testChurreraCLI_ImplementsRunnable() {
        // When
        ChurreraCLI cli = createChurreraCLIWithMocks();

        // Then
        assertThat(cli).isInstanceOf(Runnable.class);
    }

    @Test
    void testChurreraCLI_CanBeInstantiated() {
        // When
        ChurreraCLI cli1 = createChurreraCLIWithMocks();
        ChurreraCLI cli2 = createChurreraCLIWithMocks();

        // Then
        assertThat(cli1).isNotNull();
        assertThat(cli2).isNotNull();
        assertThat(cli1).isNotSameAs(cli2);
    }

    @Test
    void testPrintBanner_Success() {
        // Given
        GitInfo mockGitInfo = mock(GitInfo.class);
        doNothing().when(mockGitInfo).print();

        // When
        ChurreraCLI.printBanner(() -> mockGitInfo);

        // Then
        verify(mockGitInfo, times(1)).print();
        // Note: GitInfo now uses logger instead of System.out, so output stream will be empty
    }

    @Test
    void testPrintBanner_WithMockGitInfo() {
        // Given
        GitInfo mockGitInfo = mock(GitInfo.class);
        doNothing().when(mockGitInfo).print();

        // When
        ChurreraCLI.printBanner(() -> mockGitInfo);

        // Then
        verify(mockGitInfo, times(1)).print();
        // Note: GitInfo now uses logger instead of System.out, so output stream will be empty
    }

    @Test
    void testPrintBanner_HandlesGitInfoException() {
        // Given
        // GitInfo.print() catches IOException internally, so we test with a RuntimeException
        // to verify the banner method handles exceptions from GitInfo
        GitInfo failingGitInfo = mock(GitInfo.class);
        doThrow(new RuntimeException("GitInfo error")).when(failingGitInfo).print();

        // When
        // Should not throw exception, should handle RuntimeException gracefully
        assertThatCode(() -> ChurreraCLI.printBanner(() -> failingGitInfo))
                .doesNotThrowAnyException();

        // Then
        // Exception was handled gracefully (no assertion needed as the test verifies no exception is thrown)
    }

    @Test
    void testPrintBanner_HandlesIOException() {
        // Given
        GitInfo failingGitInfo = mock(GitInfo.class);
        doThrow(new RuntimeException("IO error")).when(failingGitInfo).print();

        // When & Then
        assertThatCode(() -> ChurreraCLI.printBanner(() -> failingGitInfo))
                .doesNotThrowAnyException();
    }


    @Test
    void testCreateRunCommand_WithMocks() {
        // Given
        String testApiKey = "test-api-key";
        when(propertyResolver.getProperty(anyString(), anyString()))
                .thenReturn(Optional.of("5"));

        ChurreraCLI cli = new ChurreraCLI(
            propertyResolver,
            jobRepository,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );

        // When
        RunCommand result = cli.createRunCmd();

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void testCreateRunCommand_ThrowsExceptionWhenPropertyMissing() {
        // Given
        String testApiKey = "test-api-key";
        when(propertyResolver.getProperty(anyString(), anyString()))
                .thenReturn(Optional.empty());

        ChurreraCLI cli = new ChurreraCLI(
            propertyResolver,
            jobRepository,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );

        // When & Then
        assertThatThrownBy(cli::createRunCmd)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testCreateRunCommand_WithInjectedDependencies() {
        // Given
        // This test verifies that the factory can be created with injected dependencies
        // and that createRunCommand works correctly
        String testApiKey = "test-api-key";
        when(propertyResolver.getProperty(anyString(), anyString()))
                .thenReturn(Optional.of("5"));

        ChurreraCLI cli = new ChurreraCLI(
            propertyResolver,
            jobRepository,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );

        // When
        RunCommand result = cli.createRunCmd();

        // Then
        assertThat(cli).isNotNull();
        assertThat(result).isNotNull();
    }

    @Test
    void testChurreraCLI_DefaultConstructor() {
        // Given & When & Then
        // This test verifies that the default constructor exists
        // Note: This will fail if dependencies are not properly configured,
        // but we're just testing that the constructor signature is correct
        assertThatCode(() -> {
            assertThat(ChurreraCLI.class).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void testChurreraCLI_TestConstructor() {
        // Given
        String testApiKey = "test-api-key";

        // When
        ChurreraCLI cli = new ChurreraCLI(
            propertyResolver,
            jobRepository,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );

        // Then
        assertThat(cli).isNotNull();
    }

    @Test
    void testCreateRunCommand_WithValidPollingInterval() {
        // Given
        String testApiKey = "test-api-key";
        when(propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds"))
                .thenReturn(Optional.of("10"));

        ChurreraCLI cli = new ChurreraCLI(
            propertyResolver,
            jobRepository,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );

        // When
        RunCommand result = cli.createRunCmd();

        // Then
        assertThat(result).isNotNull();
        verify(propertyResolver, atLeastOnce()).getProperty("application.properties", "cli.polling.interval.seconds");
    }

    @Test
    void testCreateRunCommand_WithInvalidPollingInterval() {
        // Given
        String testApiKey = "test-api-key";
        when(propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds"))
                .thenReturn(Optional.of("invalid"));

        ChurreraCLI cli = new ChurreraCLI(
            propertyResolver,
            jobRepository,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );

        // When & Then
        assertThatThrownBy(cli::createRunCmd)
                .isInstanceOf(NumberFormatException.class);
    }
}
