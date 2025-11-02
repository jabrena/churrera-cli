package info.jab.churrera.cli.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Prompt record.
 */
class PromptTest {

    @Test
    void shouldCreateValidPrompt() {
        // Given
        String promptId = "prompt-123";
        String jobId = "job-456";
        String pmlFile = "/path/to/prompt.pml";
        String status = "PENDING";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When
        Prompt prompt = new Prompt(promptId, jobId, pmlFile, status, createdAt, lastUpdate);

        // Then
        assertThat(prompt.promptId()).isEqualTo(promptId);
        assertThat(prompt.jobId()).isEqualTo(jobId);
        assertThat(prompt.pmlFile()).isEqualTo(pmlFile);
        assertThat(prompt.status()).isEqualTo(status);
        assertThat(prompt.createdAt()).isEqualTo(createdAt);
        assertThat(prompt.lastUpdate()).isEqualTo(lastUpdate);
    }

    @Test
    void shouldThrowExceptionWhenPromptIdIsNull() {
        // Given
        String jobId = "job-456";
        String pmlFile = "/path/to/prompt.pml";
        String status = "PENDING";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Prompt(null, jobId, pmlFile, status, createdAt, lastUpdate))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Prompt ID cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenJobIdIsNull() {
        // Given
        String promptId = "prompt-123";
        String pmlFile = "/path/to/prompt.pml";
        String status = "PENDING";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Prompt(promptId, null, pmlFile, status, createdAt, lastUpdate))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Job ID cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenPmlFileIsNull() {
        // Given
        String promptId = "prompt-123";
        String jobId = "job-456";
        String status = "PENDING";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Prompt(promptId, jobId, null, status, createdAt, lastUpdate))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("PML file cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenStatusIsNull() {
        // Given
        String promptId = "prompt-123";
        String jobId = "job-456";
        String pmlFile = "/path/to/prompt.pml";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Prompt(promptId, jobId, pmlFile, null, createdAt, lastUpdate))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Status cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenCreatedAtIsNull() {
        // Given
        String promptId = "prompt-123";
        String jobId = "job-456";
        String pmlFile = "/path/to/prompt.pml";
        String status = "PENDING";
        LocalDateTime lastUpdate = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Prompt(promptId, jobId, pmlFile, status, null, lastUpdate))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Created at cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenLastUpdateIsNull() {
        // Given
        String promptId = "prompt-123";
        String jobId = "job-456";
        String pmlFile = "/path/to/prompt.pml";
        String status = "PENDING";
        LocalDateTime createdAt = LocalDateTime.now();

        // When & Then
        assertThatThrownBy(() -> new Prompt(promptId, jobId, pmlFile, status, createdAt, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Last update cannot be null");
    }

    @Test
    void shouldCreatePromptWithUpdatedStatus() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        Prompt originalPrompt = new Prompt(
            "prompt-123",
            "job-456",
            "/path/to/prompt.pml",
            "PENDING",
            createdAt,
            LocalDateTime.now()
        );
        String newStatus = "COMPLETED";

        // When
        Prompt updatedPrompt = originalPrompt.withStatus(newStatus);

        // Then
        assertThat(updatedPrompt.status()).isEqualTo(newStatus);
        assertThat(updatedPrompt.promptId()).isEqualTo(originalPrompt.promptId());
        assertThat(updatedPrompt.jobId()).isEqualTo(originalPrompt.jobId());
        assertThat(updatedPrompt.pmlFile()).isEqualTo(originalPrompt.pmlFile());
        assertThat(updatedPrompt.createdAt()).isEqualTo(originalPrompt.createdAt());
        assertThat(updatedPrompt.lastUpdate()).isAfter(originalPrompt.lastUpdate());
    }

    @Test
    void shouldCreatePromptWithUpdatedPmlFile() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        Prompt originalPrompt = new Prompt(
            "prompt-123",
            "job-456",
            "/path/to/original.pml",
            "PENDING",
            createdAt,
            LocalDateTime.now()
        );
        String newPmlFile = "/path/to/updated.pml";

        // When
        Prompt updatedPrompt = originalPrompt.withPmlFile(newPmlFile);

        // Then
        assertThat(updatedPrompt.pmlFile()).isEqualTo(newPmlFile);
        assertThat(updatedPrompt.promptId()).isEqualTo(originalPrompt.promptId());
        assertThat(updatedPrompt.jobId()).isEqualTo(originalPrompt.jobId());
        assertThat(updatedPrompt.status()).isEqualTo(originalPrompt.status());
        assertThat(updatedPrompt.createdAt()).isEqualTo(originalPrompt.createdAt());
        assertThat(updatedPrompt.lastUpdate()).isAfter(originalPrompt.lastUpdate());
    }

