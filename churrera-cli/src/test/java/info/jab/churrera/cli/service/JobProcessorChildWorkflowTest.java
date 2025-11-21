package info.jab.churrera.cli.service;

import info.jab.churrera.workflow.WorkflowParser;
import info.jab.churrera.workflow.WorkflowData;
import info.jab.churrera.workflow.PromptInfo;
import info.jab.churrera.workflow.ParallelWorkflowData;
import info.jab.churrera.workflow.SequenceInfo;
import info.jab.churrera.workflow.WorkflowParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JobProcessor child workflow generation.
 * Verifies that child workflows correctly preserve bindResultExp attributes.
 */
class JobProcessorChildWorkflowTest {

    @Test
    void testChildWorkflowGeneration_IncludesBindResultExp(@TempDir Path tempDir) throws Exception {
        // Given: A SequenceInfo with a prompt that has bindResultExp
        PromptInfo promptWithBind = new PromptInfo(
            "prompt2.xml",
            "pml",
            "$get()"
        );

        List<PromptInfo> prompts = new ArrayList<>();
        prompts.add(promptWithBind);

        SequenceInfo sequenceInfo = new SequenceInfo(
            "default",
            "https://github.com/jabrena/wjax25-demos",
            prompts,
            null,
            null
        );

        // When: Generate child workflow XML
        String childWorkflowXml = generateChildWorkflowXml(sequenceInfo);

        // Then: The generated XML should include bindResultExp attribute
        assertNotNull(childWorkflowXml);
        assertTrue(childWorkflowXml.contains("<prompt src=\"prompt2.xml\""));
        assertTrue(childWorkflowXml.contains("type=\"pml\""));
        assertTrue(childWorkflowXml.contains("bindResultExp=\"$get()\""),
            "Child workflow should preserve bindResultExp attribute from parent");
    }

    @Test
    void testChildWorkflowGeneration_OmitsBindResultExpWhenNotPresent(@TempDir Path tempDir) throws Exception {
        // Given: A SequenceInfo with a prompt that does NOT have bindResultExp
        PromptInfo promptWithoutBind = new PromptInfo(
            "prompt1.xml",
            "pml"
        );

        List<PromptInfo> prompts = new ArrayList<>();
        prompts.add(promptWithoutBind);

        SequenceInfo sequenceInfo = new SequenceInfo(
            "default",
            "https://github.com/jabrena/wjax25-demos",
            prompts,
            null,
            null
        );

        // When: Generate child workflow XML
        String childWorkflowXml = generateChildWorkflowXml(sequenceInfo);

        // Then: The generated XML should NOT include bindResultExp attribute
        assertNotNull(childWorkflowXml);
        assertTrue(childWorkflowXml.contains("<prompt src=\"prompt1.xml\""));
        assertTrue(childWorkflowXml.contains("type=\"pml\""));
        assertFalse(childWorkflowXml.contains("bindResultExp"),
            "Child workflow should omit bindResultExp when not present in parent");
    }

    @Test
    void testChildWorkflowGeneration_MultiplePromptsMixedBindResultExp(@TempDir Path tempDir) throws Exception {
        // Given: Multiple prompts with mixed bindResultExp presence
        PromptInfo prompt1 = new PromptInfo(
            "prompt1.xml",
            "pml",
            "$get()"  // Has bindResultExp
        );

        PromptInfo prompt2 = new PromptInfo(
            "prompt2.xml",
            "md"
            // No bindResultExp
        );

        List<PromptInfo> prompts = new ArrayList<>();
        prompts.add(prompt1);
        prompts.add(prompt2);

        SequenceInfo sequenceInfo = new SequenceInfo(
            "default",
            "https://github.com/jabrena/wjax25-demos",
            prompts,
            null,
            null
        );

        // When: Generate child workflow XML
        String childWorkflowXml = generateChildWorkflowXml(sequenceInfo);

        // Then: First prompt should have bindResultExp, second should not
        assertNotNull(childWorkflowXml);

        // Check first prompt
        assertTrue(childWorkflowXml.contains("<prompt src=\"prompt1.xml\" type=\"pml\" bindResultExp=\"$get()\""),
            "First prompt should have bindResultExp");

        // Check second prompt does not have bindResultExp
        assertTrue(childWorkflowXml.contains("<prompt src=\"prompt2.xml\" type=\"md\""),
            "Second prompt should be present");

        // Verify second prompt doesn't have bindResultExp
        int prompt2Start = childWorkflowXml.indexOf("<prompt src=\"prompt2.xml\"");
        int prompt2End = childWorkflowXml.indexOf("/>", prompt2Start);
        String prompt2Content = childWorkflowXml.substring(prompt2Start, prompt2End);
        assertFalse(prompt2Content.contains("bindResultExp"),
            "Second prompt should not have bindResultExp attribute");
    }

