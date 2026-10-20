package de.aclue.training.minijira.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record Issue(
        String key,
        String summary,
        String description,
        Status status,
        Priority priority,
        String assignee,
        String sprint,
        List<String> labels,
        List<Comment> comments,
        LocalDate updated) {

    public Issue {
        labels = labels == null ? List.of() : List.copyOf(labels);
        comments = comments == null ? List.of() : List.copyOf(comments);
    }

    public Issue withAddedComment(Comment comment) {
        List<Comment> updatedComments = new ArrayList<>(comments);
        updatedComments.add(comment);
        return new Issue(key, summary, description, status, priority, assignee, sprint, labels, updatedComments,
                comment.created());
    }
}
