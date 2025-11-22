package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.cli.model.AgentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeleteJobCommand.
 */
@ExtendWith(MockitoExtension.class)
class DeleteJobCommandTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CLIAgent cliAgent;

    private DeleteJobCommand deleteJobCommand;
    private Job testJob;

    @BeforeEach
    void setUp() {
        testJob = new Job("test-job-id",
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
    }

    @Test
    void testRun_JobNotFound() {
        // Given
        String jobId = "non-existent-job";
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // When
        deleteJobCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
        verifyNoInteractions(cliAgent);
        verify(jobRepository, never()).findJobsByParentId(anyString());
        verify(jobRepository, never()).deletePromptsByJobId(anyString());
        verify(jobRepository, never()).deleteById(anyString());
    }

    @Test
    void testRun_JobFoundWithoutCursorAgent() {
        // Given
        String jobId = "test-job-id";
        Job jobWithoutAgent = new Job(jobId,
            "/test/path",
            null, // no cursor agent
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(jobWithoutAgent));
        when(jobRepository.findJobsByParentId(jobId)).thenReturn(Collections.emptyList());

        // When
        deleteJobCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
        verify(jobRepository).findJobsByParentId(jobId);
        verifyNoInteractions(cliAgent);
        verify(jobRepository).deletePromptsByJobId(jobId);
        verify(jobRepository).deleteById(jobId);
    }

    @Test
    void testRun_JobFoundWithCursorAgent_Success() {
        // Given
        String jobId = "test-job-id";
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobsByParentId(jobId)).thenReturn(Collections.emptyList());

        // When
        deleteJobCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
        verify(jobRepository).findJobsByParentId(jobId);
        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(jobRepository).deletePromptsByJobId(jobId);
        verify(jobRepository).deleteById(jobId);
    }

    @Test
    void testRun_JobFoundWithCursorAgent_DeleteAgentFails() {
        // Given
        String jobId = "test-job-id";
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobsByParentId(jobId)).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("API Error")).when(cliAgent).deleteAgent("cursor-agent-123");

        // When
        deleteJobCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(jobId);
        verify(jobRepository).findJobsByParentId(jobId);
        verify(cliAgent).deleteAgent("cursor-agent-123");
        // Should continue with database deletion even if Cursor API fails
        verify(jobRepository).deletePromptsByJobId(jobId);
        verify(jobRepository).deleteById(jobId);
    }

    @Test
    void testRun_DatabaseException() {
        // Given
        String jobId = "test-job-id";
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertDoesNotThrow(() -> deleteJobCommand.run());
        verify(jobRepository).findById(jobId);
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testRun_QueryException() {
        // Given
        String jobId = "test-job-id";
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenThrow(new RuntimeException("Query error"));

        // When & Then
        assertDoesNotThrow(() -> deleteJobCommand.run());
        verify(jobRepository).findById(jobId);
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testRun_JobWithChildJobs_CascadeDelete() {
        // Given
        String parentJobId = "parent-job-id";
        Job parentJob = new Job(parentJobId,
            "/parent/path",
            "cursor-agent-parent",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        String childJobId1 = "child-job-id-1";
        Job childJob1 = new Job(childJobId1,
            "/child1/path",
            "cursor-agent-child1",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), parentJobId, null, null, null, null, null, null);

        String childJobId2 = "child-job-id-2";
        Job childJob2 = new Job(childJobId2,
            "/child2/path",
            null,
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), parentJobId, null, null, null, null, null, null);

        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, parentJobId);

        when(jobRepository.findById(parentJobId)).thenReturn(Optional.of(parentJob));
        when(jobRepository.findJobsByParentId(parentJobId)).thenReturn(Arrays.asList(childJob1, childJob2));
        when(jobRepository.findJobsByParentId(childJobId1)).thenReturn(Collections.emptyList());
        when(jobRepository.findJobsByParentId(childJobId2)).thenReturn(Collections.emptyList());

        // When
        deleteJobCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(parentJobId);
        verify(jobRepository).findJobsByParentId(parentJobId);
        verify(jobRepository).findJobsByParentId(childJobId1);
        verify(jobRepository).findJobsByParentId(childJobId2);

        // Verify child jobs were deleted
        verify(cliAgent).deleteAgent("cursor-agent-child1");
        verify(jobRepository).deletePromptsByJobId(childJobId1);
        verify(jobRepository).deleteById(childJobId1);
        verify(jobRepository).deletePromptsByJobId(childJobId2);
        verify(jobRepository).deleteById(childJobId2);

        // Verify parent job was deleted
        verify(cliAgent).deleteAgent("cursor-agent-parent");
        verify(jobRepository).deletePromptsByJobId(parentJobId);
        verify(jobRepository).deleteById(parentJobId);
    }

    @Test
    void testRun_JobWithNestedChildJobs_RecursiveCascadeDelete() {
        // Given
        String grandparentJobId = "grandparent-job-id";
        Job grandparentJob = new Job(grandparentJobId,
            "/grandparent/path",
            "cursor-agent-grandparent",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        String parentJobId = "parent-job-id";
        Job parentJob = new Job(parentJobId,
            "/parent/path",
            "cursor-agent-parent",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), grandparentJobId, null, null, null, null, null, null);

        String childJobId = "child-job-id";
        Job childJob = new Job(childJobId,
            "/child/path",
            "cursor-agent-child",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), parentJobId, null, null, null, null, null, null);

        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, grandparentJobId);

        when(jobRepository.findById(grandparentJobId)).thenReturn(Optional.of(grandparentJob));
        when(jobRepository.findJobsByParentId(grandparentJobId)).thenReturn(Arrays.asList(parentJob));
        when(jobRepository.findJobsByParentId(parentJobId)).thenReturn(Arrays.asList(childJob));
        when(jobRepository.findJobsByParentId(childJobId)).thenReturn(Collections.emptyList());

        // When
        deleteJobCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(grandparentJobId);
        verify(jobRepository).findJobsByParentId(grandparentJobId);
        verify(jobRepository).findJobsByParentId(parentJobId);
        verify(jobRepository).findJobsByParentId(childJobId);

        // Verify deletion order: child -> parent -> grandparent
        verify(cliAgent).deleteAgent("cursor-agent-child");
        verify(jobRepository).deletePromptsByJobId(childJobId);
        verify(jobRepository).deleteById(childJobId);

        verify(cliAgent).deleteAgent("cursor-agent-parent");
        verify(jobRepository).deletePromptsByJobId(parentJobId);
        verify(jobRepository).deleteById(parentJobId);

        verify(cliAgent).deleteAgent("cursor-agent-grandparent");
        verify(jobRepository).deletePromptsByJobId(grandparentJobId);
        verify(jobRepository).deleteById(grandparentJobId);
    }


    // Tests for resolveJobId method scenarios

    @Test
    void testRun_ExactMatchSuccess() {
        // Given
        String fullJobId = "12345678-1234-1234-1234-123456789abc";
        Job jobWithMatchingId = new Job(fullJobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, fullJobId);

        when(jobRepository.findById(fullJobId)).thenReturn(Optional.of(jobWithMatchingId));
        when(jobRepository.findJobsByParentId(fullJobId)).thenReturn(Collections.emptyList());

        // When
        deleteJobCommand.run();

        // Then
        verify(jobRepository, atLeastOnce()).findById(fullJobId);
        verify(jobRepository).findJobsByParentId(fullJobId);
        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(jobRepository).deletePromptsByJobId(fullJobId);
        verify(jobRepository).deleteById(fullJobId);
        verify(jobRepository, never()).findAll();
    }

    @Test
    void testRun_EightCharPrefix_UniqueMatch() {
        // Given
        String prefix = "12345678";
        String fullJobId = "12345678-1234-1234-1234-123456789abc";
        Job matchingJob = new Job(fullJobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(Arrays.asList(matchingJob));
        // After resolving prefix, it calls findById again with the resolved full job ID
        when(jobRepository.findById(fullJobId)).thenReturn(Optional.of(matchingJob));
        when(jobRepository.findJobsByParentId(fullJobId)).thenReturn(Collections.emptyList());

        // Capture stdout to verify message is not printed
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // When
            deleteJobCommand.run();

            // Then
            verify(jobRepository).findById(prefix);
            verify(jobRepository).findAll();
            verify(jobRepository, atLeastOnce()).findById(fullJobId);
            verify(jobRepository).findJobsByParentId(fullJobId);
            verify(cliAgent).deleteAgent("cursor-agent-123");
            verify(jobRepository).deletePromptsByJobId(fullJobId);
            verify(jobRepository).deleteById(fullJobId);
            assertFalse(outContent.toString().contains("No job found starting with"));
            assertFalse(outContent.toString().contains("Ambiguous job prefix"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRun_EightCharPrefix_MultipleMatches_Ambiguous() {
        // Given
        String prefix = "12345678";
        String fullJobId1 = "12345678-1234-1234-1234-123456789abc";
        String fullJobId2 = "12345678-5678-5678-5678-567890abcdef";
        Job matchingJob1 = new Job(fullJobId1,
            "/test/path1",
            "cursor-agent-1",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        Job matchingJob2 = new Job(fullJobId2,
            "/test/path2",
            "cursor-agent-2",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(Arrays.asList(matchingJob1, matchingJob2));

        // Capture stdout to verify ambiguous message
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // When
            deleteJobCommand.run();

            // Then
            verify(jobRepository).findById(prefix);
            verify(jobRepository).findAll();
            verifyNoInteractions(cliAgent);
            verify(jobRepository, never()).deletePromptsByJobId(anyString());
            verify(jobRepository, never()).deleteById(anyString());

            String output = outContent.toString();
            assertTrue(output.contains("Ambiguous job prefix"),
                "Should contain ambiguous prefix message");
            assertTrue(output.contains(fullJobId1),
                "Should list first matching job");
            assertTrue(output.contains(fullJobId2),
                "Should list second matching job");
            assertTrue(output.contains("Please specify a full UUID or a unique 8-char prefix"),
                "Should contain instruction message");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRun_EightCharPrefix_NoMatches() {
        // Given
        String prefix = "12345678";
        String nonMatchingJobId = "87654321-1234-1234-1234-123456789abc";
        Job nonMatchingJob = new Job(nonMatchingJobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(Arrays.asList(nonMatchingJob));

        // Capture stdout to verify no matches message
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // When
            deleteJobCommand.run();

            // Then
            verify(jobRepository).findById(prefix);
            verify(jobRepository).findAll();
            verifyNoInteractions(cliAgent);
            verify(jobRepository, never()).deletePromptsByJobId(anyString());
            verify(jobRepository, never()).deleteById(anyString());

            String output = outContent.toString();
            assertTrue(output.contains("No job found starting with: " + prefix),
                "Should contain no matches message");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRun_EightCharPrefix_EmptyJobList() {
        // Given
        String prefix = "12345678";
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(Collections.emptyList());

        // Capture stdout
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // When
            deleteJobCommand.run();

            // Then
            verify(jobRepository).findById(prefix);
            verify(jobRepository).findAll();
            verifyNoInteractions(cliAgent);
            verify(jobRepository, never()).deletePromptsByJobId(anyString());
            verify(jobRepository, never()).deleteById(anyString());

            String output = outContent.toString();
            assertTrue(output.contains("No job found starting with: " + prefix),
                "Should contain no matches message");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRun_NonEightChar_NonExactMatch() {
        // Given
        String partialId = "1234567"; // 7 characters
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, partialId);

        when(jobRepository.findById(partialId)).thenReturn(Optional.empty());

        // Capture stdout
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // When
            deleteJobCommand.run();

            // Then
            verify(jobRepository).findById(partialId);
            verify(jobRepository, never()).findAll();
            verifyNoInteractions(cliAgent);
            verify(jobRepository, never()).deletePromptsByJobId(anyString());
            verify(jobRepository, never()).deleteById(anyString());

            String output = outContent.toString();
            assertTrue(output.contains("Job not found: " + partialId),
                "Should contain job not found message");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRun_TooLongPrefix_NonExactMatch() {
        // Given
        String longPrefix = "123456789"; // 9 characters
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, longPrefix);

        when(jobRepository.findById(longPrefix)).thenReturn(Optional.empty());

        // Capture stdout
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // When
            deleteJobCommand.run();

            // Then
            verify(jobRepository).findById(longPrefix);
            verify(jobRepository, never()).findAll();
            verifyNoInteractions(cliAgent);

            String output = outContent.toString();
            assertTrue(output.contains("Job not found: " + longPrefix),
                "Should contain job not found message");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRun_EightCharPrefix_FiltersNullJobIds() {
        // Given
        String prefix = "12345678";
        String fullJobId = "12345678-1234-1234-1234-123456789abc";
        Job matchingJob = new Job(fullJobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        // Job with different prefix should not match
        String differentPrefixId = "87654321-1234-1234-1234-123456789abc";
        Job jobWithDifferentPrefix = new Job(differentPrefixId,
            "/test/path2",
            "cursor-agent-456",
            "test-model",
            "test-repo",
            AgentState.FINISHED(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(Arrays.asList(matchingJob, jobWithDifferentPrefix));
        // After resolving prefix, it calls findById again with the resolved full job ID
        when(jobRepository.findById(fullJobId)).thenReturn(Optional.of(matchingJob));
        when(jobRepository.findJobsByParentId(fullJobId)).thenReturn(Collections.emptyList());

        // When
        deleteJobCommand.run();

        // Then - should only match and delete the job with matching prefix
        verify(jobRepository).findById(prefix);
        verify(jobRepository).findAll();
        verify(jobRepository, atLeastOnce()).findById(fullJobId);
        verify(jobRepository).findJobsByParentId(fullJobId);
        verify(cliAgent).deleteAgent("cursor-agent-123");
        verify(jobRepository).deletePromptsByJobId(fullJobId);
        verify(jobRepository).deleteById(fullJobId);
    }

    @Test
    void testRun_EightCharPrefix_FindAllThrowsBaseXException() {
        // Given
        String prefix = "12345678";
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertDoesNotThrow(() -> deleteJobCommand.run());
        verify(jobRepository).findById(prefix);
        verify(jobRepository).findAll();
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testRun_EightCharPrefix_FindAllThrowsQueryException() {
        // Given
        String prefix = "12345678";
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenThrow(new RuntimeException("Query error"));

        // When & Then
        assertDoesNotThrow(() -> deleteJobCommand.run());
        verify(jobRepository).findById(prefix);
        verify(jobRepository).findAll();
        verifyNoInteractions(cliAgent);
    }

    @Test
    void testRun_NullJobIdProvided() {
        // Given
        String nullJobId = null;
        deleteJobCommand = new DeleteJobCommand(jobRepository, cliAgent, nullJobId);

        // Capture stdout
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // When
            deleteJobCommand.run();

            // Then
            verify(jobRepository, never()).findById(anyString());
            verify(jobRepository, never()).findAll();
            verifyNoInteractions(cliAgent);

            String output = outContent.toString();
            assertTrue(output.contains("Job not found: null"),
                "Should handle null gracefully");
        } finally {
            System.setOut(originalOut);
        }
    }
}
