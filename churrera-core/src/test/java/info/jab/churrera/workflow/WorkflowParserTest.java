package info.jab.churrera.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkflowParser.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowParserTest {

    private WorkflowParser workflowParser;
    private File testWorkflowFile;

    @BeforeEach
    void setUp() throws IOException {
        workflowParser = new WorkflowParser();

        // Create a temporary workflow file for testing
        testWorkflowFile = File.createTempFile("test-workflow", ".xml");
        testWorkflowFile.deleteOnExit();
    }

    @Test
    void testParse_ValidV2Workflow() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml"/>
                    <prompt src="prompt2.xml"/>
                    <prompt src="prompt3.xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        assertEquals("prompt1.xml", result.getLaunchPrompt().getSrcFile());
        assertEquals("test-model", result.getModel());
        assertEquals("test-repo", result.getRepository());
        assertEquals(2, result.getUpdatePrompts().size());
        assertEquals("prompt2.xml", result.getUpdatePrompts().get(0).getSrcFile());
        assertEquals("prompt3.xml", result.getUpdatePrompts().get(1).getSrcFile());
        assertTrue(result.hasUpdateAgents());
        assertEquals(2, result.getUpdateAgentCount());
    }

    @Test
    void testParse_ValidV2WorkflowWithPath() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml"/>
                    <prompt src="prompt2.xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile.toPath());

        // Then
        assertNotNull(result);
        assertEquals("prompt1.xml", result.getLaunchPrompt().getSrcFile());
        assertEquals("test-model", result.getModel());
        assertEquals("test-repo", result.getRepository());
        assertEquals(1, result.getUpdatePrompts().size());
        assertEquals("prompt2.xml", result.getUpdatePrompts().get(0).getSrcFile());
    }

    @Test
    void testParse_WorkflowWithEmptyModelAndRepository() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence>
                    <prompt src="prompt1.xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        assertEquals("prompt1.xml", result.getLaunchPrompt().getSrcFile());
        assertEquals("", result.getModel());
        assertEquals("", result.getRepository());
        assertFalse(result.hasUpdateAgents());
        assertEquals(0, result.getUpdateAgentCount());
    }

    @Test
    void testParse_WorkflowWithOnlyLaunchPrompt() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        assertEquals("prompt1.xml", result.getLaunchPrompt().getSrcFile());
        assertEquals("test-model", result.getModel());
        assertEquals("test-repo", result.getRepository());
        assertFalse(result.hasUpdateAgents());
        assertEquals(0, result.getUpdateAgentCount());
    }

    @Test
    void testParse_InvalidRootElement() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <invalid-root>
                <sequence model="test-model" repository="test-repo">
                    <prompt pml="prompt1.xml"/>
                </sequence>
            </invalid-root>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(testWorkflowFile));
    }

    @Test
    void testParse_NoSequenceElement() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <invalid-element>
                    <prompt pml="prompt1.xml"/>
                </invalid-element>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(testWorkflowFile));
    }

    @Test
    void testParse_NoPromptElements() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(testWorkflowFile));
    }

    @Test
    void testParse_PromptMissingPmlAttribute() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(testWorkflowFile));
    }

    @Test
    void testParse_PromptWithEmptyPmlAttribute() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt pml=""/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(testWorkflowFile));
    }

    @Test
    void testParse_InvalidXml() throws Exception {
        // Given
        String workflowContent = "invalid xml content";
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(testWorkflowFile));
    }

    @Test
    void testParse_FileNotFound() {
        // Given
        File nonExistentFile = new File("/non/existent/file.xml");

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(nonExistentFile));
    }

    @Test
    void testParse_PathFileNotFound() {
        // Given
        Path nonExistentPath = Paths.get("/non/existent/file.xml");

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(nonExistentPath));
    }

    @Test
    void testWorkflowData_Immutability() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml"/>
                    <prompt src="prompt2.xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);
        List<PromptInfo> updatePrompts = result.getUpdatePrompts();

        // Then
        // Verify that the returned list is a copy and modifications don't affect the original
        updatePrompts.add(new PromptInfo("new-file.xml", "pml"));
        assertEquals(1, result.getUpdatePrompts().size());
        assertEquals("prompt2.xml", result.getUpdatePrompts().get(0).getSrcFile());
    }

    @Test
    void testWorkflowData_EmptyUpdateFiles() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result.getUpdatePrompts());
        assertTrue(result.getUpdatePrompts().isEmpty());
        assertFalse(result.hasUpdateAgents());
        assertEquals(0, result.getUpdateAgentCount());
    }

    @Test
    void testParse_ParallelWorkflow() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <parallel src="prompt1.xml" bindResultType="List_Integer">
                    <sequence model="test-model" repository="test-repo">
                        <prompt src="prompt2.xml"/>
                        <prompt src="prompt3.xml"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        assertTrue(result.isParallelWorkflow());
        assertEquals("prompt1.xml", result.getLaunchPrompt().getSrcFile());
        assertEquals("pml", result.getLaunchPrompt().getType());
        assertEquals("test-model", result.getModel());
        assertEquals("test-repo", result.getRepository());

        ParallelWorkflowData parallelData = result.getParallelWorkflowData();
        assertNotNull(parallelData);
        assertTrue(parallelData.hasBindResultType());
        assertEquals("List_Integer", parallelData.getBindResultType());
        assertEquals(1, parallelData.getSequences().size());

        SequenceInfo sequence = parallelData.getSequences().get(0);
        assertEquals("test-model", sequence.getModel());
        assertEquals("test-repo", sequence.getRepository());
        assertEquals(2, sequence.getPrompts().size());
        assertEquals("prompt2.xml", sequence.getPrompts().get(0).getSrcFile());
        assertEquals("prompt3.xml", sequence.getPrompts().get(1).getSrcFile());
    }

    @Test
    void testParse_ParallelWorkflow_WithoutBindResultType() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <parallel src="prompt1.xml">
                    <sequence model="test-model" repository="test-repo">
                        <prompt src="prompt2.xml"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        assertTrue(result.isParallelWorkflow());
        ParallelWorkflowData parallelData = result.getParallelWorkflowData();
        assertNotNull(parallelData);
        assertFalse(parallelData.hasBindResultType());
    }

    @Test
    void testParse_ParallelWorkflow_MissingSrcAttribute() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <parallel>
                    <sequence model="test-model" repository="test-repo">
                        <prompt src="prompt2.xml"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(testWorkflowFile));
    }

    @Test
    void testParse_ParallelWorkflow_NoSequences() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <parallel src="prompt1.xml">
                </parallel>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When & Then
        assertThrows(WorkflowParseException.class, () -> workflowParser.parse(testWorkflowFile));
    }

    @Test
    void testParse_ParallelWorkflow_MultipleSequences() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <parallel src="prompt1.xml" bindResultType="List_Integer">
                    <sequence model="model1" repository="repo1">
                        <prompt src="prompt2.xml"/>
                    </sequence>
                    <sequence model="model2" repository="repo2">
                        <prompt src="prompt3.xml"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        assertTrue(result.isParallelWorkflow());
        ParallelWorkflowData parallelData = result.getParallelWorkflowData();
        assertEquals(2, parallelData.getSequences().size());
        assertEquals("model1", parallelData.getSequences().get(0).getModel());
        assertEquals("model2", parallelData.getSequences().get(1).getModel());
    }

    @Test
    void testParse_PromptWithBindResultExp() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml" bindResultExp="$get()"/>
                    <prompt src="prompt2.xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        PromptInfo launchPrompt = result.getLaunchPrompt();
        assertTrue(launchPrompt.hasBindResultExp());
        assertEquals("$get()", launchPrompt.getBindResultExp());
        assertTrue(launchPrompt.isPml());

        // Second prompt has no bindResultExp
        assertFalse(result.getUpdatePrompts().get(0).hasBindResultExp());
    }

    @Test
    void testParse_PromptWithEmptyBindResultExp() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml" bindResultExp=""/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        PromptInfo launchPrompt = result.getLaunchPrompt();
        assertFalse(launchPrompt.hasBindResultExp());
    }

    @Test
    void testParse_PromptDefaultsTosPmlType() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        assertEquals("pml", result.getLaunchPrompt().getType());
        assertTrue(result.getLaunchPrompt().isPml());
    }

    @Test
    void testParse_PromptNonPmlType() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.md"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowData result = workflowParser.parse(testWorkflowFile);

        // Then
        assertNotNull(result);
        assertEquals("markdown", result.getLaunchPrompt().getType());
        assertFalse(result.getLaunchPrompt().isPml());
    }

    @Test
    void testDetermineWorkflowType_SequenceWorkflow() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml"/>
                </sequence>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowType result = WorkflowParser.determineWorkflowType(testWorkflowFile);

        // Then
        assertEquals(WorkflowType.SEQUENCE, result);
    }

    @Test
    void testDetermineWorkflowType_ParallelWorkflow() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
                <parallel src="prompt1.xml">
                    <sequence model="test-model" repository="test-repo">
                        <prompt src="prompt2.xml"/>
                    </sequence>
                </parallel>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowType result = WorkflowParser.determineWorkflowType(testWorkflowFile);

        // Then
        assertEquals(WorkflowType.PARALLEL, result);
    }

    @Test
    void testDetermineWorkflowType_InvalidRootElement() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <invalid-root>
                <sequence model="test-model" repository="test-repo">
                    <prompt src="prompt1.xml"/>
                </sequence>
            </invalid-root>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowType result = WorkflowParser.determineWorkflowType(testWorkflowFile);

        // Then
        assertNull(result);
    }

    @Test
    void testDetermineWorkflowType_NoSequenceOrParallel() throws Exception {
        // Given
        String workflowContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pml-workflow>
            </pml-workflow>
            """;
        Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

        // When
        WorkflowType result = WorkflowParser.determineWorkflowType(testWorkflowFile);

        // Then
        assertNull(result);
    }

    @Test
    void testDetermineWorkflowType_FileNotFound() {
        // Given
        File nonExistentFile = new File("/non/existent/file.xml");

        // When
        WorkflowType result = WorkflowParser.determineWorkflowType(nonExistentFile);

        // Then
        assertNull(result);
    }

    @Test
    void testPromptInfo_Constructor_WithBindResultExp() {
        // When
        PromptInfo promptInfo = new PromptInfo("test.xml", "pml", "$get()");

        // Then
        assertEquals("test.xml", promptInfo.getSrcFile());
        assertEquals("pml", promptInfo.getType());
        assertEquals("$get()", promptInfo.getBindResultExp());
        assertTrue(promptInfo.hasBindResultExp());
        assertTrue(promptInfo.isPml());
    }

    @Test
    void testPromptInfo_Constructor_WithoutBindResultExp() {
        // When
        PromptInfo promptInfo = new PromptInfo("test.md", "md");

        // Then
        assertEquals("test.md", promptInfo.getSrcFile());
        assertEquals("md", promptInfo.getType());
        assertNull(promptInfo.getBindResultExp());
        assertFalse(promptInfo.hasBindResultExp());
        assertFalse(promptInfo.isPml());
    }

    @Test
    void testSequenceInfo_Constructor() {
        // Given
        List<PromptInfo> prompts = List.of(
            new PromptInfo("test.xml", "pml")
        );

        // When
        SequenceInfo sequenceInfo = new SequenceInfo("model", "repo", prompts);

        // Then
        assertEquals("model", sequenceInfo.getModel());
        assertEquals("repo", sequenceInfo.getRepository());
        assertEquals(1, sequenceInfo.getPrompts().size());
        assertEquals("test.xml", sequenceInfo.getPrompts().get(0).getSrcFile());

        // Test immutability
        List<PromptInfo> returnedPrompts = sequenceInfo.getPrompts();
        returnedPrompts.add(new PromptInfo("test2.xml", "pml"));
        assertEquals(1, sequenceInfo.getPrompts().size());
    }

    @Test
    void testParallelWorkflowData_Constructor() {
        // Given
        PromptInfo parallelPrompt = new PromptInfo("parallel.xml", "pml");
        List<SequenceInfo> sequences = List.of(
            new SequenceInfo("model", "repo", List.of())
        );

        // When
        ParallelWorkflowData parallelData = new ParallelWorkflowData(
            parallelPrompt, "List_Integer", sequences
        );

        // Then
        assertEquals(parallelPrompt, parallelData.getParallelPrompt());
        assertEquals("List_Integer", parallelData.getBindResultType());
        assertTrue(parallelData.hasBindResultType());
        assertEquals(1, parallelData.getSequences().size());

        // Test immutability
        List<SequenceInfo> returnedSequences = parallelData.getSequences();
        returnedSequences.add(new SequenceInfo("model2", "repo2", List.of()));
        assertEquals(1, parallelData.getSequences().size());
    }

    @Test
    void testWorkflowParseException_WithMessage() {
        // When
        WorkflowParseException exception = new WorkflowParseException("Test message");

        // Then
        assertEquals("Test message", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testWorkflowParseException_WithMessageAndCause() {
        // Given
        Throwable cause = new RuntimeException("Cause message");

        // When
        WorkflowParseException exception = new WorkflowParseException("Test message", cause);

        // Then
        assertEquals("Test message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}

