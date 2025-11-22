package info.jab.churrera.cli.service;

import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.WorkflowParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WorkflowFileService.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowFileServiceTest {

    @Mock
    private WorkflowParser workflowParser;

    private WorkflowFileService workflowFileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        workflowFileService = new WorkflowFileService(workflowParser);
    }

    @Test
    void testParseWorkflow_Success() throws Exception {
        // Given
        Path workflowPath = tempDir.resolve("workflow.xml");
        Files.createFile(workflowPath);
        
        WorkflowData expectedData = new WorkflowData(
            new PromptInfo("prompt1.pml", "pml"),
            "test-model",
            "test-repo",
            List.of(),
            null, null, null
        );
        when(workflowParser.parse(any(Path.class))).thenReturn(expectedData);

        // When
        WorkflowData result = workflowFileService.parseWorkflow(workflowPath.toString());

        // Then
        assertNotNull(result);
        assertEquals(expectedData, result);
        verify(workflowParser).parse(any(Path.class));
    }

    @Test
    void testParseWorkflow_Exception() throws Exception {
        // Given
        Path workflowPath = tempDir.resolve("workflow.xml");
        when(workflowParser.parse(any(Path.class))).thenThrow(new RuntimeException("Parse error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> {
                String path = workflowPath.toString();
                workflowFileService.parseWorkflow(path);
            });
        assertTrue(exception.getMessage().contains("Failed to parse workflow"));
    }

    @Test
    void testReadPromptFile_Success() throws Exception {
        // Given
        Path workflowPath = tempDir.resolve("workflow.xml");
        Files.createFile(workflowPath);
        Path promptPath = tempDir.resolve("prompt.pml");
        String expectedContent = "Test prompt content";
        Files.writeString(promptPath, expectedContent);

        // When
        String result = workflowFileService.readPromptFile(workflowPath.toString(), "prompt.pml");

        // Then
        assertEquals(expectedContent, result);
    }

    @Test
    void testReadPromptFile_FileNotFound() throws Exception {
        // Given
        Path workflowPath = tempDir.resolve("workflow.xml");
        Files.createFile(workflowPath);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> {
                String path = workflowPath.toString();
                workflowFileService.readPromptFile(path, "nonexistent.pml");
            });
        assertTrue(exception.getMessage().contains("Prompt file not found"));
    }

    @Test
    void testReadPromptFile_IOException() {
        // Given
        Path workflowPath = tempDir.resolve("workflow.xml");
        try {
            Files.createFile(workflowPath);
            Path promptPath = tempDir.resolve("prompt.pml");
            Files.createFile(promptPath);
            // Make file unreadable by deleting it after creating
            Files.delete(promptPath);
        } catch (java.io.IOException e) {
            // Ignore setup errors
        }

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> {
                String path = workflowPath.toString();
                workflowFileService.readPromptFile(path, "prompt.pml");
            });
        assertTrue(exception.getMessage().contains("Failed to read prompt file") || 
                   exception.getMessage().contains("Prompt file not found"));
    }

    @Test
    void testInferTypeFromExtension_Xml() {
        // When
        String result = workflowFileService.inferTypeFromExtension("test.xml");

        // Then
        assertEquals("pml", result);
    }

    @Test
    void testInferTypeFromExtension_Md() {
        // When
        String result = workflowFileService.inferTypeFromExtension("test.md");

        // Then
        assertEquals("markdown", result);
    }

    @Test
    void testInferTypeFromExtension_Txt() {
        // When
        String result = workflowFileService.inferTypeFromExtension("test.txt");

        // Then
        assertEquals("text plain", result);
    }

    @Test
    void testInferTypeFromExtension_CaseInsensitive() {
        // When
        String result = workflowFileService.inferTypeFromExtension("test.XML");

        // Then
        assertEquals("pml", result);
    }

    @Test
    void testInferTypeFromExtension_Null() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> workflowFileService.inferTypeFromExtension(null));
        assertTrue(exception.getMessage().contains("Source file cannot be null or empty"));
    }

    @Test
    void testInferTypeFromExtension_Empty() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> workflowFileService.inferTypeFromExtension(""));
        assertTrue(exception.getMessage().contains("Source file cannot be null or empty"));
    }

    @Test
    void testInferTypeFromExtension_Whitespace() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> workflowFileService.inferTypeFromExtension("   "));
        assertTrue(exception.getMessage().contains("Source file cannot be null or empty"));
    }

    @Test
    void testInferTypeFromExtension_NoExtension() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> workflowFileService.inferTypeFromExtension("test"));
        assertTrue(exception.getMessage().contains("Invalid file extension"));
    }

    @Test
    void testInferTypeFromExtension_DotAtEnd() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> workflowFileService.inferTypeFromExtension("test."));
        assertTrue(exception.getMessage().contains("Invalid file extension"));
    }

    @Test
    void testInferTypeFromExtension_UnsupportedExtension() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> workflowFileService.inferTypeFromExtension("test.pdf"));
        assertTrue(exception.getMessage().contains("Unsupported file extension"));
    }
}

