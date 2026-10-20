package de.aclue.training.minijira.mcp;

import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

@Component
public class SprintSummaryPrompt {

    private static final Logger log = LoggerFactory.getLogger(SprintSummaryPrompt.class);

    @McpPrompt(
            name = "sprint-summary",
            description = "Summarizes the status of a sprint")
    public PromptMessage sprintSummary(
            @McpArg(name = "sprint", description = "Sprint name, e.g. 'Sprint 42'", required = true) String sprint) {

        log.info("Prompt requested: sprint-summary(sprint={})", sprint);

        String text = ("Summarize the current status of %s using the Mini-Jira tools. "
                + "List the blocked tickets first, then the rest.").formatted(sprint);

        return new PromptMessage(Role.USER, new TextContent(text));
    }
}