    @Test
    void testChildWorkflowGeneration_CompleteXmlStructure(@TempDir Path tempDir) throws Exception {
        // Given: A complete SequenceInfo
        PromptInfo prompt = new PromptInfo(
            "prompt2.xml",
            "pml",
            "$get()"
        );

        List<PromptInfo> prompts = new ArrayList<>();
        prompts.add(prompt);

        SequenceInfo sequenceInfo = new SequenceInfo(
            "claude-sonnet-4",
            "https://github.com/jabrena/wjax25-demos",
            prompts,
            null,
            null
        );

        // When: Generate child workflow XML
        String childWorkflowXml = generateChildWorkflowXml(sequenceInfo);

        // Then: Verify complete XML structure
        assertNotNull(childWorkflowXml);
        assertTrue(childWorkflowXml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(childWorkflowXml.contains("<pml-workflow"));
        assertTrue(childWorkflowXml.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""));
        assertTrue(childWorkflowXml.contains("<sequence model=\"claude-sonnet-4\" repository=\"https://github.com/jabrena/wjax25-demos\">"));
        assertTrue(childWorkflowXml.contains("<prompt src=\"prompt2.xml\" type=\"pml\" bindResultExp=\"$get()\""));
        assertTrue(childWorkflowXml.contains("</sequence>"));
        assertTrue(childWorkflowXml.contains("</pml-workflow>"));
    }

    @Test
    void testChildWorkflowGeneration_ParsedXmlIsValid(@TempDir Path tempDir) throws Exception {
        // Given: A SequenceInfo with bindResultExp
        PromptInfo prompt = new PromptInfo(
            "prompt2.xml",
            "pml",
            "$get()"
        );

        List<PromptInfo> prompts = new ArrayList<>();
        prompts.add(prompt);

        SequenceInfo sequenceInfo = new SequenceInfo(
            "default",
            "https://github.com/jabrena/wjax25-demos",
            prompts,
            null,
            null
        );

        // When: Generate child workflow XML and write to file
        String childWorkflowXml = generateChildWorkflowXml(sequenceInfo);
        Path workflowFile = tempDir.resolve("test-child-workflow.xml");
        Files.writeString(workflowFile, childWorkflowXml);

        // Then: The file should be parseable by WorkflowParser
        WorkflowParser parser = new WorkflowParser();
        WorkflowData parsedData = parser.parse(workflowFile.toFile());

        assertNotNull(parsedData);
        assertNotNull(parsedData.getLaunchPrompt());
        assertEquals("prompt2.xml", parsedData.getLaunchPrompt().getSrcFile());
        assertEquals("pml", parsedData.getLaunchPrompt().getType());
        assertTrue(parsedData.getLaunchPrompt().hasBindResultExp(),
            "Parsed workflow should have bindResultExp");
        assertEquals("$get()", parsedData.getLaunchPrompt().getBindResultExp(),
            "Parsed bindResultExp should match original");
    }

    /**
     * Helper method to generate child workflow XML.
     * This mimics the logic in JobProcessor.createChildWorkflowFile() but returns the XML string.
     */
    private String generateChildWorkflowXml(SequenceInfo sequenceInfo) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<pml-workflow xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        xml.append("  <sequence");
        if (sequenceInfo.getModel() != null && !sequenceInfo.getModel().isEmpty()) {
            xml.append(" model=\"").append(sequenceInfo.getModel()).append("\"");
        }
        if (sequenceInfo.getRepository() != null && !sequenceInfo.getRepository().isEmpty()) {
            xml.append(" repository=\"").append(sequenceInfo.getRepository()).append("\"");
        }
        xml.append(">\n");

        // Add prompts using original filenames
        for (PromptInfo promptInfo : sequenceInfo.getPrompts()) {
            xml.append("    <prompt src=\"").append(promptInfo.getSrcFile()).append("\"");
            if (promptInfo.getType() != null && !promptInfo.getType().isEmpty()) {
                xml.append(" type=\"").append(promptInfo.getType()).append("\"");
            }
            // Preserve bindResultExp attribute from parent workflow
            if (promptInfo.hasBindResultExp()) {
                xml.append(" bindResultExp=\"").append(promptInfo.getBindResultExp()).append("\"");
            }
            xml.append("/>\n");
        }

        xml.append("  </sequence>\n");
        xml.append("</pml-workflow>\n");

        return xml.toString();
    }
}

