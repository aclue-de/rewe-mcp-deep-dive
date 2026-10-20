package de.aclue.training.minijira.mcp;

import de.aclue.training.minijira.store.IssueStore;
import org.springframework.stereotype.Component;

@Component
public class SprintOverviewResource {

    private final IssueStore store;

    public SprintOverviewResource(IssueStore store) {
        this.store = store;
    }

    // ============================================================
    // Lab 2 · Part C1: implement the resource "mini-jira://sprints" here.
    // Add the @McpResource annotation and return the Markdown table.
    // Task and syntax: see project_docs/LABS_PLAYBOOK.md, Lab 2 Part C.
    // Optional self-check: SprintOverviewResourceTest.
    // ============================================================
    public String sprintOverview() {
        return "TODO";
    }
}
