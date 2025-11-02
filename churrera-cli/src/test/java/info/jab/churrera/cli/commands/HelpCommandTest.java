package info.jab.churrera.cli.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HelpCommand.
 */
@ExtendWith(MockitoExtension.class)
class HelpCommandTest {

    @Test
    void testRun_DisplaysHelpMessage() {
        // Given
        HelpCommand helpCommand = new HelpCommand();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // When
            helpCommand.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("Available commands:"));
            assertTrue(output.contains("jobs"));
            assertTrue(output.contains("jobs new {path}"));
            assertTrue(output.contains("jobs status {uuid}"));
            assertTrue(output.contains("jobs logs {uuid}"));
            assertTrue(output.contains("jobs pr {uuid}"));
            assertTrue(output.contains("jobs delete {uuid}"));
            assertTrue(output.contains("help"));
            assertTrue(output.contains("clear"));
            assertTrue(output.contains("quit"));
            assertTrue(output.contains("Examples:"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRun_ContainsExampleCommands() {
        // Given
        HelpCommand helpCommand = new HelpCommand();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // When
            helpCommand.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("jobs new churrera-cli/src/test/resources/examples/hello-world/workflow-hello-world.xml"));
            // Examples now show 8-char prefixes; accept prefix examples
            assertTrue(output.contains("jobs status a90e4050"));
            assertTrue(output.contains("jobs logs a90e4050"));
            assertTrue(output.contains("jobs pr a90e4050"));
            assertTrue(output.contains("jobs delete a90e4050"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
