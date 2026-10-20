package de.aclue.training.minijira.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ConventionsResource {

    private static final Logger log = LoggerFactory.getLogger(ConventionsResource.class);

    private static final Path DEFINITION_OF_DONE = Path.of("docs/definition-of-done.md");

    @McpResource(
            uri = "docs://team/conventions",
            name = "team-conventions",
            title = "Team Conventions",
            description = "The team's Definition of Done.",
            mimeType = "text/markdown")
    public String teamConventions() throws IOException {
        log.info("Resource read: docs://team/conventions");
        return Files.readString(DEFINITION_OF_DONE);
    }
}
