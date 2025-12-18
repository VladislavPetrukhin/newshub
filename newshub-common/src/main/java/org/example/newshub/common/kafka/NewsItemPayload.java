package org.example.newshub.common.kafka;

import java.time.Instant;

public record NewsItemPayload(
        String title,
        String description,
        String link,
        String pubDateRaw,
        Instant publishedAt,
        Instant addedAt,
        String guid,
        String sourceId,
        String sourceName,
        String sourceUrl
) {}
