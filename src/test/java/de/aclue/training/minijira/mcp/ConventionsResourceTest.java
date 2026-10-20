package de.aclue.training.minijira.mcp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConventionsResourceTest {

    @Test
    void returnsDefinitionOfDoneFileContentVerbatim() throws IOException {
        ConventionsResource resource = new ConventionsResource();

        String content = resource.teamConventions();

        assertThat(content).isEqualTo(Files.readString(Path.of("docs/definition-of-done.md")));
        assertThat(content).contains("Definition of Done");
    }
}
