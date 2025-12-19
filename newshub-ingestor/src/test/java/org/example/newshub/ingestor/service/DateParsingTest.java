package org.example.newshub.ingestor.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DateParsingTest {

    @Test
    void parses_rfc1123_and_iso_instants() {
        Instant rfc = DateParsing.tryParseInstant("Tue, 03 Dec 2024 14:05:00 GMT");
        assertThat(rfc).isNotNull();

        Instant iso = DateParsing.tryParseInstant("2024-12-03T14:05:00Z");
        assertThat(iso).isEqualTo(Instant.parse("2024-12-03T14:05:00Z"));
    }

    @Test
    void parses_iso_local_datetime_as_utc_fallback() {
        Instant t = DateParsing.tryParseInstant("2024-12-03T14:05:00");
        assertThat(t).isNotNull();
    }

    @Test
    void returns_null_on_blank_or_garbage() {
        assertThat(DateParsing.tryParseInstant(null)).isNull();
        assertThat(DateParsing.tryParseInstant("   ")).isNull();
        assertThat(DateParsing.tryParseInstant("not a date")).isNull();
    }
}
