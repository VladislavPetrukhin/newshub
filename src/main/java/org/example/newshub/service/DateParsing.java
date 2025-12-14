package org.example.newshub.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

final class DateParsing {

    private DateParsing() {}

    static Instant tryParseInstant(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        try {
            return ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {}

        try {
            return Instant.parse(s);
        } catch (DateTimeParseException ignored) {}

        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        );

        for (DateTimeFormatter f : fmts) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(s, f);
                return ldt.atZone(ZoneId.systemDefault()).toInstant();
            } catch (DateTimeParseException ignored) {}
        }

        return null;
    }
}
