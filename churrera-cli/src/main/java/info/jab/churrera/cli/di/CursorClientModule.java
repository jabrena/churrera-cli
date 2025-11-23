package info.jab.churrera.cli.di;

import info.jab.cursor.client.CursorAgentGeneralEndpoints;
import info.jab.cursor.client.CursorAgentInformation;
import info.jab.cursor.client.CursorAgentManagement;
import info.jab.cursor.client.impl.CursorAgentGeneralEndpointsImpl;
import info.jab.cursor.client.impl.CursorAgentInformationImpl;
import info.jab.cursor.client.impl.CursorAgentManagementImpl;
import info.jab.cursor.generated.client.ApiClient;
import info.jab.cursor.generated.client.api.DefaultApi;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Dagger module providing Cursor API client dependencies.
 */
@Module
public class CursorClientModule {

    private static final String API_BASE_URL = "https://api.cursor.com";

    @Provides
    @Singleton
    ApiClient provideApiClient() {
        ApiClient client = new ApiClient();
        client.updateBaseUri(API_BASE_URL);
        return client;
    }

    @Provides
    @Singleton
    DefaultApi provideDefaultApi(ApiClient apiClient) {
        return new DefaultApi(apiClient);
    }

    @Provides
    @Singleton
    CursorAgentManagement provideCursorAgentManagement(
            String apiKey,
            DefaultApi defaultApi) {
        return new CursorAgentManagementImpl(apiKey, defaultApi);
    }

    @Provides
    @Singleton
    CursorAgentInformation provideCursorAgentInformation(
            String apiKey,
            DefaultApi defaultApi) {
        return new CursorAgentInformationImpl(apiKey, defaultApi);
    }

    @Provides
    @Singleton
    CursorAgentGeneralEndpoints provideCursorAgentGeneralEndpoints(
            String apiKey,
            DefaultApi defaultApi) {
        return new CursorAgentGeneralEndpointsImpl(apiKey, defaultApi);
    }
}
