package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NewJobCommand.
 */
@ExtendWith(MockitoExtension.class)
class NewJobCommandTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CLIAgent cliAgent;

    private NewJobCommand newJobCommand;
    private Scanner scanner;

    @BeforeEach
    void setUp() {
        // Mock CLIAgent to return a list of valid models (using lenient since not all tests use it)
        List<String> availableModels = Arrays.asList("default", "gpt-4", "claude-3", "model-name", "default-model");
        lenient().when(cliAgent.getModels()).thenReturn(availableModels);

        // Create a scanner with predefined input
        String input = "test/path\nmodel-name\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        scanner = new Scanner(inputStream);
        newJobCommand = new NewJobCommand(jobRepository, scanner, cliAgent);
    }

    @Test
    void testRun_ValidInput() throws IOException {
        // When
        newJobCommand.run();

        // Then
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_EmptyPath() throws IOException {
        // Given
        String input = "\nmodel-name\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner emptyPathScanner = new Scanner(inputStream);
        NewJobCommand commandWithEmptyPath = new NewJobCommand(jobRepository, emptyPathScanner, cliAgent);

        // When
        commandWithEmptyPath.run();

        // Then
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testRun_EmptyModel() throws IOException {
        // Given
        String input = "test/path\n\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner emptyModelScanner = new Scanner(inputStream);
        NewJobCommand commandWithEmptyModel = new NewJobCommand(jobRepository, emptyModelScanner, cliAgent);

        // When
        commandWithEmptyModel.run();

        // Then
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_EmptyRepository() throws IOException {
        // Given
        String input = "test/path\nmodel-name\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner emptyRepoScanner = new Scanner(inputStream);
        NewJobCommand commandWithEmptyRepo = new NewJobCommand(jobRepository, emptyRepoScanner, cliAgent);

        // When
        commandWithEmptyRepo.run();

        // Then
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_AllEmpty() throws IOException {
        // Given
        String input = "\n\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner allEmptyScanner = new Scanner(inputStream);
        NewJobCommand commandWithAllEmpty = new NewJobCommand(jobRepository, allEmptyScanner, cliAgent);

        // When
        commandWithAllEmpty.run();

        // Then
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testRun_RepositoryException() throws IOException {
        // Given
        doThrow(new RuntimeException("Database error")).when(jobRepository).save(any(Job.class));

        // When & Then
        assertThatThrownBy(() -> newJobCommand.run())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database error");
    }

    @Test
    void testRun_QueryException() throws IOException {
        // Given
        doThrow(new RuntimeException("Query error")).when(jobRepository).save(any(Job.class));

        // When & Then
        assertThatThrownBy(() -> newJobCommand.run())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Query error");
    }

    @Test
    void testRun_IOException() throws IOException {
        // Given
        doThrow(new IOException("IO error")).when(jobRepository).save(any(Job.class));

        // When & Then
        assertDoesNotThrow(() -> newJobCommand.run());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_JobCreatedWithCorrectValues() throws IOException {
        // When
        newJobCommand.run();

        // Then
        verify(jobRepository).save(argThat(job -> {
            assertEquals("test/path", job.path());
            assertEquals("model-name", job.model());
            assertEquals("repo-url", job.repository());
            assertEquals(AgentState.CREATING(), job.status());
            assertNotNull(job.jobId());
            assertNotNull(job.createdAt());
            assertNotNull(job.lastUpdate());
            return true;
        }));
    }

    @Test
    void testRun_JobCreatedWithDefaultValues() throws IOException {
        // Given
        String input = "test/path\n\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner defaultValuesScanner = new Scanner(inputStream);
        NewJobCommand commandWithDefaults = new NewJobCommand(jobRepository, defaultValuesScanner, cliAgent);

        // When
        commandWithDefaults.run();

        // Then
        verify(jobRepository).save(argThat(job -> {
            assertEquals("test/path", job.path());
            assertEquals("default-model", job.model());
            assertEquals("default-repository", job.repository());
            assertEquals(AgentState.CREATING(), job.status());
            return true;
        }));
    }

    @Test
    void testRun_ModelValidationFails() throws IOException {
        // Given
        String input = "test/path\ninvalid-model\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scannerWithInvalidModel = new Scanner(inputStream);
        NewJobCommand commandWithInvalidModel = new NewJobCommand(jobRepository, scannerWithInvalidModel, cliAgent);

        when(cliAgent.getModels()).thenReturn(List.of("valid-model-1", "valid-model-2", "default"));

        // When
        commandWithInvalidModel.run();

        // Then
        verify(jobRepository, never()).save(any(Job.class));
        verify(cliAgent).getModels();
    }

    @Test
    void testRun_ModelValidation_EmptyModelList() throws IOException {
        // Given
        String input = "test/path\nsome-model\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scannerWithModel = new Scanner(inputStream);
        NewJobCommand commandWithModel = new NewJobCommand(jobRepository, scannerWithModel, cliAgent);

        when(cliAgent.getModels()).thenReturn(List.of("default"));

        // When
        commandWithModel.run();

        // Then - "default" is always added, so validation should pass if model is "default"
        // But if model is "some-model" and only "default" is returned, validation should fail
        verify(jobRepository, never()).save(any(Job.class));
        verify(cliAgent).getModels();
    }

    @Test
    void testRun_ModelValidation_NullModelList() throws IOException {
        // Given
        String input = "test/path\nsome-model\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scannerWithModel = new Scanner(inputStream);
        NewJobCommand commandWithModel = new NewJobCommand(jobRepository, scannerWithModel, cliAgent);

        when(cliAgent.getModels()).thenReturn(null);

        // When
        commandWithModel.run();

        // Then - null list becomes ["default"], but "some-model" is not in it, so validation fails
        verify(jobRepository, never()).save(any(Job.class));
        verify(cliAgent).getModels();
    }

    @Test
    void testRun_ModelValidation_Exception() throws IOException {
        // Given
        String input = "test/path\nsome-model\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scannerWithModel = new Scanner(inputStream);
        NewJobCommand commandWithModel = new NewJobCommand(jobRepository, scannerWithModel, cliAgent);

        when(cliAgent.getModels()).thenThrow(new RuntimeException("API error"));

        // When
        commandWithModel.run();

        // Then
        verify(jobRepository, never()).save(any(Job.class));
        verify(cliAgent).getModels();
    }

    @Test
    void testRun_ModelValidation_WhitespaceTrimmed() throws IOException {
        // Given
        String input = "test/path\n  model-name  \nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scannerWithWhitespace = new Scanner(inputStream);
        NewJobCommand commandWithWhitespace = new NewJobCommand(jobRepository, scannerWithWhitespace, cliAgent);

        when(cliAgent.getModels()).thenReturn(List.of("model-name", "other-model", "default"));

        // When
        commandWithWhitespace.run();

        // Then - should trim whitespace and validate successfully
        verify(jobRepository).save(argThat(job -> {
            assertEquals("model-name", job.model());
            return true;
        }));
    }
}
