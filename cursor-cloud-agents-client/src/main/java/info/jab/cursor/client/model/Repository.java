package info.jab.cursor.client.model;

import java.util.Objects;

/**
 * Domain model for a GitHub repository.
 */
public record Repository(
    String owner,
    String name,
    String repository
) {
    public Repository {
        Objects.requireNonNull(owner, "Owner cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(repository, "Repository cannot be null");
    }


    /**
     * Factory method to create Repository from ListRepositories200ResponseRepositoriesInner.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static Repository from(info.jab.cursor.generated.client.model.ListRepositories200ResponseRepositoriesInner generated) {
        if (generated == null) {
            return null;
        }
        return new Repository(
            generated.getOwner(),
            generated.getName(),
            generated.getRepository() != null ? generated.getRepository().toString() : null
        );
    }
}

