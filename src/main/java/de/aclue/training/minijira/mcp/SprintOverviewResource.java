package de.aclue.training.minijira.mcp;

import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.Status;
import de.aclue.training.minijira.store.IssueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SprintOverviewResource {

    private static final Logger log = LoggerFactory.getLogger(SprintOverviewResource.class);

    private final IssueStore store;

    public SprintOverviewResource(IssueStore store) {
        this.store = store;
    }

    @McpResource(
            uri = "mini-jira://sprints",
            name = "sprint-overview",
            title = "Sprint Overview",
            description = "All sprints with the number of tickets per status.",
            mimeType = "text/markdown")
    public String sprintOverview() {
        log.info("Resource read: mini-jira://sprints");

        StringBuilder markdown = new StringBuilder("# Sprints\n\n");
        markdown.append("| Sprint | Total | To do | In progress | Blocked | Done |\n");
        markdown.append("|---|---|---|---|---|---|\n");
        for (String sprint : store.findSprintNames()) {
            List<Issue> issues = store.search(null, null, sprint);
            markdown.append("| %s | %d | %d | %d | %d | %d |\n".formatted(sprint, issues.size(),
                    count(issues, Status.TODO), count(issues, Status.IN_PROGRESS),
                    count(issues, Status.BLOCKED), count(issues, Status.DONE)));
        }
        return markdown.toString().stripTrailing();
    }

    private long count(List<Issue> issues, Status status) {
        return issues.stream().filter(issue -> issue.status() == status).count();
    }
}
