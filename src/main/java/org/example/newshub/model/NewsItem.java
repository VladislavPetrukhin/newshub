package org.example.newshub.model;

import java.time.Instant;
import java.util.Objects;

public record NewsItem(
        Long id,
        String title,
        String description,
        String link,
        String pubDateRaw,
        Instant publishedAt,
        Instant addedAt,
        String guid,
        String sourceId,
        String sourceName,
        String sourceUrl,
        boolean seen
) {
    /**
     * Стабильный ключ (на случай, если понадобится "seen"/дедуп без БД).
     */
    public String key() {
        if (id != null) return "id:" + id;
        if (guid != null && !guid.isBlank()) return "guid:" + guid;
        if (link != null && !link.isBlank()) return "link:" + link;
        return "title:" + Objects.toString(title, "");
    }
}
