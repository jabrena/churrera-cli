package info.jab.churrera.cli.command;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.agent.AgentState;
import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Scanner;

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

    private NewJobCommand newJobCommand;
    private Scanner scanner;

    @BeforeEach
    void setUp() {
        // Create a scanner with predefined input
        String input = "test/path\nmodel-name\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        scanner = new Scanner(inputStream);
        newJobCommand = new NewJobCommand(jobRepository, scanner);
    }

    @Test
    void testRun_ValidInput() throws BaseXException, QueryException, IOException {
        // When
        newJobCommand.run();

        // Then
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_EmptyPath() throws BaseXException, QueryException, IOException {
        // Given
        String input = "\nmodel-name\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner emptyPathScanner = new Scanner(inputStream);
        NewJobCommand commandWithEmptyPath = new NewJobCommand(jobRepository, emptyPathScanner);

        // When
        commandWithEmptyPath.run();

        // Then
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testRun_EmptyModel() throws BaseXException, QueryException, IOException {
        // Given
        String input = "test/path\n\nrepo-url\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner emptyModelScanner = new Scanner(inputStream);
        NewJobCommand commandWithEmptyModel = new NewJobCommand(jobRepository, emptyModelScanner);

        // When
        commandWithEmptyModel.run();

        // Then
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_EmptyRepository() throws BaseXException, QueryException, IOException {
        // Given
        String input = "test/path\nmodel-name\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner emptyRepoScanner = new Scanner(inputStream);
        NewJobCommand commandWithEmptyRepo = new NewJobCommand(jobRepository, emptyRepoScanner);

        // When
        commandWithEmptyRepo.run();

        // Then
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_AllEmpty() throws BaseXException, QueryException, IOException {
        // Given
        String input = "\n\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner allEmptyScanner = new Scanner(inputStream);
        NewJobCommand commandWithAllEmpty = new NewJobCommand(jobRepository, allEmptyScanner);

        // When
        commandWithAllEmpty.run();

        // Then
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void testRun_RepositoryException() throws BaseXException, QueryException, IOException {
        // Given
        doThrow(new BaseXException("Database error")).when(jobRepository).save(any(Job.class));

        // When & Then
        assertDoesNotThrow(() -> newJobCommand.run());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_QueryException() throws BaseXException, QueryException, IOException {
        // Given
        doThrow(new QueryException("Query error")).when(jobRepository).save(any(Job.class));

        // When & Then
        assertDoesNotThrow(() -> newJobCommand.run());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_IOException() throws BaseXException, QueryException, IOException {
        // Given
        doThrow(new IOException("IO error")).when(jobRepository).save(any(Job.class));

        // When & Then
        assertDoesNotThrow(() -> newJobCommand.run());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void testRun_JobCreatedWithCorrectValues() throws BaseXException, QueryException, IOException {
        // When
        newJobCommand.run();

        // Then
        verify(jobRepository).save(argThat(job -> {
            assertEquals("test/path", job.path());
            assertEquals("model-name", job.model());
            assertEquals("repo-url", job.repository());
            assertEquals(AgentState.UNKNOWN, job.status());
            assertNotNull(job.jobId());
            assertNotNull(job.createdAt());
            assertNotNull(job.lastUpdate());
            return true;
        }));
    }

    @Test
    void testRun_JobCreatedWithDefaultValues() throws BaseXException, QueryException, IOException {
        // Given
        String input = "test/path\n\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner defaultValuesScanner = new Scanner(inputStream);
        NewJobCommand commandWithDefaults = new NewJobCommand(jobRepository, defaultValuesScanner);

        // When
        commandWithDefaults.run();

        // Then
        verify(jobRepository).save(argThat(job -> {
            assertEquals("test/path", job.path());
            assertEquals("default-model", job.model());
            assertEquals("default-repository", job.repository());
            assertEquals(AgentState.UNKNOWN, job.status());
            return true;
        }));
    }
}
