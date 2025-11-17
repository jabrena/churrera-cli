package info.jab.cursor.client.model;

import java.net.URI;
import java.util.Objects;

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
    public static Target from(info.jab.cursor.generated.client.model.CreateAgent201ResponseTarget generated) {
        if (generated == null) {
            return null;
        }
        return new Target(
            generated.getBranchName() != null ? generated.getBranchName() : "",
            URI.create(generated.getUrl()),
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
    public static Target from(info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget generated) {
        if (generated == null) {
            return null;
        }
        return new Target(
            generated.getBranchName() != null ? generated.getBranchName() : "",
            URI.create(generated.getUrl()),
            generated.getAutoCreatePr() != null ? generated.getAutoCreatePr() : false,
            generated.getOpenAsCursorGithubApp() != null ? generated.getOpenAsCursorGithubApp() : false,
            generated.getSkipReviewerRequest() != null ? generated.getSkipReviewerRequest() : false
        );
    }
}

