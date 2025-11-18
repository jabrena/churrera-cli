package info.jab.cursor.client.model;

import java.net.URI;
import java.util.Objects;

import info.jab.cursor.generated.client.model.CreateAgent201ResponseTarget;
import info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget;

/**
 * Domain model for agent target configuration.
 */
public record Target(
    String branchName,
    URI url,
    boolean autoCreatePr,
    boolean openAsCursorGithubApp,
    boolean skipReviewerRequest
) {
    // Compact record constructor
    public Target {
        Objects.requireNonNull(branchName, "Branch name cannot be null");
        Objects.requireNonNull(url, "URL cannot be null");
    }

    /**
     * Factory method to create Target from CreateAgent201ResponseTarget.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static Target from(CreateAgent201ResponseTarget generated) {
        // Preconditions
        if (generated == null) {
            throw new IllegalArgumentException("Target generated cannot be null");
        }
        return new Target(
            generated.getBranchName() != null ? generated.getBranchName() : "",
            URI.create(generated.getUrl()), //Required field
            generated.getAutoCreatePr() != null ? generated.getAutoCreatePr() : false,
            generated.getOpenAsCursorGithubApp() != null ? generated.getOpenAsCursorGithubApp() : false,
            generated.getSkipReviewerRequest() != null ? generated.getSkipReviewerRequest() : false
        );
    }

    /**
     * Factory method to create Target from ListAgents200ResponseAgentsInnerTarget.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static Target from(ListAgents200ResponseAgentsInnerTarget generated) {
        // Preconditions
        if (generated == null) {
            throw new IllegalArgumentException("Target generated cannot be null");
        }
        return new Target(
            generated.getBranchName() != null ? generated.getBranchName() : "",
            URI.create(generated.getUrl()), //Required field
            generated.getAutoCreatePr() != null ? generated.getAutoCreatePr() : false,
            generated.getOpenAsCursorGithubApp() != null ? generated.getOpenAsCursorGithubApp() : false,
            generated.getSkipReviewerRequest() != null ? generated.getSkipReviewerRequest() : false
        );
    }
}

