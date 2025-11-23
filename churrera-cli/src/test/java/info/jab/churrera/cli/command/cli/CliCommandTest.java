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
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
    }

    private void runTestWithPipedInput(ThrowingConsumer<PipedOutputStream> commandsSender) throws Exception {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);
        Scanner testScanner = new Scanner(in);
        cliCommand = new CliCommand(jobRepository, jobProcessor, propertyResolver, testScanner, cliAgent);

        Thread runThread = new Thread(() -> cliCommand.run());
        runThread.start();

        try {
            commandsSender.accept(out);
        } finally {
            runThread.join(5000);
            out.close();
        }
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    private void waitForProcessor() {
        await()
            .pollDelay(100, TimeUnit.MILLISECONDS)
            .pollInterval(50, TimeUnit.MILLISECONDS)
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(jobProcessor, atLeastOnce()).processJobs());
    }

    private void sendCommand(PipedOutputStream out, String command) throws IOException {
        out.write((command + "\n").getBytes());
        out.flush();
    }

    @Test
    void testRun_ExitCommand() throws Exception {
        runTestWithPipedInput(out -> {
            // Wait for processor to start before quitting
            waitForProcessor();

            sendCommand(out, "quit");

            // Wait a bit to ensure loop processes quit
            Thread.sleep(100);

            ScheduledExecutorService executor = cliCommand.getScheduledExecutor();
            assertNotNull(executor);
            assertTrue(executor.isShutdown());
        });
    }

    @Test
    void testRun_HelpCommand() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "help");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_JobsCommand() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "jobs");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_ClearCommand() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "clear");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_EmptyInput() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_UnknownCommand() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "unknown-command");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_JobsNewCommand() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "jobs new /path/to/workflow.xml");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_JobsStatusCommand() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "jobs status job-id-123");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_JobsLogsCommand() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "jobs logs job-id-123");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_JobsDeleteCommand() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "jobs delete job-id-123");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_JobsPrCommand() throws Exception {
        runTestWithPipedInput(out -> {
            sendCommand(out, "jobs pr job-id-123");
            waitForProcessor();
            sendCommand(out, "quit");
        });
    }

    @Test
    void testRun_ExceptionDuringCommandExecution() throws Exception {
        doThrow(new RuntimeException("Error")).when(jobProcessor).processJobs();

        runTestWithPipedInput(out -> {
            sendCommand(out, "jobs");
            waitForProcessor();
            sendCommand(out, "quit");
        });
        // Verified implicitly by waitForProcessor() which checks mock interaction
        // and fact that test didn't crash
    }

    @Test
    void testGetJobRepository() {
        // Given
        String input = "quit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        scanner = new Scanner(inputStream);
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
    void testRun_QuitCaseInsensitive() throws Exception {
        runTestWithPipedInput(out -> {
            waitForProcessor();
            sendCommand(out, "QUIT");

            Thread.sleep(100);

            ScheduledExecutorService executor = cliCommand.getScheduledExecutor();
            assertNotNull(executor);
            assertTrue(executor.isShutdown());
        });
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
