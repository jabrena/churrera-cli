package info.jab.cursor.client.model;

import info.jab.cursor.generated.client.model.CreateAgent201Response;
import info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AgentResponse model class.
 */
@DisplayName("AgentResponse Model Tests")
class AgentResponseTest {

    @Nested
    @DisplayName("from(CreateAgent201Response) Tests")
    class FromCreateAgent201ResponseTests {

        @Test
        @DisplayName("Should return null when generated is null")
        void should_returnNull_when_generatedIsNull() {
            // When
            AgentResponse result = AgentResponse.from((CreateAgent201Response) null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should create AgentResponse when generated is provided")
        void should_createAgentResponse_when_generatedIsProvided() {
            // Given
            CreateAgent201Response generated = new CreateAgent201Response();
            generated.setId("test-id");
            generated.setName("test-name");
            generated.setStatus(CreateAgent201Response.StatusEnum.CREATING);
            generated.setCreatedAt(OffsetDateTime.now());

            // Mock Source and Target
            info.jab.cursor.generated.client.model.CreateAgent201ResponseSource source = 
                new info.jab.cursor.generated.client.model.CreateAgent201ResponseSource();
            source.setRepository("https://github.com/user/repo");
            source.setRef("main");
            generated.setSource(source);

            info.jab.cursor.generated.client.model.CreateAgent201ResponseTarget target = 
                new info.jab.cursor.generated.client.model.CreateAgent201ResponseTarget();
            target.setUrl("https://github.com/user/repo");
            target.setBranchName("main");
            generated.setTarget(target);

            // When
            AgentResponse result = AgentResponse.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("test-id");
            assertThat(result.name()).isEqualTo("test-name");
            assertThat(result.status()).isEqualTo(AgentStatus.CREATING);
        }
    }

    @Nested
    @DisplayName("from(ListAgents200ResponseAgentsInner) Tests")
    class FromListAgents200ResponseAgentsInnerTests {

        @Test
        @DisplayName("Should return null when generated is null")
        void should_returnNull_when_generatedIsNull() {
            // When
            AgentResponse result = AgentResponse.from((ListAgents200ResponseAgentsInner) null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should create AgentResponse when generated is provided")
        void should_createAgentResponse_when_generatedIsProvided() {
            // Given
            ListAgents200ResponseAgentsInner generated = new ListAgents200ResponseAgentsInner();
            generated.setId("test-id");
            generated.setName("test-name");
            generated.setStatus(ListAgents200ResponseAgentsInner.StatusEnum.CREATING);
            generated.setCreatedAt(OffsetDateTime.now());

            // Mock Source and Target
            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource source = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource();
            source.setRepository("https://github.com/user/repo");
            source.setRef("main");
            generated.setSource(source);

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget target = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget();
            target.setUrl("https://github.com/user/repo");
            target.setBranchName("main");
            generated.setTarget(target);

            // When
            AgentResponse result = AgentResponse.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("test-id");
            assertThat(result.name()).isEqualTo("test-name");
            assertThat(result.status()).isEqualTo(AgentStatus.CREATING);
        }
    }

    @Nested
    @DisplayName("mapStatusEnum Tests")
    class MapStatusEnumTests {

        @Test
        @DisplayName("Should map CreateAgent201Response.StatusEnum.CREATING to CREATING")
        void should_mapCreatingStatus_when_createAgentStatusIsCreating() {
            // Given
            CreateAgent201Response generated = new CreateAgent201Response();
            generated.setId("test-id");
            generated.setName("test-name");
            generated.setStatus(CreateAgent201Response.StatusEnum.CREATING);
            generated.setCreatedAt(OffsetDateTime.now());

            info.jab.cursor.generated.client.model.CreateAgent201ResponseSource source = 
                new info.jab.cursor.generated.client.model.CreateAgent201ResponseSource();
            source.setRepository("https://github.com/user/repo");
            source.setRef("main");
            generated.setSource(source);

            info.jab.cursor.generated.client.model.CreateAgent201ResponseTarget target = 
                new info.jab.cursor.generated.client.model.CreateAgent201ResponseTarget();
            target.setUrl("https://github.com/user/repo");
            target.setBranchName("main");
            generated.setTarget(target);

            // When
            AgentResponse result = AgentResponse.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(AgentStatus.CREATING);
        }


        @Test
        @DisplayName("Should map ListAgents200ResponseAgentsInner.StatusEnum.CREATING to CREATING")
        void should_mapCreatingStatus_when_listAgentsStatusIsCreating() {
            // Given
            ListAgents200ResponseAgentsInner generated = new ListAgents200ResponseAgentsInner();
            generated.setId("test-id");
            generated.setName("test-name");
            generated.setStatus(ListAgents200ResponseAgentsInner.StatusEnum.CREATING);
            generated.setCreatedAt(OffsetDateTime.now());

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource source = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource();
            source.setRepository("https://github.com/user/repo");
            source.setRef("main");
            generated.setSource(source);

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget target = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget();
            target.setUrl("https://github.com/user/repo");
            target.setBranchName("main");
            generated.setTarget(target);

            // When
            AgentResponse result = AgentResponse.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(AgentStatus.CREATING);
        }

        @Test
        @DisplayName("Should map ListAgents200ResponseAgentsInner.StatusEnum.RUNNING to RUNNING")
        void should_mapRunningStatus_when_listAgentsStatusIsRunning() {
            // Given
            ListAgents200ResponseAgentsInner generated = new ListAgents200ResponseAgentsInner();
            generated.setId("test-id");
            generated.setName("test-name");
            generated.setStatus(ListAgents200ResponseAgentsInner.StatusEnum.RUNNING);
            generated.setCreatedAt(OffsetDateTime.now());

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource source = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource();
            source.setRepository("https://github.com/user/repo");
            source.setRef("main");
            generated.setSource(source);

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget target = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget();
            target.setUrl("https://github.com/user/repo");
            target.setBranchName("main");
            generated.setTarget(target);

            // When
            AgentResponse result = AgentResponse.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(AgentStatus.RUNNING);
        }

        @Test
        @DisplayName("Should map ListAgents200ResponseAgentsInner.StatusEnum.FINISHED to FINISHED")
        void should_mapFinishedStatus_when_listAgentsStatusIsFinished() {
            // Given
            ListAgents200ResponseAgentsInner generated = new ListAgents200ResponseAgentsInner();
            generated.setId("test-id");
            generated.setName("test-name");
            generated.setStatus(ListAgents200ResponseAgentsInner.StatusEnum.FINISHED);
            generated.setCreatedAt(OffsetDateTime.now());

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource source = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource();
            source.setRepository("https://github.com/user/repo");
            source.setRef("main");
            generated.setSource(source);

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget target = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget();
            target.setUrl("https://github.com/user/repo");
            target.setBranchName("main");
            generated.setTarget(target);

            // When
            AgentResponse result = AgentResponse.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(AgentStatus.FINISHED);
        }

        @Test
        @DisplayName("Should map ListAgents200ResponseAgentsInner.StatusEnum.ERROR to ERROR")
        void should_mapErrorStatus_when_listAgentsStatusIsError() {
            // Given
            ListAgents200ResponseAgentsInner generated = new ListAgents200ResponseAgentsInner();
            generated.setId("test-id");
            generated.setName("test-name");
            generated.setStatus(ListAgents200ResponseAgentsInner.StatusEnum.ERROR);
            generated.setCreatedAt(OffsetDateTime.now());

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource source = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource();
            source.setRepository("https://github.com/user/repo");
            source.setRef("main");
            generated.setSource(source);

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget target = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget();
            target.setUrl("https://github.com/user/repo");
            target.setBranchName("main");
            generated.setTarget(target);

            // When
            AgentResponse result = AgentResponse.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(AgentStatus.ERROR);
        }

        @Test
        @DisplayName("Should map ListAgents200ResponseAgentsInner.StatusEnum.EXPIRED to EXPIRED")
        void should_mapExpiredStatus_when_listAgentsStatusIsExpired() {
            // Given
            ListAgents200ResponseAgentsInner generated = new ListAgents200ResponseAgentsInner();
            generated.setId("test-id");
            generated.setName("test-name");
            generated.setStatus(ListAgents200ResponseAgentsInner.StatusEnum.EXPIRED);
            generated.setCreatedAt(OffsetDateTime.now());

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource source = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource();
            source.setRepository("https://github.com/user/repo");
            source.setRef("main");
            generated.setSource(source);

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget target = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget();
            target.setUrl("https://github.com/user/repo");
            target.setBranchName("main");
            generated.setTarget(target);

            // When
            AgentResponse result = AgentResponse.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(AgentStatus.EXPIRED);
        }

    }
}

