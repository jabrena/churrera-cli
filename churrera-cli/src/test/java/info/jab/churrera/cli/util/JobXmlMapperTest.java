package info.jab.churrera.cli.util;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;

/**
 * Unit tests for JobXmlMapper.
 */
@DisplayName("JobXmlMapper Tests")
class JobXmlMapperTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Test
    @DisplayName("Should convert job to XML")
    void shouldConvertJobToXml() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job("job-1", "/path/to/job", "agent-123", "model-1", "repo-1",
                AgentState.CREATING(), now, now, null, null, null, null, null, null, null);

        // When
        String xml = JobXmlMapper.toXml(job, FORMATTER);

        // Then
        assertThat(xml).contains("<job>");
        assertThat(xml).contains("<jobId>job-1</jobId>");
        assertThat(xml).contains("<path>/path/to/job</path>");
        assertThat(xml).contains("<cursorAgentId>agent-123</cursorAgentId>");
        assertThat(xml).contains("<model>model-1</model>");
        assertThat(xml).contains("<repository>repo-1</repository>");
        assertThat(xml).contains("<status>CREATING</status>");
        assertThat(xml).contains("</job>");
    }

    @Test
    @DisplayName("Should convert job with null values to XML")
    void shouldConvertJobWithNullValuesToXml() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job("job-1", "/path/to/job", null, "model-1", "repo-1",
                AgentState.CREATING(), now, now, null, null, null, null, null, null, null);

        // When
        String xml = JobXmlMapper.toXml(job, FORMATTER);

        // Then
        assertThat(xml).contains("<cursorAgentId>null</cursorAgentId>");
        assertThat(xml).contains("<parentJobId>null</parentJobId>");
        assertThat(xml).contains("<result>null</result>");
    }

    @Test
    @DisplayName("Should convert job with all fields to XML")
    void shouldConvertJobWithAllFieldsToXml() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime workflowStart = now.minusHours(1);
        Job job = new Job("job-1", "/path/to/job", "agent-123", "model-1", "repo-1",
                AgentState.RUNNING(), now, now, "parent-1", "result-1", WorkflowType.SEQUENCE,
                5000L, workflowStart, "/fallback.xml", true);

        // When
        String xml = JobXmlMapper.toXml(job, FORMATTER);

        // Then
        assertThat(xml).contains("<parentJobId>parent-1</parentJobId>");
        assertThat(xml).contains("<result>result-1</result>");
        assertThat(xml).contains("<type>SEQUENCE</type>");
        assertThat(xml).contains("<timeoutMillis>5000</timeoutMillis>");
        assertThat(xml).contains("<fallbackSrc>/fallback.xml</fallbackSrc>");
        assertThat(xml).contains("<fallbackExecuted>true</fallbackExecuted>");
    }

    @Test
    @DisplayName("Should parse job from XML")
    void shouldParseJobFromXml() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        String xml = String.format(
                "<job>" +
                        "<jobId>job-1</jobId>" +
                        "<path>/path/to/job</path>" +
                        "<cursorAgentId>agent-123</cursorAgentId>" +
                        "<model>model-1</model>" +
                        "<repository>repo-1</repository>" +
                        "<status>CREATING</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "<parentJobId>null</parentJobId>" +
                        "<result>null</result>" +
                        "<type>null</type>" +
                        "<timeoutMillis>null</timeoutMillis>" +
                        "<workflowStartTime>null</workflowStartTime>" +
                        "<fallbackSrc>null</fallbackSrc>" +
                        "<fallbackExecuted>null</fallbackExecuted>" +
                        "</job>",
                now.format(FORMATTER), now.format(FORMATTER));

        // When
        Job job = JobXmlMapper.fromXml(xml, FORMATTER);

        // Then
        assertThat(job.jobId()).isEqualTo("job-1");
        assertThat(job.path()).isEqualTo("/path/to/job");
        assertThat(job.cursorAgentId()).isEqualTo("agent-123");
        assertThat(job.model()).isEqualTo("model-1");
        assertThat(job.repository()).isEqualTo("repo-1");
        assertThat(job.status()).isEqualTo(AgentState.CREATING());
    }

    @Test
    @DisplayName("Should parse job with null cursor agent ID")
    void shouldParseJobWithNullCursorAgentId() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        String xml = String.format(
                "<job>" +
                        "<jobId>job-1</jobId>" +
                        "<path>/path/to/job</path>" +
                        "<cursorAgentId>null</cursorAgentId>" +
                        "<model>model-1</model>" +
                        "<repository>repo-1</repository>" +
                        "<status>CREATING</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "<parentJobId>null</parentJobId>" +
                        "<result>null</result>" +
                        "<type>null</type>" +
                        "<timeoutMillis>null</timeoutMillis>" +
                        "<workflowStartTime>null</workflowStartTime>" +
                        "<fallbackSrc>null</fallbackSrc>" +
                        "<fallbackExecuted>null</fallbackExecuted>" +
                        "</job>",
                now.format(FORMATTER), now.format(FORMATTER));

        // When
        Job job = JobXmlMapper.fromXml(xml, FORMATTER);

        // Then
        assertThat(job.cursorAgentId()).isNull();
    }

    @Test
    @DisplayName("Should parse job with all fields")
    void shouldParseJobWithAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime workflowStart = now.minusHours(1);
        String xml = String.format(
                "<job>" +
                        "<jobId>job-1</jobId>" +
                        "<path>/path/to/job</path>" +
                        "<cursorAgentId>agent-123</cursorAgentId>" +
                        "<model>model-1</model>" +
                        "<repository>repo-1</repository>" +
                        "<status>RUNNING</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "<parentJobId>parent-1</parentJobId>" +
                        "<result>result-1</result>" +
                        "<type>SEQUENCE</type>" +
                        "<timeoutMillis>5000</timeoutMillis>" +
                        "<workflowStartTime>%s</workflowStartTime>" +
                        "<fallbackSrc>/fallback.xml</fallbackSrc>" +
                        "<fallbackExecuted>true</fallbackExecuted>" +
                        "</job>",
                now.format(FORMATTER), now.format(FORMATTER), workflowStart.format(FORMATTER));

        // When
        Job job = JobXmlMapper.fromXml(xml, FORMATTER);

        // Then
        assertThat(job.parentJobId()).isEqualTo("parent-1");
        assertThat(job.result()).isEqualTo("result-1");
        assertThat(job.type()).isEqualTo(WorkflowType.SEQUENCE);
        assertThat(job.timeoutMillis()).isEqualTo(5000L);
        assertThat(job.workflowStartTime()).isEqualTo(workflowStart);
        assertThat(job.fallbackSrc()).isEqualTo("/fallback.xml");
        assertThat(job.fallbackExecuted()).isTrue();
    }

    @Test
    @DisplayName("Should round trip job conversion")
    void shouldRoundTripJob() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job original = new Job("job-1", "/path/to/job", "agent-123", "model-1", "repo-1",
                AgentState.RUNNING(), now, now, "parent-1", "result-1", WorkflowType.PARALLEL,
                3000L, now.minusMinutes(30), "/fallback.xml", false);

        // When
        String xml = JobXmlMapper.toXml(original, FORMATTER);
        Job parsed = JobXmlMapper.fromXml(xml, FORMATTER);

        // Then
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    @DisplayName("Should parse multiple jobs from document")
    void shouldParseMultipleJobsFromDocument() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        String xml = String.format(
                "<jobs>" +
                        "<job>" +
                        "<jobId>job-1</jobId>" +
                        "<path>/path/1</path>" +
                        "<cursorAgentId>agent-1</cursorAgentId>" +
                        "<model>model-1</model>" +
                        "<repository>repo-1</repository>" +
                        "<status>CREATING</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "<parentJobId>null</parentJobId>" +
                        "<result>null</result>" +
                        "<type>null</type>" +
                        "<timeoutMillis>null</timeoutMillis>" +
                        "<workflowStartTime>null</workflowStartTime>" +
                        "<fallbackSrc>null</fallbackSrc>" +
                        "<fallbackExecuted>null</fallbackExecuted>" +
                        "</job>" +
                        "<job>" +
                        "<jobId>job-2</jobId>" +
                        "<path>/path/2</path>" +
                        "<cursorAgentId>agent-2</cursorAgentId>" +
                        "<model>model-2</model>" +
                        "<repository>repo-2</repository>" +
                        "<status>RUNNING</status>" +
                        "<createdAt>%s</createdAt>" +
                        "<lastUpdate>%s</lastUpdate>" +
                        "<parentJobId>null</parentJobId>" +
                        "<result>null</result>" +
                        "<type>null</type>" +
                        "<timeoutMillis>null</timeoutMillis>" +
                        "<workflowStartTime>null</workflowStartTime>" +
                        "<fallbackSrc>null</fallbackSrc>" +
                        "<fallbackExecuted>null</fallbackExecuted>" +
                        "</job>" +
                        "</jobs>",
                now.format(FORMATTER), now.format(FORMATTER),
                now.format(FORMATTER), now.format(FORMATTER));

        // When
        List<Job> jobs = JobXmlMapper.fromDocument(xml, FORMATTER);

        // Then
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).jobId()).isEqualTo("job-1");
        assertThat(jobs.get(1).jobId()).isEqualTo("job-2");
    }

    @Test
    @DisplayName("Should escape XML special characters in job fields")
    void shouldEscapeXmlInJobFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job("job-1", "/path<with>&special\"chars'", "agent-123", "model-1", "repo-1",
                AgentState.CREATING(), now, now, null, null, null, null, null, null, null);

        // When
        String xml = JobXmlMapper.toXml(job, FORMATTER);
        Job parsed = JobXmlMapper.fromXml(xml, FORMATTER);

        // Then
        assertThat(parsed.path()).isEqualTo("/path<with>&special\"chars'");
    }
}

