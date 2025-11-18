package info.jab.cursor.client.model;

import java.net.URI;
import java.util.Objects;

import info.jab.cursor.generated.client.model.CreateAgent201ResponseSource;
import info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource;

/**
 * Domain model for agent source configuration.
 */
public record Source(
    URI repository,
    String ref
) {
    // Compact record constructor
    public Source {
        Objects.requireNonNull(repository, "Repository cannot be null");
    }

    /**
     * Factory method to create Source from CreateAgent201ResponseSource.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static Source from(CreateAgent201ResponseSource generated) {
        return new Source(
            URI.create(generated.getRepository()), //Required field
            generated.getRef()
        );
    }

    /**
     * Factory method to create Source from ListAgents200ResponseAgentsInnerSource.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static Source from(ListAgents200ResponseAgentsInnerSource generated) {
        return new Source(
            URI.create(generated.getRepository()),
            generated.getRef()
        );
    }
}

