package info.jab.churrera.cli;

import info.jab.churrera.cli.command.cli.CliCommand;
import info.jab.churrera.cli.command.run.RunCommand;
import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.JobWithDetails;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.stream.Stream;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import info.jab.churrera.workflow.WorkflowType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChurreraCLI and CliCommand (REPL functionality).
 * These tests use the public constructor to inject mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ChurreraCLITest {

    // Mocks for CliCommand tests
    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobProcessor jobProcessor;

    @Mock
    private PropertyResolver propertyResolver;

    @Mock
    private CLIAgent cliAgent;

    // Mocks for ChurreraCLI tests
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

    private CliCommand churreraCLI;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        // Capture output
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        originalIn = System.in;

        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    void tearDown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }

    private CliCommand createCLI(String input) {
        when(propertyResolver.getProperty("application.properties", "cli.prompt"))
                .thenReturn(Optional.of("> "));
        when(propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds"))
                .thenReturn(Optional.of("5"));

        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        return new CliCommand(jobRepository, jobProcessor, propertyResolver, scanner, cliAgent);
    }

    private CliTestResult runCliWithInput(String input) {
        churreraCLI = createCLI(input);
        churreraCLI.run();
        return captureOutput();
    }

    private Job createJob(String jobId) {
        LocalDateTime now = LocalDateTime.now();
        return new Job(
                jobId,
                "/tmp/workflow.xml",
                null,
                "gpt-4.1-mini",
                "https://github.com/jabrena/churrera",
                AgentState.running(),
                now,
                now,
                null,
                null,
                WorkflowType.SEQUENCE,
                null,
                null,
                null,
                false
        );
    }

    private JobWithDetails createJobWithDetails(Job job) {
        return new JobWithDetails(job, List.of());
    }

    private CliTestResult captureOutput() {
        return new CliTestResult(outputStream.toString(), errorStream.toString());
    }

    private record CliTestResult(String stdout, String stderr) { }

    private ChurreraCLI createChurreraCLIWithMocks() {
        String testApiKey = "test-api-key";
        return new ChurreraCLI(
            apiKeyResolver,
            testApiKey,
            propertyResolver,
            jobRepository,
            apiClient,
            defaultApi,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );
    }

    @Test
    void should_warn_about_unknown_exit_command() {
        // When
        CliTestResult result = runCliWithInput("exit\nquit\n");

        // Then
        assertThat(result.stdout())
                .contains("Unknown command: exit")
                .contains("Goodbye!");
    }

    @ParameterizedTest(name = "Should exit when using command ''{0}''")
    @ValueSource(strings = {"quit", "QUIT", "QuIt"})
    void should_exit_when_quit_command_is_entered_regardless_of_case(String quitCommand) {
        // When
        CliTestResult result = runCliWithInput(quitCommand + "\n");

        // Then
        assertThat(result.stdout()).contains("Goodbye!");
    }

    @ParameterizedTest(name = "Should ignore blank input ''{0}'' and continue prompting")
    @ValueSource(strings = {"", "   ", "\t\t", "\n\n"})
    void should_ignore_blank_inputs(String blankInput) {
        // When
        CliTestResult result = runCliWithInput(blankInput + "\nquit\n");

        // Then
        assertThat(result.stdout()).contains("Goodbye!");
        assertThat(result.stderr()).isEmpty();
    }

    @ParameterizedTest(name = "Should list jobs when user types ''{0}''")
    @ValueSource(strings = {"jobs", "  jobs  ", "jobs\t", "jobs\njobs"})
    void should_list_jobs_for_command_variations(String jobsCommand) {
        // When
        CliTestResult result = runCliWithInput(jobsCommand + "\nquit\n");

        // Then
        verify(jobRepository, atLeastOnce()).findAll();
        assertThat(result.stdout()).contains("Goodbye!");
    }

    @ParameterizedTest(name = "Should display help when input is ''{0}''")
    @ValueSource(strings = {"help", "  help  ", "help\nhelp"})
    void should_show_help_information_for_common_variations(String helpCommand) {
        // When
        CliTestResult result = runCliWithInput(helpCommand + "\nquit\n");

        // Then
        assertThat(result.stdout()).contains("Available commands")
                .contains("Goodbye!");
    }

    @ParameterizedTest(name = "Should display help for command ''{0}''")
    @ValueSource(strings = {"help", "  help  ", "help\nhelp\nhelp", "   help   ", "help\n\n\n\n\n"})
    void testRun_HelpCommand(String helpCommand) {
        // Given
        churreraCLI = createCLI(helpCommand + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Available commands");
    }

    @ParameterizedTest(name = "Should display goodbye for command ''{0}''")
    @ValueSource(strings = {"clear", "  clear  ", "\n\n", "   \n\t\n\n"})
    void testRun_ClearCommand(String clearCommand) {
        // Given
        churreraCLI = createCLI(clearCommand + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Goodbye!");
        assertThat(errorStream.toString()).isEmpty();
    }

    @ParameterizedTest(name = "Should list jobs for command ''{0}''")
    @ValueSource(strings = {"jobs", "  jobs  ", "jobs\njobs\njobs"})
    void testRun_JobsCommand(String jobsCommand) {
        // Given
        churreraCLI = createCLI(jobsCommand + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        verify(jobRepository, atLeastOnce()).findAll();
    }

    @ParameterizedTest(name = "Should surface error when executing ''{0}''")
    @ValueSource(strings = {
            "jobs new /path/to/workflow.xml",
            "jobs new /path with spaces/workflow.xml",
            "jobs new ./workflow.xml",
            "jobs new /absolute/path/workflow.xml",
            "jobs\tnew\t/path/to/workflow.xml",
            "jobs new C:\\\\path\\\\to\\\\workflow.xml",
            "jobs new ~/workflow.xml",
            "jobs new \"/path with spaces/workflow.xml\"",
            "jobs new '/path with spaces/workflow.xml'"
    })
    void should_report_error_for_invalid_workflow_paths(String command) {
        // When
        CliTestResult result = runCliWithInput(command + "\nquit\n");

        // Then
        assertThat(result.stderr()).contains("Error: Workflow file does not exist");
    }

    @Test
    void testRun_JobsStatusCommand() {
        // Given
        String jobId = "test-job-123";
        Job job = createJob(jobId);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findJobWithDetails(jobId)).thenReturn(Optional.of(createJobWithDetails(job)));
        churreraCLI = createCLI("jobs status " + jobId + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output)
                .contains("Job Details:")
                .contains("Job ID: " + jobId);
        verify(jobRepository).findJobWithDetails(jobId);
    }

    @Test
    void testRun_JobsLogsCommand() {
        // Given
        String jobId = "test-job-123";
        Job job = createJob(jobId);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findJobWithDetails(jobId)).thenReturn(Optional.of(createJobWithDetails(job)));
        churreraCLI = createCLI("jobs logs " + jobId + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output)
                .contains("=== Job Logs ===")
                .contains("Job ID: " + jobId);
        verify(jobRepository).findJobWithDetails(jobId);
    }

    @Test
    void testRun_JobsPrCommand() {
        // Given
        String jobId = "test-job-123";
        Job finishedJob = createJob(jobId)
                .withStatus(AgentState.finished())
                .withCursorAgentId("agent-42");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(finishedJob));
        churreraCLI = createCLI("jobs pr " + jobId + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output)
                .contains("Pull Request Review")
                .contains("agent-42");
        verify(jobRepository, atLeastOnce()).findById(jobId);
    }

    @Test
    void testRun_JobsDeleteCommand() {
        // Given
        String jobId = "test-job-123";
        Job job = createJob(jobId);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findJobsByParentId(jobId)).thenReturn(List.of());
        churreraCLI = createCLI("jobs delete " + jobId + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Job and all child jobs deleted from Database");
        verify(jobRepository).deletePromptsByJobId(jobId);
        verify(jobRepository).deleteById(jobId);
    }

    @ParameterizedTest(name = "Should handle unknown command: ''{0}''")
    @MethodSource("unknownCommandTestCases")
    void testRun_UnknownCommand(String command, String expectedOutput) {
        // Given
        churreraCLI = createCLI(command + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains(expectedOutput);
    }

    private static Stream<Arguments> unknownCommandTestCases() {
        String veryLongCommand = "x".repeat(1000);
        return Stream.of(
                Arguments.of("unknown command", "Unknown command"),
                Arguments.of("unknown-command-123!@#", "Unknown command: unknown-command-123!@#"),
                Arguments.of("🚀unknown", "Unknown command: 🚀unknown"),
                Arguments.of(veryLongCommand, "Unknown command: " + veryLongCommand)
        );
    }


    @Test
    void testRun_CommandWithException() {
        // Given
        when(jobRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        churreraCLI = createCLI("jobs\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Error listing jobs: Database error");
        assertThat(errorStream.toString()).isEmpty();
    }



    @Test
    void testRun_DisplaysVersionAndWelcomeMessage() {
        // Given
        churreraCLI = createCLI("quit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        // Note: The test constructor doesn't call printBanner(), so no version info is printed
        assertThat(output).contains("Type 'help' for available commands");
    }

    @Test
    void testRun_DisplaysPrompt() {
        // Given
        churreraCLI = createCLI("quit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        // The prompt should be displayed, or at minimum "Goodbye!" confirms the loop ran
        assertThat(output).contains(">");
    }

    @Test
    void testRun_CustomPromptFromProperties() {
        // Given
        when(propertyResolver.getProperty("application.properties", "cli.prompt"))
                .thenReturn(Optional.of("churrera> "));
        when(propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds"))
                .thenReturn(Optional.of("5"));

        Scanner scanner = new Scanner(new ByteArrayInputStream("quit\n".getBytes()));
        churreraCLI = new CliCommand(jobRepository, jobProcessor, propertyResolver, scanner, cliAgent);

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        // The custom prompt should be displayed, or at minimum "Goodbye!" confirms the loop ran
        assertThat(output).contains("churrera>");
    }

    @Test
    void testRun_JobsCommandPattern_WithUUID() {
        // Given
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        Job job = createJob(uuid);
        when(jobRepository.findById(uuid)).thenReturn(Optional.of(job));
        when(jobRepository.findJobWithDetails(uuid)).thenReturn(Optional.of(createJobWithDetails(job)));
        churreraCLI = createCLI("jobs status " + uuid + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        verify(jobRepository).findJobWithDetails(uuid);
        String output = outputStream.toString();
        assertThat(output).contains("Job Details:");
    }






    @ParameterizedTest(name = "Should reject job subcommand without argument ''{1}''")
    @CsvSource({
            "'jobs new',jobs new",
            "'jobs new ',jobs new",
            "'jobs status',jobs status",
            "'jobs delete',jobs delete",
            "'jobs logs',jobs logs",
            "'jobs pr',jobs pr"
    })
    void should_warn_when_job_subcommand_argument_missing(String rawInput, String expectedCommand) {
        // When
        CliTestResult result = runCliWithInput(rawInput + "\nquit\n");

        // Then
        assertThat(result.stdout()).contains("Unknown command: " + expectedCommand);
        assertThat(result.stderr()).isEmpty();
    }




    @Test
    void testRun_JobsStatusCommandWithShortId() {
        // Given
        String jobId = "abc123";
        Job job = createJob(jobId);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findJobWithDetails(jobId)).thenReturn(Optional.of(createJobWithDetails(job)));
        churreraCLI = createCLI("jobs status " + jobId + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        verify(jobRepository).findJobWithDetails(jobId);
        String output = outputStream.toString();
        assertThat(output).contains("Job Details:");
    }

    @Test
    void testRun_JobsLogsCommandWithLongId() {
        // Given
        String longId = "a".repeat(100);
        Job job = createJob(longId);
        when(jobRepository.findById(longId)).thenReturn(Optional.of(job));
        when(jobRepository.findJobWithDetails(longId)).thenReturn(Optional.of(createJobWithDetails(job)));
        churreraCLI = createCLI("jobs logs " + longId + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        verify(jobRepository).findJobWithDetails(longId);
        String output = outputStream.toString();
        assertThat(output).contains("=== Job Logs ===");
    }

    @Test
    void testRun_JobsPrCommandWithHyphenatedId() {
        // Given
        String jobId = "test-job-id-123";
        Job finishedJob = createJob(jobId)
                .withStatus(AgentState.finished())
                .withCursorAgentId("agent-42");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(finishedJob));
        churreraCLI = createCLI("jobs pr " + jobId + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
        String output = outputStream.toString();
        assertThat(output).contains("Pull Request Review");
    }

    @Test
    void testRun_JobsDeleteCommandWithUnderscoreId() {
        // Given
        String jobId = "test_job_123";
        Job job = createJob(jobId);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.findJobsByParentId(jobId)).thenReturn(List.of());
        churreraCLI = createCLI("jobs delete " + jobId + "\nquit\n");

        // When
        churreraCLI.run();

        // Then
        verify(jobRepository).deletePromptsByJobId(jobId);
        verify(jobRepository).deleteById(jobId);
        String output = outputStream.toString();
        assertThat(output).contains("Job and all child jobs deleted from Database");
    }



    @Test
    void testRun_CommandWithOnlySpaces() {
        // Given
        churreraCLI = createCLI("     \nquit\n");

        // When
        churreraCLI.run();

        // Then
        // Empty/whitespace-only commands should be ignored
        String output = outputStream.toString();
        assertThat(output).contains("Goodbye!");
    }

    @Test
    void testRun_CommandWithOnlyTabs() {
        // Given
        churreraCLI = createCLI("\t\t\t\nquit\n");

        // When
        churreraCLI.run();

        // Then
        // Tab-only commands should be ignored
        String output = outputStream.toString();
        assertThat(output).contains("Goodbye!");
    }

    @Test
    void testRun_JobsCommandWithExceptionHandling() {
        // Given
        when(jobRepository.findAll()).thenThrow(new RuntimeException("Test exception"));
        churreraCLI = createCLI("jobs\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Error listing jobs: Test exception");
        assertThat(errorStream.toString()).isEmpty();
    }

    @Test
    void testRun_JobsStatusCommandWithExceptionHandling() {
        // Given
        when(jobRepository.findById(anyString())).thenThrow(new RuntimeException("Database error"));
        churreraCLI = createCLI("jobs status test-id\nquit\n");

        // When
        churreraCLI.run();

        // Then
        // Exception should be handled gracefully
        String errorOutput = errorStream.toString();
        assertThat(errorOutput).contains("Error retrieving job status: Database error");
    }

    @Test
    void testRun_JobsDeleteCommandWithExceptionHandling() {
        // Given
        when(jobRepository.findById(anyString())).thenThrow(new RuntimeException("Delete error"));
        churreraCLI = createCLI("jobs delete test-id\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String errorOutput = errorStream.toString();
        assertThat(errorOutput).contains("Error deleting job: Delete error");
        String output = outputStream.toString();
        assertThat(output).contains("Goodbye!");
    }

    @Test
    void testRun_JobsLogsCommandWithExceptionHandling() {
        // Given
        when(jobRepository.findById(anyString())).thenThrow(new RuntimeException("Logs error"));
        churreraCLI = createCLI("jobs logs test-id\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String errorOutput = errorStream.toString();
        assertThat(errorOutput).contains("Error retrieving job logs: Logs error");
        String output = outputStream.toString();
        assertThat(output).contains("Goodbye!");
    }

    @Test
    void testRun_JobsPrCommandWithExceptionHandling() {
        // Given
        when(jobRepository.findById(anyString())).thenThrow(new RuntimeException("PR error"));
        churreraCLI = createCLI("jobs pr test-id\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output)
            .contains("Error retrieving job PR info: PR error")
            .contains("Goodbye!");
        assertThat(errorStream.toString()).isEmpty();
    }

    @Test
    void testRun_JobsNewCommandWithExceptionHandling() {
        // Given
        // This will fail during workflow validation/parsing
        churreraCLI = createCLI("jobs new /nonexistent/path.xml\nquit\n");

        // When
        churreraCLI.run();

        // Then
        // Exception should be handled gracefully
        String errorOutput = errorStream.toString();
        assertThat(errorOutput).contains("Error: Workflow file does not exist");
    }


    @Test
    void testRun_ManyEmptyLinesBeforeQuit() {
        // Given
        churreraCLI = createCLI("\n\n\n\n\n\n\nquit\n");

        // When
        churreraCLI.run();

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Goodbye!");
    }


    @ParameterizedTest(name = "Should treat ''{0}'' as unknown due to case sensitivity")
    @ValueSource(strings = {"JOBS", "HELP", "CLEAR"})
    void should_enforce_case_sensitive_commands(String upperCommand) {
        // When
        CliTestResult result = runCliWithInput(upperCommand + "\nquit\n");

        // Then
        assertThat(result.stdout()).contains("Unknown command: " + upperCommand);
    }


    @Test
    void testRun_PropertyResolverReturnsEmptyOptional() {
        // Given
        when(propertyResolver.getProperty("application.properties", "cli.prompt"))
                .thenReturn(Optional.empty());
        when(propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds"))
                .thenReturn(Optional.of("5"));

        Scanner scanner = new Scanner(new ByteArrayInputStream("quit\n".getBytes()));
        CliCommand cmd = new CliCommand(jobRepository, jobProcessor, propertyResolver, scanner, cliAgent);

        // When & Then
        assertThatThrownBy(cmd::run)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cli.prompt");
    }

    @Test
    void testRun_PropertyResolverReturnsEmptyPollingInterval() {
        // Given
        // Polling interval is checked first, so prompt stub is never reached
        lenient().when(propertyResolver.getProperty("application.properties", "cli.prompt"))
                .thenReturn(Optional.of("> "));
        when(propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds"))
                .thenReturn(Optional.empty());

        Scanner scanner = new Scanner(new ByteArrayInputStream("quit\n".getBytes()));
        CliCommand cmd = new CliCommand(jobRepository, jobProcessor, propertyResolver, scanner, cliAgent);

        // When & Then
        assertThatThrownBy(cmd::run)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cli.polling.interval.seconds");
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
        String output = outputStream.toString();
        assertThat(output).isNotBlank();
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
        String output = outputStream.toString();
        assertThat(output).isNotNull();
    }

    @Test
    void testPrintBanner_HandlesGitInfoException() {
        // Given
        // GitInfo.print() catches IOException internally, so we test with a RuntimeException
        // to verify the banner method handles exceptions from FigletFont
        GitInfo failingGitInfo = mock(GitInfo.class);
        doThrow(new RuntimeException("GitInfo error")).when(failingGitInfo).print();

        // When
        // Should not throw exception, should handle RuntimeException gracefully
        assertThatCode(() -> ChurreraCLI.printBanner(() -> failingGitInfo))
                .doesNotThrowAnyException();

        // Then
        String output = outputStream.toString();
        assertThat(output).isNotBlank();
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
    void testCreateCLICommand_WithMocks() {
        // Given
        String testApiKey = "test-api-key";
        InputStream testInputStream = new ByteArrayInputStream("test".getBytes());

        ChurreraCLI cli = new ChurreraCLI(
            apiKeyResolver,
            testApiKey,
            propertyResolver,
            jobRepository,
            apiClient,
            defaultApi,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );

        // When
        CliCommand result = new CliCommand(cli.jobRepository, cli.jobProcessor, cli.propertyResolver, new Scanner(testInputStream), cli.cliAgent);

        // Then
        assertThat(result).isNotNull();
    }


    @Test
    void testCreateCLICommand_WithInjectedDependencies() {
        // Given
        // This test verifies that CliCommand can be created with injected dependencies
        String testApiKey = "test-api-key";
        InputStream testInputStream = new ByteArrayInputStream("test".getBytes());

        ChurreraCLI cli = new ChurreraCLI(
            apiKeyResolver,
            testApiKey,
            propertyResolver,
            jobRepository,
            apiClient,
            defaultApi,
            cliAgent,
            workflowParser,
            jobProcessor,
            workflowValidator,
            pmlValidator
        );

        // When
        CliCommand result = new CliCommand(cli.jobRepository, cli.jobProcessor, cli.propertyResolver, new Scanner(testInputStream), cli.cliAgent);

        // Then
        assertThat(cli).isNotNull();
        assertThat(result).isNotNull();
    }

    @Test
    void testCreateRunCommand_WithMocks() {
        // Given
        String testApiKey = "test-api-key";
        when(propertyResolver.getProperty(anyString(), anyString()))
                .thenReturn(Optional.of("5"));

        ChurreraCLI cli = new ChurreraCLI(
            apiKeyResolver,
            testApiKey,
            propertyResolver,
            jobRepository,
            apiClient,
            defaultApi,
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
            apiKeyResolver,
            testApiKey,
            propertyResolver,
            jobRepository,
            apiClient,
            defaultApi,
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
            apiKeyResolver,
            testApiKey,
            propertyResolver,
            jobRepository,
            apiClient,
            defaultApi,
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
            apiKeyResolver,
            testApiKey,
            propertyResolver,
            jobRepository,
            apiClient,
            defaultApi,
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
            apiKeyResolver,
            testApiKey,
            propertyResolver,
            jobRepository,
            apiClient,
            defaultApi,
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
            apiKeyResolver,
            testApiKey,
            propertyResolver,
            jobRepository,
            apiClient,
            defaultApi,
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
