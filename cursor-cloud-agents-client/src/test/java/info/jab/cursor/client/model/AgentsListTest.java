package info.jab.cursor.client.model;

import info.jab.cursor.generated.client.model.ListAgents200Response;
import info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AgentsList model class.
 */
@DisplayName("AgentsList Model Tests")
class AgentsListTest {

    @Nested
    @DisplayName("from(ListAgents200Response) Tests")
    class FromListAgents200ResponseTests {

        @Test
        @DisplayName("Should return null when generated is null")
        void should_returnNull_when_generatedIsNull() {
            // When
            AgentsList result = AgentsList.from(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should create AgentsList with agents when agents list is provided")
        void should_createAgentsList_when_agentsListIsProvided() {
            // Given
            ListAgents200Response generated = new ListAgents200Response();
            
            ListAgents200ResponseAgentsInner agent1 = new ListAgents200ResponseAgentsInner();
            agent1.setId("agent-1");
            agent1.setName("Agent 1");
            agent1.setStatus(ListAgents200ResponseAgentsInner.StatusEnum.CREATING);
            agent1.setCreatedAt(OffsetDateTime.now());

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource source1 = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerSource();
            source1.setRepository("https://github.com/user/repo1");
            source1.setRef("main");
            agent1.setSource(source1);

            info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget target1 = 
                new info.jab.cursor.generated.client.model.ListAgents200ResponseAgentsInnerTarget();
            target1.setUrl("https://github.com/user/repo1");
            target1.setBranchName("main");
            agent1.setTarget(target1);

            List<ListAgents200ResponseAgentsInner> agents = new ArrayList<>();
            agents.add(agent1);
            generated.setAgents(agents);
            generated.setNextCursor("next-cursor");

            // When
            AgentsList result = AgentsList.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.agents()).hasSize(1);
            assertThat(result.agents().get(0).id()).isEqualTo("agent-1");
            assertThat(result.nextCursor()).isEqualTo("next-cursor");
        }

        @Test
        @DisplayName("Should create AgentsList with empty list when agents list is empty")
        void should_createAgentsListWithEmptyList_when_agentsListIsEmpty() {
            // Given
            ListAgents200Response generated = new ListAgents200Response();
            generated.setAgents(new ArrayList<>());
            generated.setNextCursor("next-cursor");

            // When
            AgentsList result = AgentsList.from(generated);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.agents()).isNotNull();
            assertThat(result.agents()).isEmpty();
            assertThat(result.nextCursor()).isEqualTo("next-cursor");
        }
    }
}

