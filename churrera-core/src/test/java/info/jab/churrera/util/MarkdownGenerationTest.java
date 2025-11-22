package info.jab.churrera.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

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
            assertThat(Files.exists(outputPath)).isTrue();
            assertThat(Files.size(outputPath)).isGreaterThan(0);

            // Verify content structure
            String content = Files.readString(outputPath, StandardCharsets.UTF_8);
            assertThat(content)
                .contains("## Role")
                .contains("## Goal")
                .contains("## Output Format")
                .contains("## Safeguards");
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
            assertThat(Files.exists(outputPath)).isTrue();
            assertThat(Files.size(outputPath)).isGreaterThan(0);

            // Verify content structure and basic formatting
            String content = Files.readString(outputPath, StandardCharsets.UTF_8);
            assertThat(content)
                .isNotNull()
                .hasSizeGreaterThan(50)
                .contains("## Role")
                .contains("## Goal")
                .contains("## Safeguards")
                .contains("# ")
                .contains("## ");
        }


    }

    @Nested
    @DisplayName("Directory Structure Tests")
    class DirectoryStructureTests {

        @Test
        @DisplayName("Should create proper directory structure in target")
        void shouldCreateProperDirectoryStructureInTarget() {
            // When & Then
            assertThat(Files.exists(targetDir)).isTrue();
            assertThat(Files.exists(markdownOutputDir)).isTrue();
            assertThat(Files.isDirectory(markdownOutputDir)).isTrue();
        }

    }
}
