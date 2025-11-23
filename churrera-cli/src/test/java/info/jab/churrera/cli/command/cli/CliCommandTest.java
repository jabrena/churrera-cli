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
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
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
    void testRun_ExitCommand() throws InterruptedException {
        // Given
        String input = "quit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);

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

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
    }

    @Test
    void testRun_JobsCommand() throws InterruptedException {
        // Given
        String input = "jobs\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
    }

    @Test
    void testRun_ClearCommand() throws InterruptedException {
        // Given
        String input = "clear\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
    }

    @Test
    void testRun_EmptyInput() throws InterruptedException {
        // Given
        String input = "\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
    }

    @Test
    void testRun_UnknownCommand() throws InterruptedException {
        // Given
        String input = "unknown-command\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
    }

    @Test
    void testRun_JobsNewCommand() throws InterruptedException {
        // Given
        String input = "jobs new /path/to/workflow.xml\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
    }

    @Test
    void testRun_JobsStatusCommand() throws InterruptedException {
        // Given
        String input = "jobs status job-id-123\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
    }

    @Test
    void testRun_JobsLogsCommand() throws InterruptedException {
        // Given
        String input = "jobs logs job-id-123\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
    }

    @Test
    void testRun_JobsDeleteCommand() throws InterruptedException {
        // Given
        String input = "jobs delete job-id-123\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
    }

    @Test
    void testRun_JobsPrCommand() throws InterruptedException {
        // Given
        String input = "jobs pr job-id-123\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);
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
    void testRun_QuitCaseInsensitive() throws InterruptedException {
        // Given
        String input = "QUIT\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        // When - run in a separate thread to allow async executor to run
        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        // Then - verify scheduled executor ran at least once using Awaitility
        // Add pollDelay to give the executor time to start and execute the first task
        // Use longer delay in CI environments where scheduling can be slower
        await()
            .pollDelay(300, TimeUnit.MILLISECONDS)  // Give executor time to start and execute
            .pollInterval(50, TimeUnit.MILLISECONDS)  // Check frequently
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());

        // Wait for run to complete after verification
        runThread.join(5000);

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

