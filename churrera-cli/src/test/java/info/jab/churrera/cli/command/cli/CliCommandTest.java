package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.cli.service.JobProcessor;
import info.jab.churrera.util.PropertyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CliCommand.
 */
@ExtendWith(MockitoExtension.class)
class CliCommandTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobProcessor jobProcessor;

    @Mock
    private PropertyResolver propertyResolver;

    @Mock
    private CLIAgent cliAgent;

    private CliCommand cliCommand;
    private Scanner scanner;

    @BeforeEach
    void setUp() {
        // Setup default property values
        lenient().when(propertyResolver.getProperty("application.properties", "cli.polling.interval.seconds"))
            .thenReturn(Optional.of("5"));
        lenient().when(propertyResolver.getProperty("application.properties", "cli.prompt"))
            .thenReturn(Optional.of("churrera> "));

        // Create scanner with input stream
        String input = "quit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        scanner = new Scanner(inputStream);
    }

    @Test
    void testRun_ExitCommand() {
        // Given
        String input = "quit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then
        verify(jobProcessor, atLeastOnce()).processJobs();
        ScheduledExecutorService executor = cliCommand.getScheduledExecutor();
        assertNotNull(executor);
        assertTrue(executor.isShutdown());
    }

    @Test
    void testRun_HelpCommand() throws InterruptedException {
        // Given
        String input = "help\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then - verify scheduled executor ran at least once (with timeout to account for async execution)
        verify(jobProcessor, timeout(1000).atLeastOnce()).processJobs();
    }

    @Test
    void testRun_JobsCommand() throws InterruptedException {
        // Given
        String input = "jobs\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then - verify scheduled executor ran at least once (with timeout to account for async execution)
        verify(jobProcessor, timeout(1000).atLeastOnce()).processJobs();
    }

    @Test
    void testRun_ClearCommand() {
        // Given
        String input = "clear\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then
        verify(jobProcessor, atLeastOnce()).processJobs();
    }

    @Test
    void testRun_EmptyInput() {
        // Given
        String input = "\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then
        verify(jobProcessor, atLeastOnce()).processJobs();
    }

    @Test
    void testRun_UnknownCommand() throws InterruptedException {
        // Given
        String input = "unknown-command\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then - verify scheduled executor ran at least once (with timeout to account for async execution)
        verify(jobProcessor, timeout(1000).atLeastOnce()).processJobs();
    }

    @Test
    void testRun_JobsNewCommand() {
        // Given
        String input = "jobs new /path/to/workflow.xml\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then
        verify(jobProcessor, atLeastOnce()).processJobs();
    }

    @Test
    void testRun_JobsStatusCommand() {
        // Given
        String input = "jobs status job-id-123\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then
        verify(jobProcessor, atLeastOnce()).processJobs();
    }

    @Test
    void testRun_JobsLogsCommand() {
        // Given
        String input = "jobs logs job-id-123\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then
        verify(jobProcessor, atLeastOnce()).processJobs();
    }

    @Test
    void testRun_JobsDeleteCommand() {
        // Given
        String input = "jobs delete job-id-123\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then
        verify(jobProcessor, atLeastOnce()).processJobs();
    }

    @Test
    void testRun_JobsPrCommand() {
        // Given
        String input = "jobs pr job-id-123\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then
        verify(jobProcessor, atLeastOnce()).processJobs();
    }

    @Test
    void testRun_ExceptionDuringCommandExecution() {
        // Given
        String input = "jobs\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);
        doThrow(new RuntimeException("Error")).when(jobProcessor).processJobs();

        // When & Then - should handle exception gracefully
        assertDoesNotThrow(() -> cliCommand.run());
    }

    @Test
    void testGetJobRepository() {
        // Given
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, scanner, cliAgent);

        // When
        JobRepository result = cliCommand.getJobRepository();

        // Then
        assertSame(jobRepository, result);
    }

    @Test
    void testGetScheduledExecutor() {
        // Given
        String input = "quit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();
        ScheduledExecutorService executor = cliCommand.getScheduledExecutor();

        // Then
        assertNotNull(executor);
    }

    @Test
    void testRun_QuitCaseInsensitive() {
        // Given
        String input = "QUIT\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Then
        verify(jobProcessor, atLeastOnce()).processJobs();
        ScheduledExecutorService executor = cliCommand.getScheduledExecutor();
        assertNotNull(executor);
        assertTrue(executor.isShutdown());
    }

    @Test
    void testRun_InterruptedDuringShutdown() {
        // Given
        String input = "quit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When
        cliCommand.run();

        // Interrupt the current thread to test interruption handling
        Thread.currentThread().interrupt();

        // Then - should handle interruption gracefully
        ScheduledExecutorService executor = cliCommand.getScheduledExecutor();
        assertNotNull(executor);

        // Clear interrupt flag
        Thread.interrupted();
    }
}

