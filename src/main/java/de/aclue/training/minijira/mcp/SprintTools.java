package de.aclue.training.minijira.mcp;

import de.aclue.training.minijira.store.IssueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class SprintTools {

    private static final Logger log = LoggerFactory.getLogger(SprintTools.class);

    private final IssueStore store;

    public SprintTools(IssueStore store) {
        this.store = store;
    }

    // ============================================================
    // Lab 2 · Part B: implement get_sprint_report here.
    // Task and syntax: see project_docs/LABS_PLAYBOOK.md, Lab 2 Part B.
    // Optional self-check: SprintToolsTest, SprintToolsDataConsistencyTest.
    // ============================================================
    @McpTool(
            name = "get_sprint_report",
            description = "TODO: describe when a model should reach for this tool instead of search_issues, and give an example sprint value.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    public String getSprintReport(
            @McpToolParam(description = "TODO: describe the sprint parameter, e.g. \"Sprint name, like 'Sprint 42'\"") String sprint) {

        log.info("Tool call: get_sprint_report(sprint={})", sprint);

        // TODO: if the sprint doesn't exist, throw a ToolCallException that lists the sprints that do exist
        //       (IssueStore.findSprintNames() helps)
        // TODO: count how many tickets are in each status (TODO, IN_PROGRESS, BLOCKED, DONE)
        // TODO: list the BLOCKED tickets and the open (not DONE) HIGH-priority tickets
        // TODO: return it all as a short, readable Markdown report
        return "TODO";
    }
}
