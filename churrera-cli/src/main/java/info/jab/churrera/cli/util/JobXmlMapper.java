package info.jab.churrera.cli.util;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.workflow.WorkflowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for mapping Job entities to and from XML.
 */
public final class JobXmlMapper {

    private static final Logger logger = LoggerFactory.getLogger(JobXmlMapper.class);

    private JobXmlMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a Job to XML string representation.
     *
     * @param job the job to convert
     * @param formatter the date-time formatter to use
     * @return XML string representation of the job
     */
    public static String toXml(Job job, DateTimeFormatter formatter) {
        return String.format(
                "<job>" +
                        "<jobId>%s</jobId>" +
                        "<path>%s</path>" +
                        "<cursorAgentId>%s</cursorAgentId>" +
                        "<model>%s</model>" +
                        "<repository>%s</repository>" +
                        "<status>%s</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "<parentJobId>%s</parentJobId>" +
                        "<result>%s</result>" +
                        "<type>%s</type>" +
                        "<timeoutMillis>%s</timeoutMillis>" +
                        "<workflowStartTime>%s</workflowStartTime>" +
                        "<fallbackSrc>%s</fallbackSrc>" +
                        "<fallbackExecuted>%s</fallbackExecuted>" +
                        "</job>",
                XmlUtils.escapeXml(job.jobId()),
                XmlUtils.escapeXml(job.path()),
                job.cursorAgentId() != null ? XmlUtils.escapeXml(job.cursorAgentId()) : "null",
                XmlUtils.escapeXml(job.model()),
                XmlUtils.escapeXml(job.repository()),
                XmlUtils.escapeXml(job.status().toString()),
                job.createdAt().format(formatter),
                job.lastUpdate().format(formatter),
                job.parentJobId() != null ? XmlUtils.escapeXml(job.parentJobId()) : "null",
                job.result() != null ? XmlUtils.escapeXml(job.result()) : "null",
                job.type() != null ? job.type().toString() : "null",
                job.timeoutMillis() != null ? String.valueOf(job.timeoutMillis()) : "null",
                job.workflowStartTime() != null ? job.workflowStartTime().format(formatter) : "null",
                job.fallbackSrc() != null ? XmlUtils.escapeXml(job.fallbackSrc()) : "null",
                job.fallbackExecuted() != null ? String.valueOf(job.fallbackExecuted()) : "null");
    }

    /**
     * Parses a Job from XML string representation.
     *
     * @param xml the XML string to parse
     * @param formatter the date-time formatter to use
     * @return the parsed Job
     */
    public static Job fromXml(String xml, DateTimeFormatter formatter) {
        String jobId = XmlUtils.extractXmlValue(xml, "jobId");
        String path = XmlUtils.extractXmlValue(xml, "path");
        String cursorAgentId = XmlUtils.extractXmlValue(xml, "cursorAgentId");
        if ("null".equals(cursorAgentId)) {
            cursorAgentId = null;
        }
        String model = XmlUtils.extractXmlValue(xml, "model");
        String repository = XmlUtils.extractXmlValue(xml, "repository");
        String statusStr = XmlUtils.extractXmlValue(xml, "status");
        LocalDateTime createdAt = LocalDateTime.parse(XmlUtils.extractXmlValue(xml, "createdAt"), formatter);
        LocalDateTime lastUpdate = LocalDateTime.parse(XmlUtils.extractXmlValue(xml, "lastUpdate"), formatter);

        // Parse new fields with null handling
        String parentJobId = parseNullableString(xml, "parentJobId");
        String result = parseNullableString(xml, "result");
        WorkflowType type = parseWorkflowType(xml, jobId);
        AgentState status = AgentState.of(statusStr);
        Long timeoutMillis = parseTimeoutMillis(xml, jobId);
        LocalDateTime workflowStartTime = parseWorkflowStartTime(xml, formatter, jobId);
        String fallbackSrc = parseNullableString(xml, "fallbackSrc");
        Boolean fallbackExecuted = parseFallbackExecuted(xml, jobId);

        return new Job(jobId, path, cursorAgentId, model, repository, status, createdAt, lastUpdate, parentJobId,
                result, type, timeoutMillis, workflowStartTime, fallbackSrc, fallbackExecuted);
    }

    /**
     * Parses multiple Jobs from an XML document.
     *
     * @param documentXml the XML document containing multiple job elements
     * @param formatter the date-time formatter to use
     * @return list of parsed Jobs
     */
    public static List<Job> fromDocument(String documentXml, DateTimeFormatter formatter) {
        List<Job> jobs = new ArrayList<>();

        // Find all job elements in the document
        String jobStartTag = "<job>";
        String jobEndTag = "</job>";

        int startIndex = 0;
        while (true) {
            int jobStart = documentXml.indexOf(jobStartTag, startIndex);
            if (jobStart == -1) {
                break; // No more jobs found
            }

            int jobEnd = documentXml.indexOf(jobEndTag, jobStart);
            if (jobEnd == -1) {
                break; // Malformed XML
            }

            // Extract the job XML
            String jobXml = documentXml.substring(jobStart, jobEnd + jobEndTag.length());

            try {
                jobs.add(fromXml(jobXml, formatter));
            } catch (Exception e) {
                logger.error("Error parsing individual job", e);
                logger.debug("Job XML: {}", jobXml);
            }

            startIndex = jobEnd + jobEndTag.length();
        }

        return jobs;
    }

    private static String parseNullableString(String xml, String tagName) {
        String value = XmlUtils.extractXmlValueOptional(xml, tagName);
        return "null".equals(value) ? null : value;
    }

    private static WorkflowType parseWorkflowType(String xml, String jobId) {
        String typeStr = XmlUtils.extractXmlValueOptional(xml, "type");
        if (typeStr == null || "null".equals(typeStr)) {
            return null;
        }
        try {
            return WorkflowType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid workflow type '{}' for job {}, defaulting to null", typeStr, jobId);
            return null;
        }
    }

    private static Long parseTimeoutMillis(String xml, String jobId) {
        String timeoutMillisStr = XmlUtils.extractXmlValueOptional(xml, "timeoutMillis");
        if (timeoutMillisStr == null || "null".equals(timeoutMillisStr)) {
            return null;
        }
        try {
            return Long.parseLong(timeoutMillisStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid timeoutMillis '{}' for job {}, defaulting to null", timeoutMillisStr, jobId);
            return null;
        }
    }

    private static LocalDateTime parseWorkflowStartTime(String xml, DateTimeFormatter formatter, String jobId) {
        String workflowStartTimeStr = XmlUtils.extractXmlValueOptional(xml, "workflowStartTime");
        if (workflowStartTimeStr == null || "null".equals(workflowStartTimeStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(workflowStartTimeStr, formatter);
        } catch (Exception e) {
            logger.warn("Invalid workflowStartTime '{}' for job {}, defaulting to null", workflowStartTimeStr, jobId);
            return null;
        }
    }

    private static Boolean parseFallbackExecuted(String xml, String jobId) {
        String fallbackExecutedStr = XmlUtils.extractXmlValueOptional(xml, "fallbackExecuted");
        if (fallbackExecutedStr == null || "null".equals(fallbackExecutedStr)) {
            return Boolean.FALSE;
        }
        try {
            return Boolean.parseBoolean(fallbackExecutedStr);
        } catch (Exception e) {
            logger.warn("Invalid fallbackExecuted '{}' for job {}, defaulting to false", fallbackExecutedStr, jobId);
            return Boolean.FALSE;
        }
    }
}

