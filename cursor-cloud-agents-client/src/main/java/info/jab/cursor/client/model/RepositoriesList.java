package info.jab.cursor.client.model;

import java.util.List;
import java.util.Objects;

/**
 * Domain model for a list of repositories.
 */
public record RepositoriesList(
    List<Repository> repositories
) {
    public RepositoriesList {
        Objects.requireNonNull(repositories, "Repositories cannot be null");
    }


    /**
     * Factory method to create RepositoriesList from ListRepositories200Response.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static RepositoriesList from(info.jab.cursor.generated.client.model.ListRepositories200Response generated) {
        if (generated == null) {
            return null;
        }
        List<Repository> repositories = generated.getRepositories() != null
            ? generated.getRepositories().stream()
                .map(Repository::from)
                .toList()
            : List.of();
        return new RepositoriesList(repositories);
    }
}

