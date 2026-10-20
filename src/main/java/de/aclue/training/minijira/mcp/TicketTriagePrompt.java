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
public class TicketTriagePrompt {

    private static final Logger log = LoggerFactory.getLogger(TicketTriagePrompt.class);

    @McpPrompt(
            name = "ticket-triage",
            description = "Checks a ticket against the team's Definition of Done and suggests next steps")
    public PromptMessage ticketTriage(
            @McpArg(name = "key", description = "Ticket key, e.g. SHOP-104", required = true) String key) {

        log.info("Prompt requested: ticket-triage(key={})", key);

        String text = ("Triage ticket %s. Load it with the get_issue tool and read the team's "
                + "Definition of Done (resource docs://team/conventions). List which points of the "
                + "Definition of Done the ticket does not cover yet, then suggest up to three "
                + "concrete next steps.").formatted(key);

        return new PromptMessage(Role.USER, new TextContent(text));
    }
}
