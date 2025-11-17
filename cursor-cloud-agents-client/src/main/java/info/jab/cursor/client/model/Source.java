package info.jab.cursor.client.model;

import java.net.URI;
import java.util.Objects;

/**
 * Domain model for agent source configuration.
 */
public record Source(
    URI repository,
    String ref
) {
    public Source {
        Objects.requireNonNull(repository, "Repository cannot be null");
    }


    /**
     * Factory method to create Source from CreateAgent201ResponseSource.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static Source from(info.jab.cursor.generated.client.model.CreateAgent201ResponseSource generated) {
        if (generated == null) {
            return null;
        }
        return new Source(
            URI.create(generated.getRepository()),
            generated.getRef()
        );
    }

    /**
     * Factory method to create Source from ListAgents200ResponseAgentsInnerSource.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static Source from(info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource generated) {
        if (generated == null) {
            return null;
        }
        String repoStr = generated.getRepository();
        if (repoStr == null) {
            throw new IllegalArgumentException("Repository cannot be null");
        }
        return new Source(
            URI.create(repoStr),
            generated.getRef()
        );
    }
}

