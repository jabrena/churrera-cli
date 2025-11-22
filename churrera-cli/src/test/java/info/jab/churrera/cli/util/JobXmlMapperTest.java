package info.jab.churrera.cli.util;

import info.jab.churrera.cli.model.AgentState;
import info.jab.churrera.cli.model.Job;
import info.jab.churrera.workflow.WorkflowType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JobXmlMapper.
 */
class JobXmlMapperTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

    private static LocalDateTime now() {
        return FIXED_TIME;
    }

    private Job createJobWithAllFields() {
        LocalDateTime workflowStart = FIXED_TIME.minusHours(1);
        return new Job(
                "job-1",
                "/path/to/job",
                "agent-123",
                "model-1",
                "repo-1",
                AgentState.RUNNING(),
                FIXED_TIME,
                FIXED_TIME,
                "parent-1",
                "result-1",
                WorkflowType.SEQUENCE,
                5000L,
                workflowStart,
                "/fallback.xml",
                true
        );
    }

    private String buildJobXml(String body) {
        return """
                <job>
                %s
                </job>
                """.formatted(body).replace(System.lineSeparator(), "");
    }

    private String buildBaseJobXml(LocalDateTime timestamp) {
        return buildJobXml(
                """
                        <jobId>job-1</jobId>
                        <path>/path/to/job</path>
                        <cursorAgentId>agent-123</cursorAgentId>
                        <model>model-1</model>
                        <repository>repo-1</repository>
                        <status>CREATING</status>
                        <createdAt>%s</createdAt>
                        <lastUpdate>%s</lastUpdate>
                        <parentJobId>null</parentJobId>
                        <result>null</result>
                        <type>null</type>
                        <timeoutMillis>null</timeoutMillis>
                        <workflowStartTime>null</workflowStartTime>
                        <fallbackSrc>null</fallbackSrc>
                        <fallbackExecuted>null</fallbackExecuted>
                        """.formatted(timestamp.format(FORMATTER), timestamp.format(FORMATTER))
        );
    }

    @Nested
    class ToXmlTests {

        @Test
        void shouldConvertJobToExactXml() {
            // Given
            Job job = new Job("job-1", "/path/to/job", "agent-123", "model-1", "repo-1",
                    AgentState.CREATING(), FIXED_TIME, FIXED_TIME, null, null, null, null, null, null, null);
            String expectedXml = buildBaseJobXml(FIXED_TIME);

            // When
            String xml = JobXmlMapper.toXml(job, FORMATTER);

            // Then
            assertThat(xml).isEqualTo(expectedXml);
        }

        @Test
        void shouldConvertJobWithNullValuesToXml() {
            // Given
            Job job = new Job("job-1", "/path/to/job", null, "model-1", "repo-1",
                    AgentState.CREATING(), FIXED_TIME, FIXED_TIME, null, null, null, null, null, null, null);

            // When
            String xml = JobXmlMapper.toXml(job, FORMATTER);

            // Then
            assertThat(xml).contains("<cursorAgentId>null</cursorAgentId>");
            assertThat(xml).contains("<parentJobId>null</parentJobId>");
            assertThat(xml).contains("<result>null</result>");
        }

        @Test
        void shouldConvertJobWithAllFieldsToXml() {
            // Given
            Job job = createJobWithAllFields();

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
        void shouldEscapeAllStringFields() {
            // Given
            LocalDateTime timestamp = now();
            Job job = new Job(
                    "<id>", "/<path>", "</agent>", "\"model\"", "'repo'",
                    AgentState.RUNNING(), timestamp, timestamp, "<parent>", "<result>", WorkflowType.SEQUENCE,
                    null, null, "<fallback>", true
            );

            // When
            String xml = JobXmlMapper.toXml(job, FORMATTER);

            // Then
            assertThat(xml).contains("&lt;id&gt;")
                    .contains("&lt;path&gt;")
                    .contains("&lt;parent&gt;")
                    .contains("&lt;result&gt;")
                    .contains("&lt;fallback&gt;");
        }
    }

    @Nested
    class FromXmlTests {

        @Test
        void shouldParseJobFromXml() {
            // Given
            LocalDateTime now = now();
            String xml = buildBaseJobXml(now);

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
        void shouldParseJobWithNullCursorAgentId() {
            // Given
            LocalDateTime now = now();
            String xml = buildBaseJobXml(now).replace("<cursorAgentId>agent-123</cursorAgentId>", "<cursorAgentId>null</cursorAgentId>");

            // When
            Job job = JobXmlMapper.fromXml(xml, FORMATTER);

            // Then
            assertThat(job.cursorAgentId()).isNull();
        }

        @Test
        void shouldParseJobWithAllFields() {
            // Given
            LocalDateTime workflowStart = now().minusHours(1);
            String xml = buildJobXml("""
                    <jobId>job-1</jobId>
                    <path>/path/to/job</path>
                    <cursorAgentId>agent-123</cursorAgentId>
                    <model>model-1</model>
                    <repository>repo-1</repository>
                    <status>RUNNING</status>
                    <createdAt>%1$s</createdAt>
                    <lastUpdate>%1$s</lastUpdate>
                    <parentJobId>parent-1</parentJobId>
                    <result>result-1</result>
                    <type>SEQUENCE</type>
                    <timeoutMillis>5000</timeoutMillis>
                    <workflowStartTime>%2$s</workflowStartTime>
                    <fallbackSrc>/fallback.xml</fallbackSrc>
                    <fallbackExecuted>true</fallbackExecuted>
                    """.formatted(now().format(FORMATTER), workflowStart.format(FORMATTER)));

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
        void shouldDefaultToNullWhenWorkflowTypeInvalid() {
            // Given
            String xml = buildBaseJobXml(now()).replace("<type>null</type>", "<type>NOT_A_TYPE</type>");

            // When
            Job job = JobXmlMapper.fromXml(xml, FORMATTER);

            // Then
            assertThat(job.type()).isNull();
        }

        @Test
        void shouldIgnoreInvalidTimeoutAndWorkflowStartValues() {
            // Given
            String xml = buildBaseJobXml(now())
                    .replace("<timeoutMillis>null</timeoutMillis>", "<timeoutMillis>not-a-number</timeoutMillis>")
                    .replace("<workflowStartTime>null</workflowStartTime>", "<workflowStartTime>bad-date</workflowStartTime>");

            // When
            Job job = JobXmlMapper.fromXml(xml, FORMATTER);

            // Then
            assertThat(job.timeoutMillis()).isNull();
            assertThat(job.workflowStartTime()).isNull();
        }

        @Test
        void shouldHandleInvalidBooleanGracefully() {
            // Given
            String xml = buildBaseJobXml(now()).replace("<fallbackExecuted>null</fallbackExecuted>", "<fallbackExecuted>maybe</fallbackExecuted>");

            // When
            Job job = JobXmlMapper.fromXml(xml, FORMATTER);

            // Then
            assertThat(job.fallbackExecuted()).isFalse();
        }

        @Test
        void shouldFailFastWhenRequiredTagMissing() {
            // Given
            String xml = "<job></job>";

            // When & Then
            assertThatThrownBy(() -> JobXmlMapper.fromXml(xml, FORMATTER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("jobId");
        }
    }

    @Test
    void shouldRoundTripJob() {
        // Given
        LocalDateTime now = now();
        Job original = new Job("job-1", "/path/to/job", "agent-123", "model-1", "repo-1",
                AgentState.RUNNING(), now, now, "parent-1", "result-1", WorkflowType.PARALLEL,
                3000L, now.minusMinutes(30), "/fallback.xml", false);

        // When
        String xml = JobXmlMapper.toXml(original, FORMATTER);
        Job parsed = JobXmlMapper.fromXml(xml, FORMATTER);

        // Then
        assertThat(parsed).isEqualTo(original);
    }

    @Nested
    class DocumentParsingTests {

        @Test
        void shouldParseMultipleJobsFromDocument() {
            // Given
            LocalDateTime now = now();
            String xml = """
                    <jobs>
                        %1$s
                        %2$s
                    </jobs>
                    """.formatted(
                    buildBaseJobXml(now),
                    buildBaseJobXml(now).replace("<jobId>job-1</jobId>", "<jobId>job-2</jobId>")
                            .replace("<cursorAgentId>agent-123</cursorAgentId>", "<cursorAgentId>agent-456</cursorAgentId>")
            ).replace(System.lineSeparator(), "");

            // When
            List<Job> jobs = JobXmlMapper.fromDocument(xml, FORMATTER);

            // Then
            assertThat(jobs).hasSize(2);
            assertThat(jobs.get(0).jobId()).isEqualTo("job-1");
            assertThat(jobs.get(1).jobId()).isEqualTo("job-2");
        }

        @Test
        void shouldSkipMalformedJobsButContinueProcessing() {
            // Given
            String validJob = buildBaseJobXml(now());
            String malformedJob = "<job><jobId>broken</job>";
            String xml = "<jobs>" + validJob + malformedJob + validJob.replace("job-1", "job-3") + "</jobs>";

            // When
            List<Job> jobs = JobXmlMapper.fromDocument(xml, FORMATTER);

            // Then
            assertThat(jobs).extracting(Job::jobId).containsExactly("job-1", "job-3");
        }

        @Test
        void shouldReturnEmptyListWhenNoJobsPresent() {
            // When
            List<Job> jobs = JobXmlMapper.fromDocument("<jobs></jobs>", FORMATTER);

            // Then
            assertThat(jobs).isEmpty();
        }
    }
}

