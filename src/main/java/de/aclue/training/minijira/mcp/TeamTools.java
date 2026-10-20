package de.aclue.training.minijira.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.store.IssueStore;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Written by a colleague to answer "Who in the team is overloaded?".
 * It works - but Claude has a hard time using it.
 */
@Component
public class TeamTools {

    private final IssueStore store;
    private final ObjectMapper objectMapper;

    public TeamTools(IssueStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // Lab 2 · Part A: find out why Claude struggles with this tool, and fix it here.
    // Task: see project_docs/LABS_PLAYBOOK.md, Lab 2 Part A.
    // ============================================================
    @McpTool(name = "data", description = "Returns data.")
    public String data(
            @McpToolParam(description = "t") String t,
            @McpToolParam(description = "f") String f) throws Exception {

        if (!t.equals("w")) {
            throw new IllegalArgumentException("unsupported t: " + t);
        }

        List<Issue> issues = store.findAll().stream()
                .filter(issue -> issue.sprint().equals(f))
                .toList();

        return objectMapper.writeValueAsString(issues);
    }
}
