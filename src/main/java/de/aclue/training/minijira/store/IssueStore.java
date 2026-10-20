package de.aclue.training.minijira.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aclue.training.minijira.domain.Comment;
import de.aclue.training.minijira.domain.Issue;
import de.aclue.training.minijira.domain.IssueNotFoundException;
import de.aclue.training.minijira.domain.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the tickets once at startup and keeps them in memory. Changes (comments) are never
 * written back to the file: every server restart starts again from the original data.
 */
@Component
public class IssueStore {

    private static final Logger log = LoggerFactory.getLogger(IssueStore.class);

    private final ObjectMapper objectMapper;
    private final File dataFile;
    private final Map<String, Issue> issuesByKey = new LinkedHashMap<>();

    public IssueStore(ObjectMapper objectMapper, @Value("${mini-jira.data.path}") String dataPath) {
        this.objectMapper = objectMapper;
        this.dataFile = new File(dataPath);
        load();
    }

    private void load() {
        if (!dataFile.isFile()) {
            fail("Ticket file not found: " + dataFile.getAbsolutePath(), null);
        }
        try {
            List<Issue> issues = objectMapper.readValue(dataFile, new TypeReference<List<Issue>>() {
            });
            issuesByKey.clear();
            for (Issue issue : issues) {
                issuesByKey.put(issue.key(), issue);
            }
        } catch (IOException e) {
            fail("Ticket file is invalid: " + dataFile.getAbsolutePath(), e);
        }
    }

    private void fail(String message, Exception cause) {
        log.error(message, cause);
        System.err.println(message);
        throw new IllegalStateException(message, cause);
    }

    public List<Issue> findAll() {
        return List.copyOf(issuesByKey.values());
    }

    public Optional<Issue> findByKey(String key) {
        return Optional.ofNullable(issuesByKey.get(key));
    }

    public List<Issue> search(Status status, String assignee, String sprint) {
        List<Issue> result = new ArrayList<>();
        for (Issue issue : issuesByKey.values()) {
            if (status != null && issue.status() != status) {
                continue;
            }
            if (assignee != null && !issue.assignee().equalsIgnoreCase(assignee)) {
                continue;
            }
            if (sprint != null && !issue.sprint().equalsIgnoreCase(sprint)) {
                continue;
            }
            result.add(issue);
        }
        return result;
    }

    public List<String> findSprintNames() {
        return issuesByKey.values().stream()
                .map(Issue::sprint)
                .distinct()
                .sorted()
                .toList();
    }

    public Issue addComment(String key, String author, LocalDate created, String body) {
        Issue issue = issuesByKey.get(key);
        if (issue == null) {
            throw new IssueNotFoundException(key);
        }
        Issue updated = issue.withAddedComment(new Comment(author, created, body));
        issuesByKey.put(key, updated);
        return updated;
    }
}
