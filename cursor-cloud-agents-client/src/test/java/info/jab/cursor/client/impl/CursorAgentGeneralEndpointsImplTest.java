package info.jab.cursor.client.impl;

import info.jab.cursor.client.model.ApiKeyInfo;
import info.jab.cursor.generated.client.api.DefaultApi;
import info.jab.cursor.generated.client.ApiException;
import info.jab.cursor.generated.client.model.GetMe200Response;
import info.jab.cursor.generated.client.model.ListModels200Response;
import info.jab.cursor.generated.client.model.ListRepositories200Response;
import info.jab.cursor.generated.client.model.ListRepositories200ResponseRepositoriesInner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CursorAgentGeneralEndpointsImpl class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CursorAgentGeneralEndpointsImpl Tests")
class CursorAgentGeneralEndpointsImplTest {

    @Mock
    private DefaultApi defaultApi;

    private static final String TEST_API_KEY = "test-api-key";

    @Nested
    @DisplayName("getApiKeyInfo() Tests")
    class GetApiKeyInfoTests {

        @Test
        @DisplayName("Should return ApiKeyInfo when API call succeeds")
        void should_returnApiKeyInfo_when_apiCallSucceeds() throws ApiException {
            // Given
            CursorAgentGeneralEndpointsImpl impl = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
            GetMe200Response response = new GetMe200Response();
            response.setApiKeyName("API_KEY_TOKEN_V2");
            response.setUserEmail("test@example.com");
            response.setCreatedAt(OffsetDateTime.now());
            when(defaultApi.getMe(any())).thenReturn(response);

            // When
            ApiKeyInfo result = impl.getApiKeyInfo();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.apiKeyName()).isEqualTo("API_KEY_TOKEN_V2");
            assertThat(result.userEmail()).isEqualTo("test@example.com");
            assertThat(result.createdAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
            CursorAgentGeneralEndpointsImpl impl = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
            ApiException apiException = new ApiException(500, "Internal Server Error");
            when(defaultApi.getMe(any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(impl::getApiKeyInfo)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get API key info")
                .hasCause(apiException);
        }
    }

    @Nested
    @DisplayName("getModels() Tests")
    class GetModelsTests {

        @Test
        @DisplayName("Should return list of models when API call succeeds")
        void should_returnListOfModels_when_apiCallSucceeds() throws ApiException {
            // Given
            CursorAgentGeneralEndpointsImpl impl = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
            ListModels200Response response = new ListModels200Response();
            response.setModels(List.of("model1", "model2", "model3"));
            when(defaultApi.listModels(any())).thenReturn(response);

            // When
            List<String> result = impl.getModels();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly("model1", "model2", "model3");
        }

        @Test
        @DisplayName("Should return empty list when models list is null")
        void should_returnEmptyList_when_modelsListIsNull() throws ApiException {
            // Given
            CursorAgentGeneralEndpointsImpl impl = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
            ListModels200Response response = new ListModels200Response();
            response.setModels(null);
            when(defaultApi.listModels(any())).thenReturn(response);

            // When
            List<String> result = impl.getModels();

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
            CursorAgentGeneralEndpointsImpl impl = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
            ApiException apiException = new ApiException(500, "Internal Server Error");
            when(defaultApi.listModels(any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(impl::getModels)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get models")
                .hasCause(apiException);
        }
    }

    @Nested
    @DisplayName("getRepositories() Tests")
    class GetRepositoriesTests {

        @Test
        @DisplayName("Should return empty list when response is null")
        void should_returnEmptyList_when_responseIsNull() throws ApiException {
            // Given
            CursorAgentGeneralEndpointsImpl impl = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
            when(defaultApi.listRepositories(any())).thenReturn(null);

            // When
            List<String> result = impl.getRepositories();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when repositories list is null")
        void should_returnEmptyList_when_repositoriesListIsNull() throws ApiException {
            // Given
            CursorAgentGeneralEndpointsImpl impl = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
            ListRepositories200Response response = new ListRepositories200Response();
            response.setRepositories(null);
            when(defaultApi.listRepositories(any())).thenReturn(response);

            // When
            List<String> result = impl.getRepositories();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should filter out null repositories")
        void should_filterOutNullRepositories() throws ApiException {
            // Given
            CursorAgentGeneralEndpointsImpl impl = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
            ListRepositories200Response response = new ListRepositories200Response();

            ListRepositories200ResponseRepositoriesInner repo1 = new ListRepositories200ResponseRepositoriesInner();
            repo1.setRepository(URI.create("https://github.com/user/repo1"));

            ListRepositories200ResponseRepositoriesInner repo2 = new ListRepositories200ResponseRepositoriesInner();
            repo2.setRepository(null);

            ListRepositories200ResponseRepositoriesInner repo3 = new ListRepositories200ResponseRepositoriesInner();
            repo3.setRepository(URI.create("https://github.com/user/repo3"));

            List<ListRepositories200ResponseRepositoriesInner> repositories = new ArrayList<>();
            repositories.add(repo1);
            repositories.add(repo2);
            repositories.add(repo3);
            response.setRepositories(repositories);

            when(defaultApi.listRepositories(any())).thenReturn(response);

            // When
            List<String> result = impl.getRepositories();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(
                "https://github.com/user/repo1",
                "https://github.com/user/repo3"
            );
        }

        @Test
        @DisplayName("Should throw RuntimeException when ApiException occurs")
        void should_throwRuntimeException_when_apiExceptionOccurs() throws ApiException {
            // Given
            CursorAgentGeneralEndpointsImpl impl = new CursorAgentGeneralEndpointsImpl(TEST_API_KEY, defaultApi);
            ApiException apiException = new ApiException(500, "Internal Server Error");
            when(defaultApi.listRepositories(any())).thenThrow(apiException);

            // When & Then
            assertThatThrownBy(impl::getRepositories)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get repositories")
                .hasCause(apiException);
        }
    }
}

