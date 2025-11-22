package info.jab.churrera.cli.util;

import info.jab.churrera.cli.model.Prompt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptXmlMapperTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, 1, 1, 0, 0);

    private Prompt samplePrompt() {
        return new Prompt("prompt-1", "job-1", "/path/to/prompt.pml", "SENT", FIXED_TIME, FIXED_TIME);
    }

    private String expectedPromptXml(Prompt prompt) {
        return """
                <prompt><promptId>%s</promptId><jobId>%s</jobId><pmlFile>%s</pmlFile><status>%s</status><createdAt>%s</createdAt><lastUpdate>%s</lastUpdate></prompt>
                """.formatted(
                prompt.promptId(),
                prompt.jobId(),
                prompt.pmlFile(),
                prompt.status(),
                prompt.createdAt().format(FORMATTER),
                prompt.lastUpdate().format(FORMATTER)
        ).replace("\n", "").replace("\r", "");
    }

    @Nested
    class ToXmlTests {

        @Test
        void shouldConvertPromptToExactXml() {
            // Given
            Prompt prompt = samplePrompt();

            // When
            String xml = PromptXmlMapper.toXml(prompt, FORMATTER);

            // Then
            assertThat(xml).isEqualTo(expectedPromptXml(prompt));
        }

        @Test
        void shouldEscapeSpecialCharacters() {
            // Given
            Prompt prompt = new Prompt("<id>", "<job>", "/path<&>.pml", "\"status\"", FIXED_TIME, FIXED_TIME);

            // When
            String xml = PromptXmlMapper.toXml(prompt, FORMATTER);

            // Then
            assertThat(xml).contains("&lt;id&gt;").contains("&lt;job&gt;").contains("&lt;&amp;&gt;");
        }
    }

    @Nested
    class FromXmlTests {

        @Test
        void shouldParsePromptFromXml() {
            // Given
            Prompt expected = samplePrompt();
            String xml = expectedPromptXml(expected);

            // When
            Prompt prompt = PromptXmlMapper.fromXml(xml, FORMATTER);

            // Then
            assertThat(prompt).isEqualTo(expected);
        }

        @Test
        void shouldRoundTripPrompt() {
            // Given
            Prompt original = samplePrompt();

            // When
            String xml = PromptXmlMapper.toXml(original, FORMATTER);
            Prompt parsed = PromptXmlMapper.fromXml(xml, FORMATTER);

            // Then
            assertThat(parsed).isEqualTo(original);
        }
    }

    @Nested
    class DocumentParsingTests {

        @Test
        void shouldParseMultiplePromptsFromDocument() {
            // Given
            String doc = "<prompts>" + expectedPromptXml(samplePrompt())
                    + expectedPromptXml(samplePrompt()).replace("prompt-1", "prompt-2")
                    + "</prompts>";

            // When
            List<Prompt> prompts = PromptXmlMapper.fromDocument(doc, FORMATTER);

            // Then
            assertThat(prompts).hasSize(2);
            assertThat(prompts).extracting(Prompt::promptId).containsExactly("prompt-1", "prompt-2");
        }

        @Test
        void shouldSkipMalformedPromptButContinue() {
            // Given
            String malformed = "<prompt><promptId>oops</prompt>";
            String doc = "<prompts>" + expectedPromptXml(samplePrompt()) + malformed
                    + expectedPromptXml(samplePrompt()).replace("prompt-1", "prompt-3") + "</prompts>";

            // When
            List<Prompt> prompts = PromptXmlMapper.fromDocument(doc, FORMATTER);

            // Then
            assertThat(prompts).extracting(Prompt::promptId).containsExactly("prompt-1", "prompt-3");
        }

        @Test
        void shouldHandleEmptyDocument() {
            // When
            List<Prompt> prompts = PromptXmlMapper.fromDocument("<prompts></prompts>", FORMATTER);

            // Then
            assertThat(prompts).isEmpty();
        }
    }

    @Test
    @DisplayName("Should escape XML special characters in prompt fields")
    void shouldEscapeXmlInPromptFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Prompt prompt = new Prompt("prompt-1", "job-1", "/path<with>&special\"chars'.pml", "SENT", now, now);

        // When
        String xml = PromptXmlMapper.toXml(prompt, FORMATTER);
        Prompt parsed = PromptXmlMapper.fromXml(xml, FORMATTER);

        // Then
        assertThat(parsed.pmlFile()).isEqualTo("/path<with>&special\"chars'.pml");
    }

}

