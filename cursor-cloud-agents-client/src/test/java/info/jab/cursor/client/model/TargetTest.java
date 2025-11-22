package info.jab.cursor.client.model;

import info.jab.cursor.generated.client.model.CreateAgent201ResponseTarget;
import info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Target model class.
 */
@DisplayName("Target Model Tests")
class TargetTest {

    @Nested
    @DisplayName("from(CreateAgent201ResponseTarget) Tests")
    class FromCreateAgent201ResponseTargetTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when generated is null")
        void should_throwIllegalArgumentException_when_generatedIsNull() {
            // When & Then
            assertThatThrownBy(() -> Target.from((CreateAgent201ResponseTarget) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target generated cannot be null");
        }

        @Test
        @DisplayName("Should create Target with all fields when all fields are provided")
        void should_createTarget_when_allFieldsProvided() {
            // Given
            CreateAgent201ResponseTarget generated = new CreateAgent201ResponseTarget();
            generated.setBranchName("main");
            generated.setUrl("https://github.com/user/repo");
            generated.setAutoCreatePr(true);
            generated.setOpenAsCursorGithubApp(true);
            generated.setSkipReviewerRequest(false);

            // When
            Target target = Target.from(generated);

            // Then
            assertThat(target).isNotNull();
            assertThat(target.branchName()).isEqualTo("main");
            assertThat(target.url()).isEqualTo(URI.create("https://github.com/user/repo"));
            assertThat(target.autoCreatePr()).isTrue();
            assertThat(target.openAsCursorGithubApp()).isTrue();
            assertThat(target.skipReviewerRequest()).isFalse();
        }

        @Test
        @DisplayName("Should create Target with default values when nullable fields are null")
        void should_createTargetWithDefaults_when_nullableFieldsAreNull() {
            // Given
            CreateAgent201ResponseTarget generated = new CreateAgent201ResponseTarget();
            generated.setBranchName(null);
            generated.setUrl("https://github.com/user/repo");
            generated.setAutoCreatePr(null);
            generated.setOpenAsCursorGithubApp(null);
            generated.setSkipReviewerRequest(null);

            // When
            Target target = Target.from(generated);

            // Then
            assertThat(target).isNotNull();
            assertThat(target.branchName()).isEqualTo("");
            assertThat(target.url()).isEqualTo(URI.create("https://github.com/user/repo"));
            assertThat(target.autoCreatePr()).isFalse();
            assertThat(target.openAsCursorGithubApp()).isFalse();
            assertThat(target.skipReviewerRequest()).isFalse();
        }

        @Test
        @DisplayName("Should create Target with empty branchName when branchName is null")
        void should_createTargetWithEmptyBranchName_when_branchNameIsNull() {
            // Given
            CreateAgent201ResponseTarget generated = new CreateAgent201ResponseTarget();
            generated.setBranchName(null);
            generated.setUrl("https://github.com/user/repo");
            generated.setAutoCreatePr(true);
            generated.setOpenAsCursorGithubApp(false);
            generated.setSkipReviewerRequest(true);

            // When
            Target target = Target.from(generated);

            // Then
            assertThat(target).isNotNull();
            assertThat(target.branchName()).isEqualTo("");
        }

        @Test
        @DisplayName("Should create Target with default autoCreatePr when autoCreatePr is null")
        void should_createTargetWithDefaultAutoCreatePr_when_autoCreatePrIsNull() {
            // Given
            CreateAgent201ResponseTarget generated = new CreateAgent201ResponseTarget();
            generated.setBranchName("main");
            generated.setUrl("https://github.com/user/repo");
            generated.setAutoCreatePr(null);
            generated.setOpenAsCursorGithubApp(true);
            generated.setSkipReviewerRequest(false);

            // When
            Target target = Target.from(generated);

            // Then
            assertThat(target).isNotNull();
            assertThat(target.autoCreatePr()).isFalse();
        }

        @Test
        @DisplayName("Should create Target with default openAsCursorGithubApp when openAsCursorGithubApp is null")
        void should_createTargetWithDefaultOpenAsCursorGithubApp_when_openAsCursorGithubAppIsNull() {
            // Given
            CreateAgent201ResponseTarget generated = new CreateAgent201ResponseTarget();
            generated.setBranchName("main");
            generated.setUrl("https://github.com/user/repo");
            generated.setAutoCreatePr(true);
            generated.setOpenAsCursorGithubApp(null);
            generated.setSkipReviewerRequest(false);

            // When
            Target target = Target.from(generated);

            // Then
            assertThat(target).isNotNull();
            assertThat(target.openAsCursorGithubApp()).isFalse();
        }

        @Test
        @DisplayName("Should create Target with default skipReviewerRequest when skipReviewerRequest is null")
        void should_createTargetWithDefaultSkipReviewerRequest_when_skipReviewerRequestIsNull() {
            // Given
            CreateAgent201ResponseTarget generated = new CreateAgent201ResponseTarget();
            generated.setBranchName("main");
            generated.setUrl("https://github.com/user/repo");
            generated.setAutoCreatePr(true);
            generated.setOpenAsCursorGithubApp(false);
            generated.setSkipReviewerRequest(null);

            // When
            Target target = Target.from(generated);

            // Then
            assertThat(target).isNotNull();
            assertThat(target.skipReviewerRequest()).isFalse();
        }
    }

    @Nested
    @DisplayName("from(ListAgents200ResponseAgentsInnerTarget) Tests")
    class FromListAgents200ResponseAgentsInnerTargetTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when generated is null")
        void should_throwIllegalArgumentException_when_generatedIsNull() {
            // When & Then
            assertThatThrownBy(() -> Target.from((ListAgents200ResponseAgentsInnerTarget) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target generated cannot be null");
        }

        @Test
        @DisplayName("Should create Target with all fields when all fields are provided")
        void should_createTarget_when_allFieldsProvided() {
            // Given
            ListAgents200ResponseAgentsInnerTarget generated = new ListAgents200ResponseAgentsInnerTarget();
            generated.setBranchName("main");
            generated.setUrl("https://github.com/user/repo");
            generated.setAutoCreatePr(true);
            generated.setOpenAsCursorGithubApp(true);
            generated.setSkipReviewerRequest(false);

            // When
            Target target = Target.from(generated);

            // Then
            assertThat(target).isNotNull();
            assertThat(target.branchName()).isEqualTo("main");
            assertThat(target.url()).isEqualTo(URI.create("https://github.com/user/repo"));
            assertThat(target.autoCreatePr()).isTrue();
            assertThat(target.openAsCursorGithubApp()).isTrue();
            assertThat(target.skipReviewerRequest()).isFalse();
        }

        @Test
        @DisplayName("Should create Target with empty branchName when branchName is null")
        void should_createTargetWithEmptyBranchName_when_branchNameIsNull() {
            // Given
            ListAgents200ResponseAgentsInnerTarget generated = new ListAgents200ResponseAgentsInnerTarget();
            generated.setBranchName(null);
            generated.setUrl("https://github.com/user/repo");
            generated.setAutoCreatePr(true);
            generated.setOpenAsCursorGithubApp(false);
            generated.setSkipReviewerRequest(true);

            // When
            Target target = Target.from(generated);

            // Then
            assertThat(target).isNotNull();
            assertThat(target.branchName()).isEqualTo("");
        }

        @Test
        @DisplayName("Should create Target with default autoCreatePr when autoCreatePr is null")
        void should_createTargetWithDefaultAutoCreatePr_when_autoCreatePrIsNull() {
            // Given
            ListAgents200ResponseAgentsInnerTarget generated = new ListAgents200ResponseAgentsInnerTarget();
            generated.setBranchName("main");
            generated.setUrl("https://github.com/user/repo");
            generated.setAutoCreatePr(null);
            generated.setOpenAsCursorGithubApp(true);
            generated.setSkipReviewerRequest(false);

            // When
            Target target = Target.from(generated);

            // Then
            assertThat(target).isNotNull();
            assertThat(target.autoCreatePr()).isFalse();
        }
    }
}

