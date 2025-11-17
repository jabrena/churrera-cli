package info.jab.cursor.client.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Domain model for API key information.
 */
public record ApiKeyInfo(
    String apiKeyName,
    OffsetDateTime createdAt,
    String userEmail
) {
    public ApiKeyInfo {
        Objects.requireNonNull(apiKeyName, "API key name cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
    }


    /**
     * Factory method to create ApiKeyInfo from GetMe200Response.
     *
     * @param generated the generated OpenAPI model
     * @return domain model instance, or null if input is null
     */
    public static ApiKeyInfo from(info.jab.cursor.generated.client.model.GetMe200Response generated) {
        if (generated == null) {
            return null;
        }
        return new ApiKeyInfo(
            generated.getApiKeyName(),
            generated.getCreatedAt(),
            generated.getUserEmail()
        );
    }
}