    @Test
    void shouldHaveProperEquality() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        Prompt prompt1 = new Prompt("id", "job", "file.pml", "PENDING", timestamp, timestamp);
        Prompt prompt2 = new Prompt("id", "job", "file.pml", "PENDING", timestamp, timestamp);
        Prompt prompt3 = new Prompt("id2", "job", "file.pml", "PENDING", timestamp, timestamp);

        // When & Then
        assertThat(prompt1).isEqualTo(prompt2);
        assertThat(prompt1).isNotEqualTo(prompt3);
        assertThat(prompt1.hashCode()).isEqualTo(prompt2.hashCode());
    }

    @Test
    void shouldHaveProperToString() {
        // Given
        LocalDateTime timestamp = LocalDateTime.of(2024, 10, 11, 14, 30);
        Prompt prompt = new Prompt(
            "prompt-123",
            "job-456",
            "/path/to/prompt.pml",
            "PENDING",
            timestamp,
            timestamp
        );

        // When
        String toString = prompt.toString();

        // Then
        assertThat(toString).contains("prompt-123");
        assertThat(toString).contains("job-456");
        assertThat(toString).contains("/path/to/prompt.pml");
        assertThat(toString).contains("PENDING");
    }

    @Test
    void shouldHandleDifferentStatuses() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        String[] statuses = {"PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"};

        // When & Then
        for (String status : statuses) {
            Prompt prompt = new Prompt("id", "job", "file.pml", status, timestamp, timestamp);
            assertThat(prompt.status()).isEqualTo(status);
        }
    }

    @Test
    void shouldHandleDifferentPmlFileFormats() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        String[] files = {
            "/path/to/prompt.pml",
            "relative/path/prompt.pml",
            "./prompt.pml",
            "../parent/prompt.pml"
        };

        // When & Then
        for (String file : files) {
            Prompt prompt = new Prompt("id", "job", file, "PENDING", timestamp, timestamp);
            assertThat(prompt.pmlFile()).isEqualTo(file);
        }
    }

    @Test
    void shouldPreservePromptIdWhenUpdating() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        Prompt original = new Prompt("prompt-123", "job-456", "file.pml", "PENDING", timestamp, timestamp);

        // When
        Prompt withStatus = original.withStatus("COMPLETED");
        Prompt withFile = original.withPmlFile("newfile.pml");

        // Then
        assertThat(withStatus.promptId()).isEqualTo(original.promptId());
        assertThat(withFile.promptId()).isEqualTo(original.promptId());
    }

    @Test
    void shouldPreserveJobIdWhenUpdating() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        Prompt original = new Prompt("prompt-123", "job-456", "file.pml", "PENDING", timestamp, timestamp);

        // When
        Prompt withStatus = original.withStatus("COMPLETED");
        Prompt withFile = original.withPmlFile("newfile.pml");

        // Then
        assertThat(withStatus.jobId()).isEqualTo(original.jobId());
        assertThat(withFile.jobId()).isEqualTo(original.jobId());
    }

    @Test
    void shouldPreserveCreatedAtWhenUpdating() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now().minusDays(1);
        Prompt original = new Prompt("prompt-123", "job-456", "file.pml", "PENDING", timestamp, timestamp);

        // When
        Prompt withStatus = original.withStatus("COMPLETED");
        Prompt withFile = original.withPmlFile("newfile.pml");

        // Then
        assertThat(withStatus.createdAt()).isEqualTo(original.createdAt());
        assertThat(withFile.createdAt()).isEqualTo(original.createdAt());
    }
}

