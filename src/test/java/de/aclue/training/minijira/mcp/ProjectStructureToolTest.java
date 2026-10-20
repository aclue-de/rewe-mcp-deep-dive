package de.aclue.training.minijira.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectStructureToolTest {

    @Test
    void rendersTreeAndSkipsBuildOutputLogsAndHiddenFolders(@TempDir Path root) throws IOException {
        Files.createDirectories(root.resolve("src/main/java"));
        Files.writeString(root.resolve("src/main/java/App.java"), "");
        Files.writeString(root.resolve("pom.xml"), "");
        Files.createDirectories(root.resolve("target"));
        Files.createDirectories(root.resolve("logs"));
        Files.createDirectories(root.resolve(".git"));

        String tree = new ProjectStructureTool(root).getProjectStructure();

        assertThat(tree).contains("src/", "main/", "java/", "App.java", "pom.xml");
        assertThat(tree).doesNotContain("target", "logs", ".git");
        assertThat(tree).doesNotContain(root.toString());
    }
}
