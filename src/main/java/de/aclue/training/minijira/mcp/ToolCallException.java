package de.aclue.training.minijira.mcp;

/**
 * Thrown from an {@code @McpTool} method to signal a tool-level error. Spring AI catches
 * this and turns it into a normal tool result with {@code isError: true}, using
 * {@link #getMessage()} as the text shown to the model - so the message must already be
 * the exact text the model (and ultimately the user) should see.
 */
public class ToolCallException extends RuntimeException {

    public ToolCallException(String message) {
        super(message);
    }
}
