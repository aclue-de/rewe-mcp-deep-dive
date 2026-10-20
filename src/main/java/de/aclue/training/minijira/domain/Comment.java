package de.aclue.training.minijira.domain;

import java.time.LocalDate;

public record Comment(String author, LocalDate created, String body) {
}
