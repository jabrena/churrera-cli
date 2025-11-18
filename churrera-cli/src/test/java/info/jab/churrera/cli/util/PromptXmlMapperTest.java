package info.jab.churrera.cli.util;

import info.jab.churrera.cli.model.Prompt;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;

/**
 * Unit tests for PromptXmlMapper.
 */
@DisplayName("PromptXmlMapper Tests")
class PromptXmlMapperTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Test
    @DisplayName("Should convert prompt to XML")
    void shouldConvertPromptToXml() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Prompt prompt = new Prompt("prompt-1", "job-1", "/path/to/prompt.pml", "SENT", now, now);

        // When
        String xml = PromptXmlMapper.toXml(prompt, FORMATTER);

        // Then
        assertThat(xml).contains("<prompt>");
        assertThat(xml).contains("<promptId>prompt-1</promptId>");
        assertThat(xml).contains("<jobId>job-1</jobId>");
        assertThat(xml).contains("<pmlFile>/path/to/prompt.pml</pmlFile>");
        assertThat(xml).contains("<status>SENT</status>");
        assertThat(xml).contains("</prompt>");
    }

    @Test
    @DisplayName("Should parse prompt from XML")
    void shouldParsePromptFromXml() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        String xml = String.format(
                "<prompt>" +
                        "<promptId>prompt-1</promptId>" +
                        "<jobId>job-1</jobId>" +
                        "<pmlFile>/path/to/prompt.pml</pmlFile>" +
                        "<status>SENT</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "</prompt>",
                now.format(FORMATTER), now.format(FORMATTER));

        // When
        Prompt prompt = PromptXmlMapper.fromXml(xml, FORMATTER);

        // Then
        assertThat(prompt.promptId()).isEqualTo("prompt-1");
        assertThat(prompt.jobId()).isEqualTo("job-1");
        assertThat(prompt.pmlFile()).isEqualTo("/path/to/prompt.pml");
        assertThat(prompt.status()).isEqualTo("SENT");
        assertThat(prompt.createdAt()).isEqualTo(now);
        assertThat(prompt.lastUpdate()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should round trip prompt conversion")
    void shouldRoundTripPrompt() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Prompt original = new Prompt("prompt-1", "job-1", "/path/to/prompt.pml", "SENT", now, now);

        // When
        String xml = PromptXmlMapper.toXml(original, FORMATTER);
        Prompt parsed = PromptXmlMapper.fromXml(xml, FORMATTER);

        // Then
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    @DisplayName("Should parse multiple prompts from document")
    void shouldParseMultiplePromptsFromDocument() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        String xml = String.format(
                "<prompts>" +
                        "<prompt>" +
                        "<promptId>prompt-1</promptId>" +
                        "<jobId>job-1</jobId>" +
                        "<pmlFile>/path/1.pml</pmlFile>" +
                        "<status>SENT</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "</prompt>" +
                        "<prompt>" +
                        "<promptId>prompt-2</promptId>" +
                        "<jobId>job-1</jobId>" +
                        "<pmlFile>/path/2.pml</pmlFile>" +
                        "<status>UNKNOWN</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "</prompt>" +
                        "</prompts>",
                now.format(FORMATTER), now.format(FORMATTER),
                now.format(FORMATTER), now.format(FORMATTER));

        // When
        List<Prompt> prompts = PromptXmlMapper.fromDocument(xml, FORMATTER);

        // Then
        assertThat(prompts).hasSize(2);
        assertThat(prompts.get(0).promptId()).isEqualTo("prompt-1");
        assertThat(prompts.get(1).promptId()).isEqualTo("prompt-2");
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

    @Test
    @DisplayName("Should handle empty document")
    void shouldHandleEmptyDocument() {
        // Given
        String xml = "<prompts></prompts>";

        // When
        List<Prompt> prompts = PromptXmlMapper.fromDocument(xml, FORMATTER);

        // Then
        assertThat(prompts).isEmpty();
    }
}

