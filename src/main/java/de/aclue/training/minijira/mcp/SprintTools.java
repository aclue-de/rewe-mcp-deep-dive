package de.aclue.training.minijira.mcp;

import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.Priority;
import de.aclue.training.minijira.domain.Status;
import de.aclue.training.minijira.store.IssueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SprintTools {

    private static final Logger log = LoggerFactory.getLogger(SprintTools.class);

    private final IssueStore store;

    public SprintTools(IssueStore store) {
        this.store = store;
    }

    @McpTool(
            name = "get_sprint_report",
            description = "Gives an at-a-glance overview of a sprint's status: count per status, blocked tickets, and open high-priority tickets. Use this tool for questions like 'How is Sprint 42 doing?'. For individual tickets: get_issue.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    public String getSprintReport(
            @McpToolParam(description = "Sprint name, e.g. 'Sprint 42'") String sprint) {

        log.info("Tool call: get_sprint_report(sprint={})", sprint);

        List<Issue> issues = store.search(null, null, sprint);

        if (issues.isEmpty()) {
            throw new ToolCallException("Sprint %s not found. Existing sprints: %s"
                    .formatted(sprint, String.join(", ", store.findSprintNames())));
        }

        StringBuilder report = new StringBuilder();
        report.append("# %s\n\n".formatted(sprint));

        report.append("**Count per status:**\n\n");
        for (Status status : Status.values()) {
            long count = issues.stream().filter(issue -> issue.status() == status).count();
            report.append("- %s: %d\n".formatted(status, count));
        }

        report.append("\n## Blocked tickets\n\n");
        List<Issue> blocked = issues.stream().filter(issue -> issue.status() == Status.BLOCKED).toList();
        if (blocked.isEmpty()) {
            report.append("None.\n");
        } else {
            for (Issue issue : blocked) {
                report.append("- %s: %s (Assignee: %s)\n".formatted(issue.key(), issue.summary(), issue.assignee()));
            }
        }

        report.append("\n## Open tickets with high priority\n\n");
        List<Issue> openHighPriority = issues.stream()
                .filter(issue -> issue.status() != Status.DONE)
                .filter(issue -> issue.priority() == Priority.HIGH)
                .toList();
        if (openHighPriority.isEmpty()) {
            report.append("None.\n");
        } else {
            for (Issue issue : openHighPriority) {
                report.append("- %s: %s (Status: %s, Assignee: %s)\n"
                        .formatted(issue.key(), issue.summary(), issue.status(), issue.assignee()));
            }
        }

        return report.toString().stripTrailing();
    }
}
