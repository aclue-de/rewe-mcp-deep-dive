package de.aclue.training.minijira.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aclue.training.minijira.domain.Comment;
import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.Priority;
import de.aclue.training.minijira.domain.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DataConsistencyTest {

    private static final String SPRINT_42 = "Sprint 42";

    private IssueStore store;

    @BeforeEach
    void loadRealData() {
        store = new IssueStore(new ObjectMapper().findAndRegisterModules(), "data/issues.json");
    }

    @Test
    void shop101MatchesLab2Scenario() {
        Issue shop101 = store.findByKey("SHOP-101").orElseThrow();

        assertThat(shop101.summary()).isEqualTo("Checkout fails on coupon code");
        assertThat(shop101.status()).isEqualTo(Status.IN_PROGRESS);
        assertThat(shop101.priority()).isEqualTo(Priority.HIGH);
        assertThat(shop101.assignee()).isEqualTo("lena");
        assertThat(shop101.sprint()).isEqualTo(SPRINT_42);
        assertThat(shop101.comments()).hasSizeBetween(2, 3);
    }

    @Test
    void shop103MatchesLab1Scenario() {
        Issue shop103 = store.findByKey("SHOP-103").orElseThrow();

        assertThat(shop103.summary()).isEqualTo("Newsletter opt-in doesn't store a date");
        assertThat(shop103.status()).isEqualTo(Status.BLOCKED);
        assertThat(shop103.priority()).isEqualTo(Priority.HIGH);
        assertThat(shop103.sprint()).isEqualTo(SPRINT_42);
    }

    @Test
    void shop104DescriptionMissesTestsAsDodPoint() {
        Issue shop104 = store.findByKey("SHOP-104").orElseThrow();

        assertThat(shop104.description()).doesNotContainPattern("(?i)\\btests?\\b");
    }

    @Test
    void sprint42HasExactlyTwoBlockedTicketsWithDifferentAssignees() {
        List<Issue> blocked = store.search(Status.BLOCKED, null, SPRINT_42);

        assertThat(blocked).hasSize(2);
        assertThat(blocked.stream().map(Issue::assignee).distinct()).hasSize(2);
    }

    @Test
    void sprint42HasAtLeastTwoOpenHighPriorityTickets() {
        long openHighPriority = store.search(null, null, SPRINT_42).stream()
                .filter(issue -> issue.status() != Status.DONE)
                .filter(issue -> issue.priority() == Priority.HIGH)
                .count();

        assertThat(openHighPriority).isGreaterThanOrEqualTo(2);
    }

    @Test
    void lenaHasExactlyThreeTicketsThatAreNotDone() {
        long lenaNotDone = store.findAll().stream()
                .filter(issue -> issue.assignee().equalsIgnoreCase("lena"))
                .filter(issue -> issue.status() != Status.DONE)
                .count();

        assertThat(lenaNotDone).isEqualTo(3);
    }

    @Test
    void everyStatusAppearsInSprint42() {
        Map<Status, List<Issue>> byStatus = store.search(null, null, SPRINT_42).stream()
                .collect(Collectors.groupingBy(Issue::status));

        assertThat(byStatus.keySet()).containsExactlyInAnyOrder(Status.values());
    }

    @Test
    void tomHasClearlyTheMostOpenWorkInSprint42() {
        Map<String, Long> openPerAssignee = store.search(null, null, SPRINT_42).stream()
                .filter(issue -> issue.status() != Status.DONE)
                .collect(Collectors.groupingBy(Issue::assignee, Collectors.counting()));

        long tom = openPerAssignee.get("tom");
        assertThat(tom).isGreaterThanOrEqualTo(3);
        openPerAssignee.forEach((assignee, open) -> {
            if (!assignee.equals("tom")) {
                assertThat(open).isLessThan(tom);
            }
        });
    }

    @Test
    void everyTicketHasAnUpdatedDateNotBeforeItsLastComment() {
        for (Issue issue : store.findAll()) {
            assertThat(issue.updated()).describedAs(issue.key()).isNotNull();
            issue.comments().forEach(comment ->
                    assertThat(issue.updated()).describedAs(issue.key()).isAfterOrEqualTo(comment.created()));
        }
    }

    @Test
    void hasAroundTwentyTickets() {
        assertThat(store.findAll()).hasSizeBetween(20, 26);
        assertThat(store.findByKey("SHOP-117")).isEmpty();
        assertThat(store.findByKey("SHOP-124")).isEmpty();
    }

    @Test
    void ticketsAreDistributedAcrossThreeSprints() {
        List<String> sprints = store.findAll().stream()
                .map(Issue::sprint)
                .distinct()
                .toList();

        assertThat(sprints).containsExactlyInAnyOrder("Sprint 41", "Sprint 42", "Sprint 43");
    }
}
