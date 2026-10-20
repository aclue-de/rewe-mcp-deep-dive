package de.aclue.training.minijira.mcp;

import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Lab 2 · Part C2: remove @Disabled once the prompt ticket-triage is implemented")
class TicketTriagePromptTest {

    @Test
    void producesUserMessageThatChecksTheTicketAgainstTheDefinitionOfDone() {
        TicketTriagePrompt prompt = new TicketTriagePrompt();

        PromptMessage message = prompt.ticketTriage("SHOP-104");

        assertThat(message.role()).isEqualTo(Role.USER);
        assertThat(message.content()).isInstanceOf(TextContent.class);

        String text = ((TextContent) message.content()).text();
        assertThat(text).contains("SHOP-104", "get_issue", "docs://team/conventions");
    }
}
