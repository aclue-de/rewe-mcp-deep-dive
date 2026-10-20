package de.aclue.training.minijira.domain;

import java.time.LocalDate;

public record CalendarEntry(String person, Type type, LocalDate from, LocalDate to, String sprint) {

    public enum Type {
        VACATION,
        ON_CALL
    }
}
