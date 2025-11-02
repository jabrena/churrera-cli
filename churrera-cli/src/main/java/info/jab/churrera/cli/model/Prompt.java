package info.jab.churrera.cli.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a prompt in the churrera system.
 * Each prompt is linked to a job and contains PML file details.
 */
public record Prompt(
    String promptId,
    String jobId,
    String pmlFile,
    String status,
    LocalDateTime createdAt,
    LocalDateTime lastUpdate
) {

    /**
     * Creates a new prompt with the given parameters.
     *
     * @param promptId the unique prompt identifier (UUID)
     * @param jobId the job ID this prompt belongs to
     * @param pmlFile the path to the PML prompt file
     * @param status the current status of the prompt
     * @param createdAt when the prompt was created
     * @param lastUpdate when the prompt was last updated
     */
    public Prompt {
        Objects.requireNonNull(promptId, "Prompt ID cannot be null");
        Objects.requireNonNull(jobId, "Job ID cannot be null");
        Objects.requireNonNull(pmlFile, "PML file cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(lastUpdate, "Last update cannot be null");
    }

    /**
     * Creates a new prompt with updated status and lastUpdate timestamp.
     *
     * @param newStatus the new status
     * @return a new Prompt instance with updated status and timestamp
     */
    public Prompt withStatus(String newStatus) {
        return new Prompt(promptId, jobId, pmlFile, newStatus, createdAt, LocalDateTime.now());
    }

    /**
     * Creates a new prompt with updated PML file and lastUpdate timestamp.
     *
     * @param newPmlFile the new PML file path
     * @return a new Prompt instance with updated PML file and timestamp
     */
    public Prompt withPmlFile(String newPmlFile) {
        return new Prompt(promptId, jobId, newPmlFile, status, createdAt, LocalDateTime.now());
    }
}
