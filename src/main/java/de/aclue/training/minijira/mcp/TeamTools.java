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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class TeamTools {

    private static final Logger log = LoggerFactory.getLogger(TeamTools.class);

    private static final int OVERLOAD_THRESHOLD = 3;

    private final IssueStore store;

    public TeamTools(IssueStore store) {
        this.store = store;
    }

    @McpTool(
            name = "get_team_workload",
            description = "Shows how open work in a sprint is distributed across the team: open, in-progress, blocked, and open high-priority tickets per person. Use this tool for questions like 'Who is overloaded in Sprint 42?' or 'Who has the most open work?'. For one person's ticket list: search_issues.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    public String getTeamWorkload(
            @McpToolParam(description = "Sprint name, e.g. 'Sprint 42'") String sprint) {

        log.info("Tool call: get_team_workload(sprint={})", sprint);

        List<Issue> issues = store.search(null, null, sprint);

        if (issues.isEmpty()) {
            throw new ToolCallException("Sprint %s not found. Existing sprints: %s"
                    .formatted(sprint, String.join(", ", store.findSprintNames())));
        }

        Map<String, List<Issue>> openByAssignee = new TreeMap<>();
        for (Issue issue : issues) {
            if (issue.status() != Status.DONE) {
                openByAssignee.computeIfAbsent(issue.assignee(), assignee -> new ArrayList<>()).add(issue);
            }
        }

        if (openByAssignee.isEmpty()) {
            return "# Workload %s\n\nNo open tickets.".formatted(sprint);
        }

        StringBuilder result = new StringBuilder();
        result.append("# Workload %s\n\n".formatted(sprint));
        result.append("| Person | Open | In progress | Blocked | High priority | Overloaded (%d+ open) |\n"
                .formatted(OVERLOAD_THRESHOLD));
        result.append("|---|---|---|---|---|---|\n");

        openByAssignee.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, List<Issue>> entry) -> entry.getValue().size())
                        .reversed())
                .forEach(entry -> {
                    List<Issue> open = entry.getValue();
                    result.append("| %s | %d | %d | %d | %d | %s |\n".formatted(
                            entry.getKey(),
                            open.size(),
                            count(open, Status.IN_PROGRESS),
                            count(open, Status.BLOCKED),
                            open.stream().filter(issue -> issue.priority() == Priority.HIGH).count(),
                            open.size() >= OVERLOAD_THRESHOLD ? "yes" : "no"));
                });

        return result.toString().stripTrailing();
    }

    private long count(List<Issue> issues, Status status) {
        return issues.stream().filter(issue -> issue.status() == status).count();
    }
}
