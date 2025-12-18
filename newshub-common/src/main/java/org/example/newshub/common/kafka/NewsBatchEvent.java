package org.example.newshub.common.kafka;

import java.time.Instant;
import java.util.List;

public record NewsBatchEvent(
        String fetchId,
        Instant fetchedAt,
        String sourceId,
        String sourceName,
        String sourceUrl,
        List<NewsItemPayload> items,
        String error
) {
    public boolean ok() {
        return error == null || error.isBlank();
    }
}
