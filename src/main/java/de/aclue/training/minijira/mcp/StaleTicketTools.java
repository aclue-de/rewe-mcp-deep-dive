package de.aclue.training.minijira.mcp;

import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.Status;
import de.aclue.training.minijira.store.IssueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
public class StaleTicketTools {

    private static final Logger log = LoggerFactory.getLogger(StaleTicketTools.class);

    private static final int DEFAULT_DAYS = 14;

    private final IssueStore store;
    private final Clock clock;

    public StaleTicketTools(IssueStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @McpTool(
            name = "get_stale_tickets",
            description = "Finds tickets that are in progress or blocked but haven't been updated for a number of days, oldest first. Use this tool for questions like 'Which tickets are stuck?' or 'What hasn't moved in two weeks?'.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    public String getStaleTickets(
            @McpToolParam(required = false, description = "Minimum number of days without an update, default 14") Integer days) {

        log.info("Tool call: get_stale_tickets(days={})", days);

        int threshold = days == null ? DEFAULT_DAYS : days;
        if (threshold < 1) {
            throw new ToolCallException("days must be at least 1, got %d.".formatted(threshold));
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate cutoff = today.minusDays(threshold);

        List<Issue> stale = store.findAll().stream()
                .filter(issue -> issue.status() == Status.IN_PROGRESS || issue.status() == Status.BLOCKED)
                .filter(issue -> issue.updated() != null && issue.updated().isBefore(cutoff))
                .sorted(Comparator.comparing(Issue::updated))
                .toList();

        if (stale.isEmpty()) {
            return "No in-progress or blocked tickets without an update for more than %d days.".formatted(threshold);
        }

        StringBuilder result = new StringBuilder();
        result.append("# Tickets without an update for more than %d days\n\n".formatted(threshold));
        for (Issue issue : stale) {
            result.append("- %s: %s (Status: %s, Assignee: %s, last update: %s, %d days ago)\n".formatted(
                    issue.key(), issue.summary(), issue.status(), issue.assignee(), issue.updated(),
                    ChronoUnit.DAYS.between(issue.updated(), today)));
        }
        return result.toString().stripTrailing();
    }
}
