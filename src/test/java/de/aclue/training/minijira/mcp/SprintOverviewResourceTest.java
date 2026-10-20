package de.aclue.training.minijira.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aclue.training.minijira.store.IssueStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Lab 2 · Part C1: remove @Disabled once the resource mini-jira://sprints is implemented")
class SprintOverviewResourceTest {

    private final IssueStore store = new IssueStore(new ObjectMapper().findAndRegisterModules(), "data/issues.json");
    private final SprintOverviewResource resource = new SprintOverviewResource(store);

    @Test
    void listsEverySprintAsATableRow() {
        String markdown = resource.sprintOverview();

        assertThat(markdown).contains("| Sprint | Total | To do | In progress | Blocked | Done |");
        assertThat(markdown).contains("| Sprint 41 |", "| Sprint 42 |", "| Sprint 43 |");
    }

    @Test
    void countsTicketsPerStatus() {
        long sprint42Total = store.search(null, null, "Sprint 42").size();

        String markdown = resource.sprintOverview();

        // Sprint 42 has exactly two blocked tickets (see DataConsistencyTest).
        String sprint42Row = markdown.lines().filter(line -> line.startsWith("| Sprint 42 |")).findFirst().orElseThrow();
        String[] cells = sprint42Row.split("\\|");
        assertThat(cells[2].trim()).isEqualTo(String.valueOf(sprint42Total));
        assertThat(cells[5].trim()).isEqualTo("2");
    }
}
