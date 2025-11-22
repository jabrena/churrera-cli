package info.jab.cursor.client.model;

import info.jab.cursor.generated.client.model.GetMe200Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApiKeyInfo model class.
 */
@DisplayName("ApiKeyInfo Model Tests")
class ApiKeyInfoTest {

    @Nested
    @DisplayName("from(GetMe200Response) Tests")
    class FromGetMe200ResponseTests {

        @Test
        @DisplayName("Should return null when generated is null")
        void should_returnNull_when_generatedIsNull() {
            // When
            ApiKeyInfo result = ApiKeyInfo.from(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should create ApiKeyInfo when all fields are provided")
        void should_createApiKeyInfo_when_allFieldsProvided() {
            // Given
            GetMe200Response generated = new GetMe200Response();
            generated.setApiKeyName("test-api-key");
            generated.setCreatedAt(OffsetDateTime.now());
            generated.setUserEmail("test@example.com");

            // When
            ApiKeyInfo result = ApiKeyInfo.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.apiKeyName()).isEqualTo("test-api-key");
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.userEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should create ApiKeyInfo when userEmail is null")
        void should_createApiKeyInfo_when_userEmailIsNull() {
            // Given
            GetMe200Response generated = new GetMe200Response();
            generated.setApiKeyName("test-api-key");
            generated.setCreatedAt(OffsetDateTime.now());
            generated.setUserEmail(null);

            // When
            ApiKeyInfo result = ApiKeyInfo.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.apiKeyName()).isEqualTo("test-api-key");
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.userEmail()).isNull();
        }
    }
}

