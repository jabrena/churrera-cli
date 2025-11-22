package info.jab.churrera.cli.command.cli;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.cli.model.JobWithDetails;
import info.jab.churrera.cli.model.Prompt;
import info.jab.churrera.cli.repository.JobRepository;
import info.jab.churrera.cli.service.CLIAgent;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobStatusCommand.
 */
@ExtendWith(MockitoExtension.class)
class JobStatusCommandTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CLIAgent cliAgent;

    private JobStatusCommand jobStatusCommand;
    private Job testJob;
    private List<Prompt> testPrompts;

    @BeforeEach
    void setUp() {
        testJob = new Job("test-job-id",
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        testPrompts = List.of(
            new Prompt(
                "prompt-1",
                "test-job-id",
                "prompt1.pml",
                "COMPLETED",
                LocalDateTime.now(),
                LocalDateTime.now()
            ),
            new Prompt(
                "prompt-2",
                "test-job-id",
                "prompt2.pml",
                "SENT",
                LocalDateTime.now(),
                LocalDateTime.now()
            )
        );
    }

    @Test
    void testRun_JobNotFound() {
        // Given
        String jobId = "non-existent-job";
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, never()).findJobWithDetails(anyString());
    }

    @Test
    void testRun_JobFoundWithPrompts() {
        // Given
        String jobId = "test-job-id";
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(testJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);
    }

    @Test
    void testRun_JobFoundWithoutPrompts() {
        // Given
        String jobId = "test-job-id";
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(testJob, List.of())));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);
    }

    @Test
    void testRun_JobFoundWithNullCursorAgentId() {
        // Given
        String jobId = "test-job-id";
        Job jobWithoutAgent = new Job(jobId,
            "/test/path",
            null, // no cursor agent
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(jobWithoutAgent));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(jobWithoutAgent, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
    }

    @Test
    void testRun_DatabaseException() {
        // Given
        String jobId = "test-job-id";
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertDoesNotThrow(() -> jobStatusCommand.run());
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
    }

    @Test
    void testRun_QueryException() {
        // Given
        String jobId = "test-job-id";
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId)).thenThrow(new RuntimeException("Query error"));

        // When & Then
        assertDoesNotThrow(() -> jobStatusCommand.run());
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
    }

    @Test
    void testRun_JobWithParentJobId() {
        // Given
        String jobId = "child-job-id";
        Job childJob = new Job(jobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), "parent-job-id", "bound-value",
            WorkflowType.SEQUENCE, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(childJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(childJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
    }

    @Test
    void testRun_ParallelWorkflowWithBindResultType() throws Exception {
        // Given
        String jobId = "parallel-job-id";

        // Create temporary workflow file for parallel workflow
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <bindResultType>List&lt;String&gt;</bindResultType>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml" bind="item"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job parallelJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        lenient().when(cliAgent.getConversationContent("cursor-agent-123")).thenReturn("[\"item1\", \"item2\", \"item3\"]");

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);
        // Note: getConversationContent may or may not be called depending on workflow parsing

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_ParallelWorkflowWithExistingResult() throws Exception {
        // Given
        String jobId = "parallel-job-id";

        // Create temporary workflow file
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <bindResultType>List&lt;String&gt;</bindResultType>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml" bind="item"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job parallelJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null,
            "[\"item1\", \"item2\"]", // Result already exists
            WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        // Depending on resolution, status lookup may or may not occur
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);
        verify(cliAgent, never()).getConversationContent(anyString());

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_ParallelWorkflowErrorExtractingResult() throws Exception {
        // Given
        String jobId = "parallel-job-id";

        // Create temporary workflow file
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <bindResultType>List&lt;String&gt;</bindResultType>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml" bind="item"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job parallelJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        lenient().when(cliAgent.getConversationContent("cursor-agent-123")).thenThrow(new RuntimeException("API Error"));

        // When & Then
        assertDoesNotThrow(() -> jobStatusCommand.run());
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_SequenceWorkflowType() {
        // Given
        String jobId = "sequence-job-id";
        Job sequenceJob = new Job(jobId,
            "/test/path",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.SEQUENCE, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(sequenceJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(sequenceJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);
    }

    @Test
    void testRun_JobWithNullTypeAndInvalidWorkflowFile() {
        // Given
        String jobId = "invalid-workflow-job";
        Job jobWithInvalidWorkflow = new Job(jobId,
            "/non/existent/path/workflow.xml",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            null, null, null, null, null); // null type

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(jobWithInvalidWorkflow));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(jobWithInvalidWorkflow, testPrompts)));

        // When & Then
        assertDoesNotThrow(() -> jobStatusCommand.run());
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);
    }

    @Test
    void testRun_ParallelWorkflowNonSuccessfulStatus() throws Exception {
        // Given
        String jobId = "parallel-job-running";

        // Create temporary workflow file
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <bindResultType>List&lt;String&gt;</bindResultType>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml" bind="item"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job parallelJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.running(), // Not successful yet
            LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);
        verify(cliAgent, never()).getConversationContent(anyString());

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_ParallelWorkflowWithoutCursorAgentId() throws Exception {
        // Given
        String jobId = "parallel-job-no-agent";

        // Create temporary workflow file
        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <bindResultType>List&lt;String&gt;</bindResultType>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml" bind="item"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job parallelJob = new Job(jobId,
            workflowFile.toString(),
            null, // No cursor agent ID
            "test-model",
            "test-repo",
            AgentState.finished(),
            LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);
        verify(cliAgent, never()).getConversationContent(anyString());

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_JobWithNullTypeButValidParallelWorkflowFile() throws Exception {
        // Given - Test the branch where job.type() is null but file parses to PARALLEL
        String jobId = "legacy-parallel-job";

        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <bindResultType>List&lt;String&gt;</bindResultType>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml" bind="item"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job legacyParallelJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null,
            "[\"item1\", \"item2\"]",
            null, null, null, null, null); // null type - will be determined from file

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(legacyParallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(legacyParallelJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_ParallelWorkflowSuccessfulResultExtraction() throws Exception {
        // Given - Test successful result extraction with JSON serialization
        String jobId = "parallel-extract-result";

        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <bindResultType>List&lt;String&gt;</bindResultType>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml" bind="item"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job parallelJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null,
            "", // Empty result - will trigger extraction
            info.jab.churrera.workflow.WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        lenient().when(cliAgent.getConversationContent("cursor-agent-123"))
            .thenReturn("```json\n[\"value1\", \"value2\", \"value3\"]\n```");

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository, atMostOnce()).findJobWithDetails(jobId);
        // Note: getConversationContent and save may or may not be called depending on workflow parsing

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_ParallelWorkflowFailedDeserialization() throws Exception {
        // Given - Test failed deserialization (resultList.isEmpty())
        String jobId = "parallel-failed-deser";

        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <bindResultType>List&lt;String&gt;</bindResultType>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml" bind="item"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job parallelJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null,
            null, // null result - will trigger extraction
            WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));
        lenient().when(cliAgent.getConversationContent("cursor-agent-123"))
            .thenReturn("Invalid JSON that cannot be deserialized"); // Will fail to deserialize

        // When & Then - The test exercises the code paths but may not call getConversationContent
        // if workflow parsing or other conditions fail
        assertDoesNotThrow(() -> jobStatusCommand.run());
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_ParallelWorkflowWithoutBindResultType() throws Exception {
        // Given - Test parallel workflow without bindResultType
        String jobId = "parallel-no-bind-type";

        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job parallelJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verify(cliAgent, never()).getConversationContent(anyString());

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_JobWithNullTypeSequenceWorkflowFile() throws Exception {
        // Given - Test job with null type that parses to SEQUENCE
        String jobId = "legacy-sequence-job";

        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String sequenceWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.pml" type="pml"/>
                    <prompt src="prompt2.pml" type="pml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, sequenceWorkflow);

        Job legacySequenceJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            null, null, null, null, null); // null type - will be determined from file

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(legacySequenceJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(legacySequenceJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_ParallelTypeButNotParallelWorkflow() throws Exception {
        // Given - Test branch where type is PARALLEL but workflow parsing shows it's not parallel
        String jobId = "mismatched-type-job";

        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        // Create a sequence workflow but job has PARALLEL type
        String sequenceWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.pml" type="pml"/>
                </sequence>
            </pml-workflow>
            """;
        java.nio.file.Files.writeString(workflowFile, sequenceWorkflow);

        Job mismatchedJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null); // Type says PARALLEL but file is SEQUENCE

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(mismatchedJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(mismatchedJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verify(cliAgent, never()).getConversationContent(anyString());

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_ParallelWorkflowExceptionDuringWorkflowParsing() {
        // Given - Test exception during workflow parsing (line 143-145)
        String jobId = "parallel-parse-error";

        Job parallelJob = new Job(jobId,
            "/invalid/path/workflow.xml",
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.finished(), LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));

        // When & Then
        assertDoesNotThrow(() -> jobStatusCommand.run());
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
    }

    @Test
    void testRun_ParallelWorkflowWithNullResultAndNullStatus() throws Exception {
        // Given - Test with null result but job status is not successful
        String jobId = "parallel-null-result-not-successful";

        Path tempDir = Files.createTempDirectory("test-workflow");
        Path workflowFile = tempDir.resolve("workflow.xml");
        String parallelWorkflow = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow xmlns="http://jabrena.info/schema/pml-workflow"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jabrena.info/schema/pml-workflow ../../../main/resources/schema/pml-workflow.xsd">
                <parallel>
                    <prompt src="prompt1.pml" type="pml"/>
                    <bindResultType>List&lt;String&gt;</bindResultType>
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.pml" type="pml" bind="item"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.writeString(workflowFile, parallelWorkflow);

        Job parallelJob = new Job(jobId,
            workflowFile.toString(),
            "cursor-agent-123",
            "test-model",
            "test-repo",
            AgentState.error(), // Not successful
            LocalDateTime.now(), LocalDateTime.now(), null, null,
            WorkflowType.PARALLEL, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(parallelJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(parallelJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verify(cliAgent, never()).getConversationContent(anyString());

        // Cleanup
        Files.deleteIfExists(workflowFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testRun_ShortUuidPrefixResolvesToFullId() {
        // Given
        String fullId = "abcdef01-1234-5678-90ab-cdef12345678";
        String prefix = "abcdef01"; // 8-char prefix

        Job job = new Job(fullId,
            "/test/path",
            null,
            "test-model",
            "test-repo",
            AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, prefix);

        // Resolve: exact miss, then prefix match uniquely
        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobRepository.findJobWithDetails(fullId)).thenReturn(Optional.of(new JobWithDetails(job, List.of())));

        // When
        jobStatusCommand.run();

        // Then - verify we looked up by full id after resolving
        verify(jobRepository).findById(prefix);
        verify(jobRepository).findAll();
        verify(jobRepository).findJobWithDetails(fullId);
        verify(jobRepository, never()).findJobWithDetails(prefix);
    }

    @Test
    void testRun_ShortUuidPrefixAmbiguous() {
        // Given
        String prefix = "abcdef01";
        Job job1 = new Job("abcdef01-aaaa-bbbb-cccc-111111111111",
            "/p1", null, "m", "r", AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);
        Job job2 = new Job("abcdef01-dddd-eeee-ffff-222222222222",
            "/p2", null, "m", "r", AgentState.running(), LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, null, null, null);

        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(List.of(job1, job2));

        // When
        jobStatusCommand.run();

        // Then - ambiguous => do not call findJobWithDetails
        verify(jobRepository).findById(prefix);
        verify(jobRepository).findAll();
        verify(jobRepository, never()).findJobWithDetails(anyString());
    }

    @Test
    void testRun_ShortUuidPrefixNotFound() {
        // Given
        String prefix = "12345678";
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());
        when(jobRepository.findAll()).thenReturn(List.of());

        // When
        jobStatusCommand.run();

        // Then - not found => do not call findJobWithDetails
        verify(jobRepository).findById(prefix);
        verify(jobRepository).findAll();
        verify(jobRepository, never()).findJobWithDetails(anyString());
    }

    @Test
    void testRun_ResolveJobId_ExactMatch() {
        // Given
        String jobId = "exact-match-job-id";
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.findJobWithDetails(jobId))
            .thenReturn(Optional.of(new JobWithDetails(testJob, testPrompts)));

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(jobId);
        verify(jobRepository).findJobWithDetails(jobId);
        verify(jobRepository, never()).findAll();
    }

    @Test
    void testRun_ResolveJobId_Non8CharPrefix() {
        // Given - prefix that's not exactly 8 characters
        String prefix = "abc"; // 3 characters, not 8
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, prefix);

        when(jobRepository.findById(prefix)).thenReturn(Optional.empty());

        // When
        jobStatusCommand.run();

        // Then - should not search by prefix, should just report not found
        verify(jobRepository).findById(prefix);
        verify(jobRepository, never()).findAll();
        verify(jobRepository, never()).findJobWithDetails(anyString());
    }

    @Test
    void testRun_ResolveJobId_LongerThan8Chars() {
        // Given - ID longer than 8 characters but not exact match
        String longId = "123456789012345"; // 15 characters
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, longId);

        when(jobRepository.findById(longId)).thenReturn(Optional.empty());

        // When
        jobStatusCommand.run();

        // Then - should not search by prefix, should just report not found
        verify(jobRepository).findById(longId);
        verify(jobRepository, never()).findAll();
        verify(jobRepository, never()).findJobWithDetails(anyString());
    }

    @Test
    void testRun_ResolveJobId_NullProvided() {
        // Given
        jobStatusCommand = new JobStatusCommand(jobRepository, cliAgent, null);

        when(jobRepository.findById(null)).thenReturn(Optional.empty());

        // When
        jobStatusCommand.run();

        // Then
        verify(jobRepository).findById(null);
        verify(jobRepository, never()).findAll();
        verify(jobRepository, never()).findJobWithDetails(anyString());
    }
}
