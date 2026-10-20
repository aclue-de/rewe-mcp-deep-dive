package de.aclue.training.minijira.mcp;

import de.aclue.training.minijira.domain.Comment;
import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.IssueNotFoundException;
import de.aclue.training.minijira.domain.Priority;
import de.aclue.training.minijira.domain.Status;
import de.aclue.training.minijira.store.IssueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueToolsTest {

    @Mock
    private IssueStore store;

    @InjectMocks
    private IssueTools issueTools;

    @Test
    void searchIssuesReturnsCompactListForMatches() {
        when(store.search(null, "lena", null)).thenReturn(List.of(
                issue("SHOP-1", "First ticket", Status.TODO, Priority.LOW, "lena", "Sprint 1")));

        String result = issueTools.searchIssues(null, "lena", null);

        assertThat(result).contains("SHOP-1", "First ticket", "TODO", "LOW", "lena", "Sprint 1");
    }

    @Test
    void searchIssuesReportsWhenNoMatches() {
        when(store.search(any(), any(), any())).thenReturn(List.of());

        String result = issueTools.searchIssues(null, null, null);

        assertThat(result).isEqualTo("No tickets found.");
    }

    @Test
    void searchIssuesCapsAtTwentyResultsAndSaysSo() {
        List<Issue> many = IntStream.rangeClosed(1, 25)
                .mapToObj(i -> issue("SHOP-" + i, "Ticket " + i, Status.TODO, Priority.LOW, "tom", "Sprint 1"))
                .toList();
        when(store.search(any(), any(), any())).thenReturn(many);

        String result = issueTools.searchIssues(null, null, null);

        long listedLines = result.lines().filter(line -> line.startsWith("- SHOP-")).count();
        assertThat(listedLines).isEqualTo(20);
        assertThat(result).contains("5 more results");
    }

    @Test
    void getIssueReturnsMarkdownWithDescriptionAndComments() {
        Issue issue = new Issue("SHOP-101", "Checkout broken", "Detailed description", Status.IN_PROGRESS,
                Priority.HIGH, "lena", "Sprint 42", List.of(),
                List.of(new Comment("tom", LocalDate.of(2026, 1, 1), "Comment text")), null);
        when(store.findByKey("SHOP-101")).thenReturn(Optional.of(issue));

        String markdown = issueTools.getIssue("SHOP-101");

        assertThat(markdown).contains("SHOP-101", "Checkout broken", "Detailed description",
                "Comment text", "tom");
    }

    @Test
    void getIssueThrowsMessageForUnknownKey() {
        when(store.findByKey("SHOP-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueTools.getIssue("SHOP-999"))
                .isInstanceOf(ToolCallException.class)
                .hasMessage("Ticket SHOP-999 not found. Tip: use search_issues.");
    }

    @Test
    void getIssueThrowsForMalformedKey() {
        assertThatThrownBy(() -> issueTools.getIssue("not-a-key"))
                .isInstanceOf(ToolCallException.class);
    }

    @Test
    void addCommentDelegatesToStoreWithClaudeAsAuthorAndTodaysDate() {
        Issue updated = issue("SHOP-101", "Checkout broken", Status.IN_PROGRESS, Priority.HIGH, "lena", "Sprint 42");
        when(store.addComment(eq("SHOP-101"), eq("claude"), eq(LocalDate.now()), eq("Thanks for reporting")))
                .thenReturn(updated);

        String result = issueTools.addComment("SHOP-101", "Thanks for reporting");

        assertThat(result).contains("SHOP-101");
    }

    @Test
    void addCommentThrowsMessageForUnknownKey() {
        when(store.addComment(eq("SHOP-999"), any(), any(), any())).thenThrow(new IssueNotFoundException("SHOP-999"));

        assertThatThrownBy(() -> issueTools.addComment("SHOP-999", "Text"))
                .isInstanceOf(ToolCallException.class)
                .hasMessage("Ticket SHOP-999 not found. Tip: use search_issues.");
    }

    private Issue issue(String key, String summary, Status status, Priority priority, String assignee,
            String sprint) {
        return new Issue(key, summary, "Description", status, priority, assignee, sprint, List.of(), List.of(), null);
    }
}
