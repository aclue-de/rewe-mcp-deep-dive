package de.aclue.training.minijira.domain;

public class IssueNotFoundException extends RuntimeException {

    private final String key;

    public IssueNotFoundException(String key) {
        super("Issue not found: " + key);
        this.key = key;
    }

    public String key() {
        return key;
    }
}
