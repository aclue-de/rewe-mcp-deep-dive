package de.aclue.training.minijira.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aclue.training.minijira.domain.CalendarEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class TeamCalendarTools {

    private static final Logger log = LoggerFactory.getLogger(TeamCalendarTools.class);

    private final List<CalendarEntry> entries;

    public TeamCalendarTools(ObjectMapper objectMapper, @Value("${mini-jira.calendar.path}") String calendarPath) {
        File calendarFile = new File(calendarPath);
        try {
            this.entries = objectMapper.readValue(calendarFile, new TypeReference<List<CalendarEntry>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Calendar file could not be read: " + calendarFile.getAbsolutePath(), e);
        }
    }

    @McpTool(
            name = "get_team_calendar",
            description = "Returns the team's vacation and on-call calendar for a sprint. Use it for questions about who is available or who is on call.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    public String getTeamCalendar(
            @McpToolParam(description = "Sprint name, e.g. 'Sprint 42'") String sprint) {

        log.info("Tool call: get_team_calendar(sprint={})", sprint);

        List<CalendarEntry> matches = entries.stream()
                .filter(entry -> entry.sprint().equalsIgnoreCase(sprint))
                .toList();

        if (matches.isEmpty()) {
            return "No calendar entries for %s.".formatted(sprint);
        }

        StringBuilder result = new StringBuilder();
        result.append("# Team calendar %s\n\n".formatted(sprint));
        for (CalendarEntry entry : matches) {
            result.append("- %s: %s from %s to %s\n".formatted(entry.person(), entry.type(), entry.from(), entry.to()));
        }
        return result.toString().stripTrailing();
    }
}
