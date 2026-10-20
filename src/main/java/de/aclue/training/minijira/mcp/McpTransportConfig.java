package de.aclue.training.minijira.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Replaces Spring AI's auto-configured HTTP transport with one that copies the caller's
 * role header into the request context, so tools can read it via
 * {@code exchange.transportContext().get(ROLE)}.
 * <p>
 * Simulation only: any client can send any header. A real server would take the role
 * from a signed login token (OAuth), not from a header the client picks itself.
 */
@Configuration
public class McpTransportConfig {

    public static final String ROLE_HEADER = "X-Mini-Jira-Role";
    public static final String ROLE = "role";
    public static final String DEFAULT_ROLE = "writer";

    @Bean
    public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(ObjectMapper objectMapper) {
        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .mcpEndpoint("/mcp")
                .contextExtractor(request -> {
                    String role = request.headers().firstHeader(ROLE_HEADER);
                    return McpTransportContext.create(Map.of(ROLE, role == null ? DEFAULT_ROLE : role));
                })
                .build();
    }
}
