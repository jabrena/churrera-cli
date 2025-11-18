package info.jab.churrera.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for WorkflowParser.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowParser Tests")
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

    @Nested
    @DisplayName("Parse Sequence Workflow Tests")
    class ParseSequenceWorkflowTests {

        @Test
        @DisplayName("Should parse valid V2 workflow with multiple prompts")
        void shouldParseValidV2WorkflowWithMultiplePrompts() throws Exception {
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
            assertThat(result).isNotNull();
            assertThat(result.getLaunchPrompt().getSrcFile()).isEqualTo("prompt1.xml");
            assertThat(result.getModel()).isEqualTo("test-model");
            assertThat(result.getRepository()).isEqualTo("test-repo");
            assertThat(result.getUpdatePrompts())
                .hasSize(2)
                .element(0)
                .extracting(PromptInfo::getSrcFile)
                .isEqualTo("prompt2.xml");
            assertThat(result.getUpdatePrompts().get(1).getSrcFile()).isEqualTo("prompt3.xml");
            assertThat(result.hasUpdateAgents()).isTrue();
            assertThat(result.getUpdateAgentCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should parse valid V2 workflow using Path")
        void shouldParseValidV2WorkflowUsingPath() throws Exception {
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
            assertThat(result).isNotNull();
            assertThat(result.getLaunchPrompt().getSrcFile()).isEqualTo("prompt1.xml");
            assertThat(result.getModel()).isEqualTo("test-model");
            assertThat(result.getRepository()).isEqualTo("test-repo");
            assertThat(result.getUpdatePrompts())
                .hasSize(1)
                .element(0)
                .extracting(PromptInfo::getSrcFile)
                .isEqualTo("prompt2.xml");
        }

        @Test
        @DisplayName("Should parse workflow with empty model and repository")
        void shouldParseWorkflowWithEmptyModelAndRepository() throws Exception {
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
            assertThat(result).isNotNull();
            assertThat(result.getLaunchPrompt().getSrcFile()).isEqualTo("prompt1.xml");
            assertThat(result.getModel()).isEmpty();
            assertThat(result.getRepository()).isEmpty();
            assertThat(result.hasUpdateAgents()).isFalse();
            assertThat(result.getUpdateAgentCount()).isZero();
        }

        @Test
        @DisplayName("Should parse workflow with only launch prompt")
        void shouldParseWorkflowWithOnlyLaunchPrompt() throws Exception {
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
            assertThat(result).isNotNull();
            assertThat(result.getLaunchPrompt().getSrcFile()).isEqualTo("prompt1.xml");
            assertThat(result.getModel()).isEqualTo("test-model");
            assertThat(result.getRepository()).isEqualTo("test-repo");
            assertThat(result.hasUpdateAgents()).isFalse();
            assertThat(result.getUpdateAgentCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Parse Error Tests")
    class ParseErrorTests {

        @Test
        @DisplayName("Should throw exception for invalid root element")
        void shouldThrowExceptionForInvalidRootElement() throws Exception {
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
            assertThatThrownBy(() -> workflowParser.parse(testWorkflowFile))
                .isInstanceOf(WorkflowParseException.class);
        }

        @Test
        @DisplayName("Should throw exception when no sequence element")
        void shouldThrowExceptionWhenNoSequenceElement() throws Exception {
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
            assertThatThrownBy(() -> workflowParser.parse(testWorkflowFile))
                .isInstanceOf(WorkflowParseException.class);
        }

        @Test
        @DisplayName("Should throw exception when no prompt elements")
        void shouldThrowExceptionWhenNoPromptElements() throws Exception {
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
            assertThatThrownBy(() -> workflowParser.parse(testWorkflowFile))
                .isInstanceOf(WorkflowParseException.class);
        }

        @Test
        @DisplayName("Should throw exception when prompt missing src attribute")
        void shouldThrowExceptionWhenPromptMissingSrcAttribute() throws Exception {
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
            assertThatThrownBy(() -> workflowParser.parse(testWorkflowFile))
                .isInstanceOf(WorkflowParseException.class);
        }

        @Test
        @DisplayName("Should throw exception when prompt has empty src attribute")
        void shouldThrowExceptionWhenPromptHasEmptySrcAttribute() throws Exception {
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
            assertThatThrownBy(() -> workflowParser.parse(testWorkflowFile))
                .isInstanceOf(WorkflowParseException.class);
        }

        @Test
        @DisplayName("Should throw exception for invalid XML")
        void shouldThrowExceptionForInvalidXml() throws Exception {
            // Given
            String workflowContent = "invalid xml content";
            Files.write(testWorkflowFile.toPath(), workflowContent.getBytes());

            // When & Then
            assertThatThrownBy(() -> workflowParser.parse(testWorkflowFile))
                .isInstanceOf(WorkflowParseException.class);
        }

        @Test
        @DisplayName("Should throw exception when file does not exist")
        void shouldThrowExceptionWhenFileDoesNotExist() {
            // Given
            File nonExistentFile = new File("/non/existent/file.xml");

            // When & Then
            assertThatThrownBy(() -> workflowParser.parse(nonExistentFile))
                .isInstanceOf(WorkflowParseException.class);
        }

        @Test
        @DisplayName("Should throw exception when Path file does not exist")
        void shouldThrowExceptionWhenPathFileDoesNotExist() {
            // Given
            Path nonExistentPath = Paths.get("/non/existent/file.xml");

            // When & Then
            assertThatThrownBy(() -> workflowParser.parse(nonExistentPath))
                .isInstanceOf(WorkflowParseException.class);
        }
    }

    @Nested
    @DisplayName("WorkflowData Tests")
    class WorkflowDataTests {

        @Test
        @DisplayName("Should return immutable update prompts list")
        void shouldReturnImmutableUpdatePromptsList() throws Exception {
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
            updatePrompts.add(new PromptInfo("new-file.xml", "pml"));

            // Then
            assertThat(result.getUpdatePrompts())
                .hasSize(1)
                .element(0)
                .extracting(PromptInfo::getSrcFile)
                .isEqualTo("prompt2.xml");
        }

        @Test
        @DisplayName("Should handle empty update files")
        void shouldHandleEmptyUpdateFiles() throws Exception {
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
            assertThat(result.getUpdatePrompts()).isNotNull().isEmpty();
            assertThat(result.hasUpdateAgents()).isFalse();
            assertThat(result.getUpdateAgentCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Parse Parallel Workflow Tests")
    class ParseParallelWorkflowTests {

        @Test
        @DisplayName("Should parse parallel workflow")
        void shouldParseParallelWorkflow() throws Exception {
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
            assertThat(result).isNotNull();
            assertThat(result.isParallelWorkflow()).isTrue();
            assertThat(result.getLaunchPrompt().getSrcFile()).isEqualTo("prompt1.xml");
            assertThat(result.getLaunchPrompt().getType()).isEqualTo("pml");
            assertThat(result.getModel()).isEqualTo("test-model");
            assertThat(result.getRepository()).isEqualTo("test-repo");

            ParallelWorkflowData parallelData = result.getParallelWorkflowData();
            assertThat(parallelData).isNotNull();
            assertThat(parallelData.hasBindResultType()).isTrue();
            assertThat(parallelData.getBindResultType()).isEqualTo("List_Integer");
            assertThat(parallelData.getSequences()).hasSize(1);

            SequenceInfo sequence = parallelData.getSequences().get(0);
            assertThat(sequence.getModel()).isEqualTo("test-model");
            assertThat(sequence.getRepository()).isEqualTo("test-repo");
            assertThat(sequence.getPrompts())
                .hasSize(2)
                .element(0)
                .extracting(PromptInfo::getSrcFile)
                .isEqualTo("prompt2.xml");
            assertThat(sequence.getPrompts().get(1).getSrcFile()).isEqualTo("prompt3.xml");
        }

        @Test
        @DisplayName("Should parse parallel workflow without bindResultType")
        void shouldParseParallelWorkflowWithoutBindResultType() throws Exception {
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
            assertThat(result).isNotNull();
            assertThat(result.isParallelWorkflow()).isTrue();
            ParallelWorkflowData parallelData = result.getParallelWorkflowData();
            assertThat(parallelData).isNotNull();
            assertThat(parallelData.hasBindResultType()).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when parallel workflow missing src attribute")
        void shouldThrowExceptionWhenParallelWorkflowMissingSrcAttribute() throws Exception {
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
            assertThatThrownBy(() -> workflowParser.parse(testWorkflowFile))
                .isInstanceOf(WorkflowParseException.class);
        }

        @Test
        @DisplayName("Should throw exception when parallel workflow has no sequences")
        void shouldThrowExceptionWhenParallelWorkflowHasNoSequences() throws Exception {
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
            assertThatThrownBy(() -> workflowParser.parse(testWorkflowFile))
                .isInstanceOf(WorkflowParseException.class);
        }

        @Test
        @DisplayName("Should parse parallel workflow with multiple sequences")
        void shouldParseParallelWorkflowWithMultipleSequences() throws Exception {
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
            assertThat(result).isNotNull();
            assertThat(result.isParallelWorkflow()).isTrue();
            ParallelWorkflowData parallelData = result.getParallelWorkflowData();
            assertThat(parallelData.getSequences())
                .hasSize(2)
                .element(0)
                .extracting(SequenceInfo::getModel)
                .isEqualTo("model1");
            assertThat(parallelData.getSequences().get(1).getModel()).isEqualTo("model2");
        }
    }

    @Nested
    @DisplayName("Prompt Tests")
    class PromptTests {

        @Test
        @DisplayName("Should parse prompt with bindResultExp")
        void shouldParsePromptWithBindResultExp() throws Exception {
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
            assertThat(result).isNotNull();
            PromptInfo launchPrompt = result.getLaunchPrompt();
            assertThat(launchPrompt.hasBindResultExp()).isTrue();
            assertThat(launchPrompt.getBindResultExp()).isEqualTo("$get()");
            assertThat(launchPrompt.isPml()).isTrue();

            // Second prompt has no bindResultExp
            assertThat(result.getUpdatePrompts().get(0).hasBindResultExp()).isFalse();
        }

        @Test
        @DisplayName("Should handle prompt with empty bindResultExp")
        void shouldHandlePromptWithEmptyBindResultExp() throws Exception {
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
            assertThat(result).isNotNull();
            PromptInfo launchPrompt = result.getLaunchPrompt();
            assertThat(launchPrompt.hasBindResultExp()).isFalse();
        }

        @Test
        @DisplayName("Should default prompt type to pml")
        void shouldDefaultPromptTypeToPml() throws Exception {
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
            assertThat(result).isNotNull();
            assertThat(result.getLaunchPrompt().getType()).isEqualTo("pml");
            assertThat(result.getLaunchPrompt().isPml()).isTrue();
        }

        @Test
        @DisplayName("Should parse prompt with non-pml type")
        void shouldParsePromptWithNonPmlType() throws Exception {
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
            assertThat(result).isNotNull();
            assertThat(result.getLaunchPrompt().getType()).isEqualTo("markdown");
            assertThat(result.getLaunchPrompt().isPml()).isFalse();
        }
    }

    @Nested
    @DisplayName("DetermineWorkflowType Tests")
    class DetermineWorkflowTypeTests {

        @Test
        @DisplayName("Should determine sequence workflow type")
        void shouldDetermineSequenceWorkflowType() throws Exception {
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
            assertThat(result).isEqualTo(WorkflowType.SEQUENCE);
        }

        @Test
        @DisplayName("Should determine parallel workflow type")
        void shouldDetermineParallelWorkflowType() throws Exception {
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
            assertThat(result).isEqualTo(WorkflowType.PARALLEL);
        }

        @Test
        @DisplayName("Should return null for invalid root element")
        void shouldReturnNullForInvalidRootElement() throws Exception {
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
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null when no sequence or parallel element")
        void shouldReturnNullWhenNoSequenceOrParallelElement() throws Exception {
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
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null when file does not exist")
        void shouldReturnNullWhenFileDoesNotExist() {
            // Given
            File nonExistentFile = new File("/non/existent/file.xml");

            // When
            WorkflowType result = WorkflowParser.determineWorkflowType(nonExistentFile);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Data Class Tests")
    class DataClassTests {

        @Test
        @DisplayName("Should create PromptInfo with bindResultExp")
        void shouldCreatePromptInfoWithBindResultExp() {
            // When
            PromptInfo promptInfo = new PromptInfo("test.xml", "pml", "$get()");

            // Then
            assertThat(promptInfo.getSrcFile()).isEqualTo("test.xml");
            assertThat(promptInfo.getType()).isEqualTo("pml");
            assertThat(promptInfo.getBindResultExp()).isEqualTo("$get()");
            assertThat(promptInfo.hasBindResultExp()).isTrue();
            assertThat(promptInfo.isPml()).isTrue();
        }

        @Test
        @DisplayName("Should create PromptInfo without bindResultExp")
        void shouldCreatePromptInfoWithoutBindResultExp() {
            // When
            PromptInfo promptInfo = new PromptInfo("test.md", "md");

            // Then
            assertThat(promptInfo.getSrcFile()).isEqualTo("test.md");
            assertThat(promptInfo.getType()).isEqualTo("md");
            assertThat(promptInfo.getBindResultExp()).isNull();
            assertThat(promptInfo.hasBindResultExp()).isFalse();
            assertThat(promptInfo.isPml()).isFalse();
        }

        @Test
        @DisplayName("Should create SequenceInfo with immutable prompts")
        void shouldCreateSequenceInfoWithImmutablePrompts() {
            // Given
            List<PromptInfo> prompts = List.of(
                new PromptInfo("test.xml", "pml")
            );

            // When
            SequenceInfo sequenceInfo = new SequenceInfo("model", "repo", prompts, null, null);

            // Then
            assertThat(sequenceInfo.getModel()).isEqualTo("model");
            assertThat(sequenceInfo.getRepository()).isEqualTo("repo");
            assertThat(sequenceInfo.getPrompts())
                .hasSize(1)
                .element(0)
                .extracting(PromptInfo::getSrcFile)
                .isEqualTo("test.xml");

            // Test immutability
            List<PromptInfo> returnedPrompts = sequenceInfo.getPrompts();
            returnedPrompts.add(new PromptInfo("test2.xml", "pml"));
            assertThat(sequenceInfo.getPrompts()).hasSize(1);
        }

        @Test
        @DisplayName("Should create ParallelWorkflowData with immutable sequences")
        void shouldCreateParallelWorkflowDataWithImmutableSequences() {
            // Given
            PromptInfo parallelPrompt = new PromptInfo("parallel.xml", "pml");
            List<SequenceInfo> sequences = List.of(
                new SequenceInfo("model", "repo", List.of(), null, null)
            );

            // When
            ParallelWorkflowData parallelData = new ParallelWorkflowData(
                parallelPrompt, "List_Integer", sequences, null, null
            );

            // Then
            assertThat(parallelData.getParallelPrompt()).isEqualTo(parallelPrompt);
            assertThat(parallelData.getBindResultType()).isEqualTo("List_Integer");
            assertThat(parallelData.hasBindResultType()).isTrue();
            assertThat(parallelData.getSequences()).hasSize(1);

            // Test immutability
            List<SequenceInfo> returnedSequences = parallelData.getSequences();
            returnedSequences.add(new SequenceInfo("model2", "repo2", List.of(), null, null));
            assertThat(parallelData.getSequences()).hasSize(1);
        }

        @Test
        @DisplayName("Should create WorkflowParseException with message")
        void shouldCreateWorkflowParseExceptionWithMessage() {
            // When
            WorkflowParseException exception = new WorkflowParseException("Test message");

            // Then
            assertThat(exception.getMessage()).isEqualTo("Test message");
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create WorkflowParseException with message and cause")
        void shouldCreateWorkflowParseExceptionWithMessageAndCause() {
            // Given
            Throwable cause = new RuntimeException("Cause message");

            // When
            WorkflowParseException exception = new WorkflowParseException("Test message", cause);

            // Then
            assertThat(exception.getMessage()).isEqualTo("Test message");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }
}

