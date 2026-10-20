package de.aclue.training.minijira.integration;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the whole server on a random port and talks to it as a real MCP client over
 * HTTP, the same way Claude Code does. Verifies the checkpoint's surface (tools,
 * resources, prompts) and, crucially, that an unknown-key tool error reaches the client
 * as a normal result with isError: true rather than a JSON-RPC protocol error.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIT {

    @LocalServerPort
    private int port;

    private final List<McpSyncClient> clients = new ArrayList<>();

    private McpSyncClient connect(Consumer<HttpRequest.Builder> requestCustomizer) {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .customizeRequest(requestCustomizer)
                .build();
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();
        clients.add(client);
        return client;
    }

    private McpSyncClient connect() {
        return connect(request -> {
        });
    }

    @AfterEach
    void disconnect() {
        clients.forEach(McpSyncClient::closeGracefully);
    }

    @Test
    void reportsServerNameAndVersion() {
        McpSyncClient client = connect();

        assertThat(client.getServerInfo().name()).isEqualTo("mini-jira");
        assertThat(client.getServerInfo().version()).isEqualTo("1.0.0");
    }

    @Test
    void exposesTheExpectedToolsForThisCheckpoint() {
        List<String> toolNames = connect().listTools().tools().stream().map(Tool::name).toList();

        assertThat(toolNames).containsExactlyInAnyOrder(
                "search_issues", "get_issue", "get_project_structure");
    }

    @Test
    void exposesTheExpectedResourcesForThisCheckpoint() {
        List<String> resourceUris = connect().listResources().resources().stream().map(Resource::uri).toList();

        assertThat(resourceUris).containsExactlyInAnyOrder(
                "docs://team/conventions");
    }

    @Test
    void exposesTheExpectedPromptsForThisCheckpoint() {
        List<String> promptNames = connect().listPrompts().prompts().stream().map(Prompt::name).toList();

        assertThat(promptNames).containsExactlyInAnyOrder(
                "sprint-summary");
    }

    @Test
    void searchIssuesSucceeds() {
        CallToolResult result = connect().callTool(CallToolRequest.builder()
                .name("search_issues")
                .arguments(Map.of("assignee", "lena"))
                .build());

        assertThat(result.isError()).isFalse();
        assertThat(firstText(result)).contains("SHOP-101");
    }

    @Test
    void getIssueWithUnknownKeyIsAToolErrorNotAProtocolError() {
        CallToolResult result = connect().callTool(CallToolRequest.builder()
                .name("get_issue")
                .arguments(Map.of("key", "SHOP-999"))
                .build());

        assertThat(result.isError()).isTrue();
        assertThat(firstText(result)).isEqualTo("Ticket SHOP-999 not found. Tip: use search_issues.");
    }

    private String firstText(CallToolResult result) {
        Content content = result.content().get(0);
        return ((TextContent) content).text();
    }
}
