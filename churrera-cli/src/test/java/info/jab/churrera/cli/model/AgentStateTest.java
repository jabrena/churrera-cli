package info.jab.churrera.cli.model;

import info.jab.cursor.client.model.AgentResponse;
import info.jab.cursor.client.model.AgentStatus;
import info.jab.cursor.client.model.Source;
import info.jab.cursor.client.model.Target;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AgentState class.
 */
public class AgentStateTest {

    private static AgentResponse createAgentResponse(AgentStatus status) {
        return new AgentResponse(
            "test-id",
            "Test Agent",
            status,
            new Source(URI.create("https://github.com/test/repo"), "main"),
            new Target("cursor/test", URI.create("https://cursor.com/agents?id=test"), false, false, false),
            OffsetDateTime.now()
        );
    }

    @Test
    public void testIsTerminal() {
        // Terminal states
        assertTrue(AgentState.FINISHED().isTerminal());
        assertTrue(AgentState.ERROR().isTerminal());
        assertTrue(AgentState.EXPIRED().isTerminal());

        // Non-terminal states
        assertFalse(AgentState.CREATING().isTerminal());
        assertFalse(AgentState.RUNNING().isTerminal());
    }

    @Test
    public void testIsSuccessful() {
        // Successful states
        assertTrue(AgentState.FINISHED().isSuccessful());

        // Non-successful states
        assertFalse(AgentState.CREATING().isSuccessful());
        assertFalse(AgentState.RUNNING().isSuccessful());
        assertFalse(AgentState.ERROR().isSuccessful());
        assertFalse(AgentState.EXPIRED().isSuccessful());
    }

    @Test
    public void testIsFailed() {
        // Failed states
        assertTrue(AgentState.ERROR().isFailed());
        assertTrue(AgentState.EXPIRED().isFailed());

        // Non-failed states
        assertFalse(AgentState.CREATING().isFailed());
        assertFalse(AgentState.RUNNING().isFailed());
        assertFalse(AgentState.FINISHED().isFailed());
    }

    @Test
    public void testIsActive() {
        // Active states (non-terminal)
        assertTrue(AgentState.CREATING().isActive());
        assertTrue(AgentState.RUNNING().isActive());

        // Inactive states (terminal)
        assertFalse(AgentState.FINISHED().isActive());
        assertFalse(AgentState.ERROR().isActive());
        assertFalse(AgentState.EXPIRED().isActive());
    }

    @Test
    public void testOfWithNullAgent() {
        AgentState result = AgentState.of((AgentResponse) null);
        assertEquals(AgentState.CREATING(), result);
    }

    @Test
    public void testOfWithNullStatus() {
        AgentResponse agent = new AgentResponse(
            "test-id",
            "Test Agent",
            null, // Will default to CREATING in constructor
            new Source(URI.create("https://github.com/test/repo"), "main"),
            new Target("cursor/test", URI.create("https://cursor.com/agents?id=test"), false, false, false),
            OffsetDateTime.now()
        );

        AgentState result = AgentState.of(agent);
        assertEquals(AgentState.CREATING(), result);
    }

    @Test
    public void testOfWithCreatingStatus() {
        AgentResponse agent = createAgentResponse(AgentStatus.CREATING);
        AgentState result = AgentState.of(agent);
        assertEquals(AgentState.CREATING(), result);
        assertEquals(AgentStatus.CREATING, result.getStatus());
    }

    @Test
    public void testOfWithRunningStatus() {
        AgentResponse agent = createAgentResponse(AgentStatus.RUNNING);
        AgentState result = AgentState.of(agent);
        assertEquals(AgentState.RUNNING(), result);
        assertEquals(AgentStatus.RUNNING, result.getStatus());
    }

    @Test
    public void testOfWithFinishedStatus() {
        AgentResponse agent = createAgentResponse(AgentStatus.FINISHED);
        AgentState result = AgentState.of(agent);
        assertEquals(AgentState.FINISHED(), result);
        assertEquals(AgentStatus.FINISHED, result.getStatus());
    }

    @Test
    public void testOfWithErrorStatus() {
        AgentResponse agent = createAgentResponse(AgentStatus.ERROR);
        AgentState result = AgentState.of(agent);
        assertEquals(AgentState.ERROR(), result);
        assertEquals(AgentStatus.ERROR, result.getStatus());
    }

    @Test
    public void testOfWithExpiredStatus() {
        AgentResponse agent = createAgentResponse(AgentStatus.EXPIRED);
        AgentState result = AgentState.of(agent);
        assertEquals(AgentState.EXPIRED(), result);
        assertEquals(AgentStatus.EXPIRED, result.getStatus());
    }

    @Test
    public void testAllValidStatusEnums() {
        // Test all valid status values
        AgentResponse creatingAgent = createAgentResponse(AgentStatus.CREATING);
        assertEquals(AgentState.CREATING(), AgentState.of(creatingAgent));

        AgentResponse runningAgent = createAgentResponse(AgentStatus.RUNNING);
        assertEquals(AgentState.RUNNING(), AgentState.of(runningAgent));

        AgentResponse finishedAgent = createAgentResponse(AgentStatus.FINISHED);
        assertEquals(AgentState.FINISHED(), AgentState.of(finishedAgent));

        AgentResponse errorAgent = createAgentResponse(AgentStatus.ERROR);
        assertEquals(AgentState.ERROR(), AgentState.of(errorAgent));

        AgentResponse expiredAgent = createAgentResponse(AgentStatus.EXPIRED);
        assertEquals(AgentState.EXPIRED(), AgentState.of(expiredAgent));
    }

