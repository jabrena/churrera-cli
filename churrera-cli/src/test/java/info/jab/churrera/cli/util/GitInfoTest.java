package info.jab.churrera.cli.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GitInfoTest {

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void shouldPrintVersionAndCommitWhenGitPropertiesExists() {
        // Given
        String properties = """
                git.build.version=1.2.3
                git.commit.id.abbrev=deadbeef
                """;
        GitInfo gitInfo = new GitInfo(() ->
                new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8)));

        // When
        gitInfo.print();

        // Then
        String output = outputStream.toString();
        assertThat(output)
                .contains("A CLI tool designed to orchestrate Cursor Cloud Agents REST API.")
                .contains("Version:")
                .contains("1.2.3")
                .contains("Commit:")
                .contains("deadbeef");
    }

    @Test
    void shouldNotifyWhenGitPropertiesMissing() {
        // Given
        GitInfo gitInfo = new GitInfo(() -> null);

        // When
        gitInfo.print();

        // Then
        assertThat(outputStream.toString()).contains("git.properties not found");
    }

    @Test
    void shouldReportErrorWhenReadingPropertiesFails() {
        // Given
        GitInfo gitInfo = new GitInfo(FailingInputStream::new);

        // When
        gitInfo.print();

        // Then
        assertThat(outputStream.toString()).contains("Error printing git info: boom!");
    }

    private static final class FailingInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("boom!");
        }
    }
}

