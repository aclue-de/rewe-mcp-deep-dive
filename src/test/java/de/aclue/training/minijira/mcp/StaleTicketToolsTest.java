package de.aclue.training.minijira.mcp;

import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.Priority;
import de.aclue.training.minijira.domain.Status;
import de.aclue.training.minijira.store.IssueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleTicketToolsTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(LocalDate.of(2026, 10, 20).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private IssueStore store;

    private StaleTicketTools staleTicketTools;

    @BeforeEach
    void setUp() {
        staleTicketTools = new StaleTicketTools(store, FIXED_CLOCK);
    }

    @Test
    void listsOnlyInProgressOrBlockedTicketsOlderThanThresholdOldestFirst() {
        when(store.findAll()).thenReturn(List.of(
                issue("SHOP-1", Status.IN_PROGRESS, LocalDate.of(2026, 9, 30)),
                issue("SHOP-2", Status.BLOCKED, LocalDate.of(2026, 9, 1)),
                issue("SHOP-3", Status.IN_PROGRESS, LocalDate.of(2026, 10, 15)),
                issue("SHOP-4", Status.TODO, LocalDate.of(2026, 8, 1))));

        String result = staleTicketTools.getStaleTickets(14);

        assertThat(result).contains("SHOP-1", "SHOP-2", "49 days ago");
        assertThat(result).doesNotContain("SHOP-3", "SHOP-4");
        assertThat(result.indexOf("SHOP-2")).isLessThan(result.indexOf("SHOP-1"));
    }

    @Test
    void usesFourteenDaysWhenNoThresholdIsGiven() {
        when(store.findAll()).thenReturn(List.of(issue("SHOP-1", Status.IN_PROGRESS, LocalDate.of(2026, 10, 10))));

        String result = staleTicketTools.getStaleTickets(null);

        assertThat(result).isEqualTo("No in-progress or blocked tickets without an update for more than 14 days.");
    }

    @Test
    void rejectsThresholdBelowOne() {
        assertThatThrownBy(() -> staleTicketTools.getStaleTickets(0))
                .isInstanceOf(ToolCallException.class)
                .hasMessage("days must be at least 1, got 0.");
    }

    private Issue issue(String key, Status status, LocalDate updated) {
        return new Issue(key, "Summary", "Description", status, Priority.MEDIUM, "tom", "Sprint 42", List.of(),
                List.of(), updated);
    }
}
