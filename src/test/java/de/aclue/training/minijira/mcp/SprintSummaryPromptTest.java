package de.aclue.training.minijira.mcp;

import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SprintSummaryPromptTest {

    @Test
    void producesUserMessageAskingToSummariseTheSprint() {
        SprintSummaryPrompt prompt = new SprintSummaryPrompt();

        PromptMessage message = prompt.sprintSummary("Sprint 42");

        assertThat(message.role()).isEqualTo(Role.USER);
        assertThat(message.content()).isInstanceOf(TextContent.class);

        String text = ((TextContent) message.content()).text();
        assertThat(text).contains("Sprint 42");
        assertThat(text.toLowerCase()).contains("blocked");
    }
}
