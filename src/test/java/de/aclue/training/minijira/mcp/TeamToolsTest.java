package de.aclue.training.minijira.mcp;

import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.Priority;
import de.aclue.training.minijira.domain.Status;
import de.aclue.training.minijira.store.IssueStore;
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
class TeamToolsTest {

    @Mock
    private IssueStore store;

    @InjectMocks
    private TeamTools teamTools;

    @Test
    void countsOpenWorkPerPersonAndFlagsOverload() {
        when(store.search(null, null, "Sprint 42")).thenReturn(List.of(
                issue("SHOP-1", Status.IN_PROGRESS, Priority.HIGH, "tom"),
                issue("SHOP-2", Status.BLOCKED, Priority.LOW, "tom"),
                issue("SHOP-3", Status.TODO, Priority.LOW, "tom"),
                issue("SHOP-4", Status.TODO, Priority.LOW, "lena"),
                issue("SHOP-5", Status.DONE, Priority.LOW, "lena")));

        String workload = teamTools.getTeamWorkload("Sprint 42");

        assertThat(workload).contains("| tom | 3 | 1 | 1 | 1 | yes |");
        assertThat(workload).contains("| lena | 1 | 0 | 0 | 0 | no |");
        assertThat(workload.indexOf("| tom")).isLessThan(workload.indexOf("| lena"));
    }

    @Test
    void unknownSprintIsAToolErrorListingExistingSprints() {
        when(store.search(null, null, "Sprint 99")).thenReturn(List.of());
        when(store.findSprintNames()).thenReturn(List.of("Sprint 41", "Sprint 42"));

        assertThatThrownBy(() -> teamTools.getTeamWorkload("Sprint 99"))
                .isInstanceOf(ToolCallException.class)
                .hasMessage("Sprint Sprint 99 not found. Existing sprints: Sprint 41, Sprint 42");
    }

    private Issue issue(String key, Status status, Priority priority, String assignee) {
        return new Issue(key, "Summary", "Description", status, priority, assignee, "Sprint 42", List.of(), List.of(),
                null);
    }
}
