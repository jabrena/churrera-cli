package info.jab.churrera.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for generating markdown output files in the target directory for local debugging.
 * This class transforms PML XML files to Markdown and saves them to the target directory
 * so developers can easily inspect the transformation results.
 */
@DisplayName("Markdown Generation Tests")
class MarkdownGenerationTest {

    private Path targetDir;
    private Path markdownOutputDir;
    private PmlConverter converter;

    @BeforeEach
    void setUp() {
        // Set up target directory structure
        targetDir = Paths.get("target");
        markdownOutputDir = targetDir.resolve("generated-markdown");

        // Create directories if they don't exist
        try {
            Files.createDirectories(markdownOutputDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create markdown output directory", e);
        }
        converter = new PmlConverter();
    }

    @Nested
    @DisplayName("Single File Generation Tests")
    class SingleFileGenerationTests {

        @Test
        @DisplayName("Should generate markdown for prompt1.xml")
        void shouldGenerateMarkdownForPrompt1Xml() throws Exception {
            // Given
            String pmlFile = "examples/hello-world/prompt1.xml";
            String expectedOutputFile = "prompt1.md";

            // When
            String markdownContent = converter.toMarkdown(pmlFile);
            Path outputPath = markdownOutputDir.resolve(expectedOutputFile);
            Files.write(outputPath, markdownContent.getBytes(StandardCharsets.UTF_8));

            // Then
            assertTrue(Files.exists(outputPath), "Markdown file should be created");
            assertTrue(Files.size(outputPath) > 0, "Markdown file should not be empty");

            // Verify content structure
            String content = Files.readString(outputPath, StandardCharsets.UTF_8);
            assertTrue(content.contains("## Role"), "Generated markdown should contain Role section");
            assertTrue(content.contains("## Goal"), "Generated markdown should contain Goal section");
            assertTrue(content.contains("## Output Format"), "Generated markdown should contain Output Format section");
            assertTrue(content.contains("## Safeguards"), "Generated markdown should contain Safeguards section");
        }

        @Test
        @DisplayName("Should generate markdown for prompt2.xml")
        void shouldGenerateMarkdownForPrompt2Xml() throws Exception {
            // Given
            String pmlFile = "examples/hello-world/prompt2.xml";
            String expectedOutputFile = "prompt2.md";

            // When
            String markdownContent = converter.toMarkdown(pmlFile);
            Path outputPath = markdownOutputDir.resolve(expectedOutputFile);
            Files.write(outputPath, markdownContent.getBytes(StandardCharsets.UTF_8));

            // Then
            assertTrue(Files.exists(outputPath), "Markdown file should be created");
            assertTrue(Files.size(outputPath) > 0, "Markdown file should not be empty");

            // Verify content structure and basic formatting
            String content = Files.readString(outputPath, StandardCharsets.UTF_8);
            assertNotNull(content, "Generated markdown content should not be null");
            assertTrue(content.length() > 50, "Generated markdown should have substantial content");

            // Verify required sections exist
            assertTrue(content.contains("## Role"), "Generated markdown should contain Role section");
            assertTrue(content.contains("## Goal"), "Generated markdown should contain Goal section");
            assertTrue(content.contains("## Safeguards"), "Generated markdown should contain Safeguards section");

            // Verify basic markdown structure
            assertTrue(content.contains("# "), "Generated markdown should contain a title (H1 header)");
            assertTrue(content.contains("## "), "Generated markdown should contain section headers (H2 headers)");
        }


    }

    @Nested
    @DisplayName("Directory Structure Tests")
    class DirectoryStructureTests {

        @Test
        @DisplayName("Should create proper directory structure in target")
        void shouldCreateProperDirectoryStructureInTarget() {
            // When & Then
            assertTrue(Files.exists(targetDir), "Target directory should exist");
            assertTrue(Files.exists(markdownOutputDir), "Markdown output directory should exist");
            assertTrue(Files.isDirectory(markdownOutputDir), "Markdown output should be a directory");
        }

    }
}
