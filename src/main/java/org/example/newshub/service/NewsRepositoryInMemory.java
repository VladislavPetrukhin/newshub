package org.example.newshub.service;

import org.example.newshub.model.NewsItem;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class NewsRepositoryInMemory {

    private final List<NewsItem> news = new CopyOnWriteArrayList<>();
    private final Set<String> knownKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> seenKeys  = ConcurrentHashMap.newKeySet();

    private volatile Instant lastFetchTime;

    public Instant lastFetchTime() {
        return lastFetchTime;
    }

    public void setLastFetchTime(Instant t) {
        this.lastFetchTime = t;
    }

    public List<NewsItem> all() {
        return new ArrayList<>(news);
    }

    public int size() {
        return news.size();
    }

    public void clearSeen() {
        seenKeys.clear();
    }

    public void markSeen(Collection<NewsItem> items) {
        if (items == null) return;
        for (NewsItem it : items) {
            seenKeys.add(it.key());
        }
    }

    public boolean isSeen(NewsItem it) {
        return it != null && seenKeys.contains(it.key());
    }

    public int newCount() {
        int c = 0;
        for (NewsItem it : news) {
            if (!seenKeys.contains(it.key())) c++;
        }
        return c;
    }

    public Set<String> uniqueSourceIds() {
        Set<String> out = new LinkedHashSet<>();
        for (NewsItem it : news) {
            if (it.sourceId() != null && !it.sourceId().isBlank()) out.add(it.sourceId());
        }
        return out;
    }

    public MergeResult merge(List<NewsItem> incoming, int maxItems) {
        int added = 0;

        for (NewsItem it : incoming) {
            String key = it.key();
            if (knownKeys.add(key)) {
                news.add(it);
                added++;
            }
        }

        news.sort((a, b) -> {
            Instant ta = a.publishedAt() != null ? a.publishedAt() : a.addedAt();
            Instant tb = b.publishedAt() != null ? b.publishedAt() : b.addedAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        if (news.size() > maxItems) {
            List<NewsItem> tail = news.subList(maxItems, news.size());
            for (NewsItem it : tail) knownKeys.remove(it.key());
            tail.clear();
        }

        return new MergeResult(added);
    }

    public record MergeResult(int added) {}
}
