package de.aclue.training.minijira.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeamCalendarToolsTest {

    private TeamCalendarTools teamCalendarTools;

    @BeforeEach
    void setUp() {
        teamCalendarTools = new TeamCalendarTools(new ObjectMapper().findAndRegisterModules(), "data/team-calendar.json");
    }

    @Test
    void listsEntriesOfTheRequestedSprint() {
        String result = teamCalendarTools.getTeamCalendar("sprint 42");

        assertThat(result).contains("tom: VACATION from 2026-10-26 to 2026-10-30", "mira: ON_CALL");
        assertThat(result).doesNotContain("aylin");
    }

    @Test
    void saysSoWhenSprintHasNoEntries() {
        assertThat(teamCalendarTools.getTeamCalendar("Sprint 99")).isEqualTo("No calendar entries for Sprint 99.");
    }
}
