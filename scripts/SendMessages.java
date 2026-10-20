import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Plays the host by hand: sends the JSON-RPC messages from a .jsonl file (one message per
 * line) to the RUNNING Mini-Jira server and prints every response.
 *
 * Usage: java scripts/SendMessages.java [path-to-jsonl-file] [--header Name:Value]
 * Default file: scripts/handshake.jsonl
 *
 * Start the server first (java -jar target/mini-jira-mcp.jar). This script never starts a
 * server itself - it connects to http://localhost:8080/mcp (or the port in MINI_JIRA_PORT).
 *
 * What happens per line, the same as in any MCP client over HTTP:
 *  - every message is one HTTP POST to /mcp;
 *  - the response to "initialize" carries an Mcp-Session-Id header, which is sent along
 *    with every later message (MCP over HTTP is stateful);
 *  - responses arrive as Server-Sent Events ("data: {...}"), or as plain JSON;
 *  - notifications get "202 Accepted" and no response body.
 */
public class SendMessages {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public static void main(String[] args) throws Exception {
        // The JVM's default stdout encoding follows the platform console codepage on
        // Windows, which garbles non-ASCII characters. Force UTF-8 explicitly.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        Path messagesFile = Path.of("scripts/handshake.jsonl");
        List<String[]> extraHeaders = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--header") && i + 1 < args.length) {
                String[] header = args[++i].split(":", 2);
                extraHeaders.add(new String[] {header[0].trim(), header.length > 1 ? header[1].trim() : ""});
            } else {
                messagesFile = Path.of(args[i]);
            }
        }

        String port = System.getenv().getOrDefault("MINI_JIRA_PORT", "8080");
        URI endpoint = URI.create("http://localhost:" + port + "/mcp");
        HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

        System.out.println("Server:   " + endpoint);
        System.out.println("Messages: " + messagesFile);
        extraHeaders.forEach(h -> System.out.println("Header:   " + h[0] + ": " + h[1]));

        String sessionId = null;
        int index = 1;
        for (String message : readNonBlankLines(messagesFile)) {
            System.out.println();
            System.out.println("=== Message " + index++ + " ===");
            System.out.println("--> " + message);

            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8));
            if (sessionId != null) {
                request.header("Mcp-Session-Id", sessionId);
            }
            extraHeaders.forEach(h -> request.header(h[0], h[1]));

            HttpResponse<Stream<String>> response;
            try {
                response = http.send(request.build(), HttpResponse.BodyHandlers.ofLines());
            } catch (ConnectException e) {
                System.out.println();
                System.out.println("No server at " + endpoint + ". Start it first: java -jar target/mini-jira-mcp.jar");
                System.exit(1);
                return;
            } catch (HttpTimeoutException e) {
                System.out.println("<-- no response within " + TIMEOUT.toSeconds() + " seconds");
                continue;
            }

            String newSession = response.headers().firstValue("Mcp-Session-Id").orElse(null);
            if (newSession != null && !newSession.equals(sessionId)) {
                sessionId = newSession;
                System.out.println("    (server opened session " + sessionId + ")");
            }

            int status = response.statusCode();
            if (status == 202) {
                System.out.println("<-- 202 Accepted: a notification, so there is no response");
                response.body().close();
                continue;
            }

            String body = firstJsonMessage(response.body());
            System.out.println("<-- HTTP " + status);
            System.out.println(body.isEmpty() ? "(empty body)" : prettyPrintJson(body));
        }
    }

    /**
     * Reads the response until the first JSON message. For an SSE stream that is the first
     * "data:" line; the stream is then closed without waiting for the server to end it.
     */
    private static String firstJsonMessage(Stream<String> lines) {
        try (lines) {
            StringBuilder plain = new StringBuilder();
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line.startsWith("data:")) {
                    return line.substring("data:".length()).trim();
                }
                if (!line.startsWith("id:") && !line.startsWith("event:") && !line.isBlank()) {
                    plain.append(line);
                }
            }
            return plain.toString();
        } catch (RuntimeException e) {
            return "(response stream ended unexpectedly: " + e.getMessage() + ")";
        }
    }

    private static List<String> readNonBlankLines(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Minimal bracket/comma-aware re-indentation. Not a real JSON parser - good enough
     * for pretty-printing our own well-formed JSON-RPC lines, deliberately kept simple
     * since this file must stay dependency-free.
     */
    private static String prettyPrintJson(String json) {
        StringBuilder result = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (inString) {
                result.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            switch (c) {
                case '"' -> {
                    inString = true;
                    result.append(c);
                }
                case '{', '[' -> {
                    indent++;
                    result.append(c).append('\n').append("  ".repeat(indent));
                }
                case '}', ']' -> {
                    indent--;
                    result.append('\n').append("  ".repeat(Math.max(indent, 0))).append(c);
                }
                case ',' -> result.append(c).append('\n').append("  ".repeat(indent));
                case ':' -> result.append(c).append(' ');
                default -> {
                    if (!Character.isWhitespace(c)) {
                        result.append(c);
                    }
                }
            }
        }

        return result.toString();
    }
}
