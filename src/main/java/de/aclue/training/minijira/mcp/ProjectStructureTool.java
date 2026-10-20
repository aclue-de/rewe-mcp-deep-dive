package de.aclue.training.minijira.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Returns the server's own project tree. Handy while exploring the server, but it
 * reveals internals, so it is only registered when mini-jira.admin-tools.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "mini-jira.admin-tools.enabled", havingValue = "true")
public class ProjectStructureTool {

    private static final Logger log = LoggerFactory.getLogger(ProjectStructureTool.class);

    private static final Set<String> SKIPPED_DIRECTORIES = Set.of("target", "logs", "node_modules");
    private static final int MAX_DEPTH = 12;

    private final Path root;

    public ProjectStructureTool() {
        this(Path.of("").toAbsolutePath());
    }

    ProjectStructureTool(Path root) {
        this.root = root;
    }

    @McpTool(
            name = "get_project_structure",
            description = "Returns the file and folder structure of this MCP server's own project. Use it when the user asks how the server is built or where something is implemented.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    public String getProjectStructure() {
        log.info("Tool call: get_project_structure()");

        StringBuilder tree = new StringBuilder(root.getFileName() + "/\n");
        appendChildren(root, "", 1, tree);
        return tree.toString().stripTrailing();
    }

    private void appendChildren(Path directory, String indent, int depth, StringBuilder tree) {
        if (depth > MAX_DEPTH) {
            return;
        }
        List<Path> children = visibleChildren(directory);
        for (int i = 0; i < children.size(); i++) {
            Path child = children.get(i);
            boolean last = i == children.size() - 1;
            boolean isDirectory = Files.isDirectory(child);
            tree.append(indent).append(last ? "└── " : "├── ")
                    .append(child.getFileName()).append(isDirectory ? "/" : "").append('\n');
            if (isDirectory) {
                appendChildren(child, indent + (last ? "    " : "│   "), depth + 1, tree);
            }
        }
    }

    private List<Path> visibleChildren(Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .filter(path -> !(Files.isDirectory(path) && SKIPPED_DIRECTORIES.contains(path.getFileName().toString())))
                    .sorted(Comparator.comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
