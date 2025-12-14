package org.example.newshub.model;

import java.time.Instant;
import java.util.Objects;

public record NewsItem(
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
) {
    public String key() {
        if (guid != null && !guid.isBlank()) return "guid:" + guid;
        if (link != null && !link.isBlank()) return "link:" + link;
        return "title:" + Objects.toString(title, "");
    }
}
