package de.aclue.training.minijira.mcp;

import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.Priority;
import de.aclue.training.minijira.domain.Status;
import de.aclue.training.minijira.store.IssueStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled("Lab 2 · Part B: remove @Disabled once get_sprint_report is implemented")
class SprintToolsTest {

    @Mock
    private IssueStore store;

    @InjectMocks
    private SprintTools sprintTools;

    @Test
    void reportsCountsPerStatusBlockedAndOpenHighPriorityTickets() {
        List<Issue> sprintIssues = List.of(
                issue("SHOP-1", "A", Status.TODO, Priority.LOW, "tom"),
                issue("SHOP-2", "B", Status.IN_PROGRESS, Priority.HIGH, "lena"),
                issue("SHOP-3", "C", Status.BLOCKED, Priority.HIGH, "aylin"),
                issue("SHOP-4", "D", Status.BLOCKED, Priority.MEDIUM, "jonas"),
                issue("SHOP-5", "E", Status.DONE, Priority.HIGH, "mira"));
        when(store.search(null, null, "Sprint 42")).thenReturn(sprintIssues);

        String report = sprintTools.getSprintReport("Sprint 42");

        assertThat(report).contains("TODO: 1", "IN_PROGRESS: 1", "BLOCKED: 2", "DONE: 1");

        String blockedSection = report.substring(report.indexOf("Blocked tickets"));
        assertThat(blockedSection).contains("SHOP-3").contains("SHOP-4").doesNotContain("SHOP-1");

        String highPrioSection = report.substring(report.indexOf("high priority"));
        assertThat(highPrioSection).contains("SHOP-2").contains("SHOP-3");
        // SHOP-5 is DONE, so it must not appear even though its priority is HIGH
        assertThat(highPrioSection).doesNotContain("SHOP-5");
    }

    @Test
    void unknownSprintThrowsErrorListingExistingSprints() {
        when(store.search(null, null, "Sprint 99")).thenReturn(List.of());
        when(store.findSprintNames()).thenReturn(List.of("Sprint 41", "Sprint 42"));

        assertThatThrownBy(() -> sprintTools.getSprintReport("Sprint 99"))
                .isInstanceOf(ToolCallException.class)
                .hasMessageContaining("Sprint 99")
                .hasMessageContaining("Sprint 41")
                .hasMessageContaining("Sprint 42");
    }

    private Issue issue(String key, String summary, Status status, Priority priority, String assignee) {
        return issue(key, summary, status, priority, assignee, "Sprint 42");
    }

    private Issue issue(String key, String summary, Status status, Priority priority, String assignee,
            String sprint) {
        return new Issue(key, summary, "Description", status, priority, assignee, sprint, List.of(), List.of(), null);
    }
}