    @Test
    public void testStateConsistency() {
        // Test that isActive() is the opposite of isTerminal()
        AgentState[] states = {
            AgentState.CREATING(),
            AgentState.RUNNING(),
            AgentState.FINISHED(),
            AgentState.ERROR(),
            AgentState.EXPIRED()
        };

        for (AgentState state : states) {
            assertEquals(!state.isTerminal(), state.isActive());
        }
    }

    @Test
    public void testSuccessfulStatesAreTerminal() {
        // All successful states should be terminal
        AgentState[] states = {
            AgentState.CREATING(),
            AgentState.RUNNING(),
            AgentState.FINISHED(),
            AgentState.ERROR(),
            AgentState.EXPIRED()
        };

        for (AgentState state : states) {
            if (state.isSuccessful()) {
                assertTrue(state.isTerminal(),
                    "Successful state " + state + " should be terminal");
            }
        }
    }

    @Test
    public void testFailedStatesAreTerminal() {
        // All failed states should be terminal
        AgentState[] states = {
            AgentState.CREATING(),
            AgentState.RUNNING(),
            AgentState.FINISHED(),
            AgentState.ERROR(),
            AgentState.EXPIRED()
        };

        for (AgentState state : states) {
            if (state.isFailed()) {
                assertTrue(state.isTerminal(),
                    "Failed state " + state + " should be terminal");
            }
        }
    }

    @Test
    public void testNoStateIsBothSuccessfulAndFailed() {
        // No state should be both successful and failed
        AgentState[] states = {
            AgentState.CREATING(),
            AgentState.RUNNING(),
            AgentState.FINISHED(),
            AgentState.ERROR(),
            AgentState.EXPIRED()
        };

        for (AgentState state : states) {
            assertFalse(state.isSuccessful() && state.isFailed(),
                "State " + state + " cannot be both successful and failed");
        }
    }

    @Test
    public void testAllStatesHaveConsistentBehavior() {
        // Comprehensive test of all state behaviors
        AgentState[] states = {
            AgentState.CREATING(),
            AgentState.RUNNING(),
            AgentState.FINISHED(),
            AgentState.ERROR(),
            AgentState.EXPIRED()
        };

        for (AgentState state : states) {
            // Terminal states should not be active
            if (state.isTerminal()) {
                assertFalse(state.isActive(),
                    "Terminal state " + state + " should not be active");
            }

            // Active states should not be terminal
            if (state.isActive()) {
                assertFalse(state.isTerminal(),
                    "Active state " + state + " should not be terminal");
            }

            // Successful states should be terminal but not failed
            if (state.isSuccessful()) {
                assertTrue(state.isTerminal(),
                    "Successful state " + state + " should be terminal");
                assertFalse(state.isFailed(),
                    "Successful state " + state + " should not be failed");
            }

            // Failed states should be terminal but not successful
            if (state.isFailed()) {
                assertTrue(state.isTerminal(),
                    "Failed state " + state + " should be terminal");
                assertFalse(state.isSuccessful(),
                    "Failed state " + state + " should not be successful");
            }
        }
    }

    @Test
    public void testToString() {
        // Test that toString returns the expected values
        assertEquals("CREATING", AgentState.CREATING().toString());
        assertEquals("RUNNING", AgentState.RUNNING().toString());
        assertEquals("FINISHED", AgentState.FINISHED().toString());
        assertEquals("ERROR", AgentState.ERROR().toString());
        assertEquals("EXPIRED", AgentState.EXPIRED().toString());
    }

    @Test
    public void testEquals() {
        // Test equality
        assertEquals(AgentState.CREATING(), AgentState.CREATING());
        assertEquals(AgentState.RUNNING(), AgentState.RUNNING());
        assertEquals(AgentState.FINISHED(), AgentState.FINISHED());
        assertEquals(AgentState.ERROR(), AgentState.ERROR());
        assertEquals(AgentState.EXPIRED(), AgentState.EXPIRED());

        // Test inequality
        assertNotEquals(AgentState.CREATING(), AgentState.RUNNING());
        assertNotEquals(AgentState.FINISHED(), AgentState.ERROR());
    }

    @Test
    public void testHashCode() {
        // Test that equal objects have equal hash codes
        assertEquals(AgentState.CREATING().hashCode(), AgentState.CREATING().hashCode());
        assertEquals(AgentState.RUNNING().hashCode(), AgentState.RUNNING().hashCode());
    }

    @Test
    public void testGetStatus() {
        // Test that getStatus returns the correct AgentStatus enum value
        assertEquals(AgentStatus.CREATING, AgentState.CREATING().getStatus());
        assertEquals(AgentStatus.RUNNING, AgentState.RUNNING().getStatus());
        assertEquals(AgentStatus.FINISHED, AgentState.FINISHED().getStatus());
        assertEquals(AgentStatus.ERROR, AgentState.ERROR().getStatus());
        assertEquals(AgentStatus.EXPIRED, AgentState.EXPIRED().getStatus());
    }

    @Test
    public void testOfWithAgentStatus() {
        // Test direct creation from AgentStatus enum
        assertEquals(AgentState.CREATING(), AgentState.of(AgentStatus.CREATING));
        assertEquals(AgentState.RUNNING(), AgentState.of(AgentStatus.RUNNING));
        assertEquals(AgentState.FINISHED(), AgentState.of(AgentStatus.FINISHED));
        assertEquals(AgentState.ERROR(), AgentState.of(AgentStatus.ERROR));
        assertEquals(AgentState.EXPIRED(), AgentState.of(AgentStatus.EXPIRED));
    }

    @Test
    public void testOfWithNullAgentStatus() {
        // Test that null AgentStatus defaults to CREATING
        AgentState result = AgentState.of((AgentStatus) null);
        assertEquals(AgentState.CREATING(), result);
    }
}
