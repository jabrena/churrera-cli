package info.jab.churrera.cli.util;

import info.jab.churrera.cli.model.Prompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for mapping Prompt entities to and from XML.
 */
public final class PromptXmlMapper {

    private static final Logger logger = LoggerFactory.getLogger(PromptXmlMapper.class);

    private PromptXmlMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a Prompt to XML string representation.
     *
     * @param prompt the prompt to convert
     * @param formatter the date-time formatter to use
     * @return XML string representation of the prompt
     */
    public static String toXml(Prompt prompt, DateTimeFormatter formatter) {
        return String.format(
                "<prompt>" +
                        "<promptId>%s</promptId>" +
                        "<jobId>%s</jobId>" +
                        "<pmlFile>%s</pmlFile>" +
                        "<status>%s</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "</prompt>",
                XmlUtils.escapeXml(prompt.promptId()),
                XmlUtils.escapeXml(prompt.jobId()),
                XmlUtils.escapeXml(prompt.pmlFile()),
                XmlUtils.escapeXml(prompt.status()),
                prompt.createdAt().format(formatter),
                prompt.lastUpdate().format(formatter));
    }

    /**
     * Parses a Prompt from XML string representation.
     *
     * @param xml the XML string to parse
     * @param formatter the date-time formatter to use
     * @return the parsed Prompt
     */
    public static Prompt fromXml(String xml, DateTimeFormatter formatter) {
        String promptId = XmlUtils.extractXmlValue(xml, "promptId");
        String jobId = XmlUtils.extractXmlValue(xml, "jobId");
        String pmlFile = XmlUtils.extractXmlValue(xml, "pmlFile");
        String status = XmlUtils.extractXmlValue(xml, "status");
        LocalDateTime createdAt = LocalDateTime.parse(XmlUtils.extractXmlValue(xml, "createdAt"), formatter);
        LocalDateTime lastUpdate = LocalDateTime.parse(XmlUtils.extractXmlValue(xml, "lastUpdate"), formatter);

        return new Prompt(promptId, jobId, pmlFile, status, createdAt, lastUpdate);
    }

    /**
     * Parses multiple Prompts from an XML document.
     *
     * @param documentXml the XML document containing multiple prompt elements
     * @param formatter the date-time formatter to use
     * @return list of parsed Prompts
     */
    public static List<Prompt> fromDocument(String documentXml, DateTimeFormatter formatter) {
        List<Prompt> prompts = new ArrayList<>();

        String promptStartTag = "<prompt>";
        String promptEndTag = "</prompt>";

        int startIndex = 0;
        boolean continueParsing = true;
        while (continueParsing) {
            int promptStart = documentXml.indexOf(promptStartTag, startIndex);
            if (promptStart == -1) {
                continueParsing = false;
            } else {
                int promptEnd = documentXml.indexOf(promptEndTag, promptStart);
                if (promptEnd == -1) {
                    continueParsing = false;
                } else {
                    String promptXml = documentXml.substring(promptStart, promptEnd + promptEndTag.length());

                    try {
                        prompts.add(fromXml(promptXml, formatter));
                    } catch (Exception e) {
                        logger.error("Error parsing individual prompt", e);
                        logger.debug("Prompt XML: {}", promptXml);
                    }

                    startIndex = promptEnd + promptEndTag.length();
                }
            }
        }

        return prompts;
    }
}

