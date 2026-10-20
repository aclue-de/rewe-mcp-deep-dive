package de.aclue.training.minijira.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.IssueNotFoundException;
import de.aclue.training.minijira.domain.Priority;
import de.aclue.training.minijira.domain.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueStoreTest {

    private static final String FIXTURE = """
            [
              {
                "key": "SHOP-1",
                "summary": "First ticket",
                "description": "Description one",
                "status": "TODO",
                "priority": "LOW",
                "assignee": "lena",
                "sprint": "Sprint 1",
                "labels": [],
                "comments": []
              },
              {
                "key": "SHOP-2",
                "summary": "Second ticket",
                "description": "Description two",
                "status": "BLOCKED",
                "priority": "HIGH",
                "assignee": "tom",
                "sprint": "Sprint 1",
                "labels": [],
                "comments": [
                  {"author": "tom", "created": "2026-01-10", "body": "First comment"}
                ]
              }
            ]
            """;

    private IssueStore storeOver(Path dataFile) {
        return new IssueStore(new ObjectMapper().findAndRegisterModules(), dataFile.toString());
    }

    private Path fixtureFile(Path tempDir) throws IOException {
        Path file = tempDir.resolve("issues.json");
        Files.writeString(file, FIXTURE);
        return file;
    }

    @Test
    void findByKeyReturnsMatchingIssue(@TempDir Path tempDir) throws IOException {
        IssueStore store = storeOver(fixtureFile(tempDir));

        assertThat(store.findByKey("SHOP-1")).isPresent();
        assertThat(store.findByKey("SHOP-1").orElseThrow().summary()).isEqualTo("First ticket");
    }

    @Test
    void findByKeyReturnsEmptyForUnknownKey(@TempDir Path tempDir) throws IOException {
        IssueStore store = storeOver(fixtureFile(tempDir));

        assertThat(store.findByKey("SHOP-999")).isEmpty();
    }

    @Test
    void searchFiltersCaseInsensitivelyByAssigneeAndSprint(@TempDir Path tempDir) throws IOException {
        IssueStore store = storeOver(fixtureFile(tempDir));

        List<Issue> result = store.search(null, "LENA", "sprint 1");

        assertThat(result).extracting(Issue::key).containsExactly("SHOP-1");
    }

    @Test
    void searchFiltersByStatus(@TempDir Path tempDir) throws IOException {
        IssueStore store = storeOver(fixtureFile(tempDir));

        List<Issue> result = store.search(Status.BLOCKED, null, null);

        assertThat(result).extracting(Issue::key).containsExactly("SHOP-2");
    }

    @Test
    void addCommentChangesOnlyTheInMemoryData(@TempDir Path tempDir) throws IOException {
        Path dataFile = fixtureFile(tempDir);
        String original = Files.readString(dataFile);
        IssueStore store = storeOver(dataFile);

        Issue updated = store.addComment("SHOP-1", "claude", LocalDate.of(2026, 9, 24), "New comment");

        assertThat(updated.comments()).hasSize(1);
        assertThat(updated.comments().get(0).body()).isEqualTo("New comment");
        assertThat(store.findByKey("SHOP-1").orElseThrow().comments()).hasSize(1);

        assertThat(Files.readString(dataFile)).isEqualTo(original);
        IssueStore restarted = storeOver(dataFile);
        assertThat(restarted.findByKey("SHOP-1").orElseThrow().comments()).isEmpty();
    }

    @Test
    void addCommentThrowsForUnknownKey(@TempDir Path tempDir) throws IOException {
        IssueStore store = storeOver(fixtureFile(tempDir));

        assertThatThrownBy(() -> store.addComment("SHOP-999", "claude", LocalDate.now(), "Text"))
                .isInstanceOf(IssueNotFoundException.class);
    }

    @Test
    void constructorFailsFastWhenDataFileIsMissing(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist.json");

        assertThatThrownBy(() -> storeOver(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ticket file not found");
    }

    @Test
    void constructorFailsFastWhenDataFileIsInvalid(@TempDir Path tempDir) throws IOException {
        Path invalid = tempDir.resolve("issues.json");
        Files.writeString(invalid, "not valid json");

        assertThatThrownBy(() -> storeOver(invalid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid");
    }
}
