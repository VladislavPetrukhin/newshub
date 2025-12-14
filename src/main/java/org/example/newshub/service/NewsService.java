package org.example.newshub.service;

import org.example.newshub.model.Feed;
import org.example.newshub.model.NewsItem;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class  NewsService {

    private static final int MAX_NEWS = 500;

    private final FeedRegistry feeds;
    private final RssFetchService fetcher;
    private final NewsRepositoryInMemory repo;

    public NewsService(FeedRegistry feeds, RssFetchService fetcher, NewsRepositoryInMemory repo) {
        this.feeds = feeds;
        this.fetcher = fetcher;
        this.repo = repo;
    }

    public RefreshResult refresh(Duration cacheDuration) {
        Instant last = repo.lastFetchTime();
        Instant now = Instant.now();

        if (last != null) {
            Duration since = Duration.between(last, now);
            if (since.compareTo(cacheDuration) < 0) {
                long minutesLeft = Math.max(1, cacheDuration.minus(since).toMinutes());
                return RefreshResult.waitMinutes(minutesLeft);
            }
        }

        List<Feed> selectedFeeds = feeds.allFeedsInOrder().stream()
                .filter(f -> feeds.selectedIds().contains(f.id()))
                .collect(Collectors.toList());

        RssFetchService.FetchResult r = fetcher.fetch(selectedFeeds);
        int added = repo.merge(r.items(), MAX_NEWS).added();
        repo.setLastFetchTime(now);

        return RefreshResult.ok(added, r.errors());
    }

    public List<NewsItem> search(String q) {
        if (q == null || q.trim().isEmpty()) return List.of();
        String needle = q.toLowerCase(Locale.ROOT);

        return repo.all().stream()
                .filter(it ->
                        safeLower(it.title()).contains(needle) ||
                        safeLower(it.description()).contains(needle) ||
                        safeLower(it.sourceName()).contains(needle)
                )
                .collect(Collectors.toList());
    }

    public Page list(String sort, String sourceId, int page, int pageSize) {
        List<NewsItem> base = repo.all();

        if (sourceId != null && !sourceId.isBlank()) {
            base = base.stream()
                    .filter(it -> sourceId.equals(it.sourceId()))
                    .collect(Collectors.toList());
        }

        List<NewsItem> sorted = new ArrayList<>(base);
        switch (sort == null ? "" : sort) {
            case "title" -> sorted.sort(Comparator.comparing(it -> safeLower(it.title())));
            case "source" -> sorted.sort(Comparator.comparing(it -> safeLower(it.sourceName())));
            case "date" -> {
                // уже отсортировано
            }
            default -> {
                // date by default
            }
        }

        int totalItems = sorted.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages <= 0) totalPages = 1;

        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);

        List<NewsItem> pageItems = (start < end) ? sorted.subList(start, end) : List.of();

        return new Page(pageItems, safePage, totalPages, totalItems);
    }

    public Stats stats() {
        Map<String, Long> bySource = repo.all().stream()
                .collect(Collectors.groupingBy(NewsItem::sourceName, Collectors.counting()));

        return new Stats(
                repo.size(),
                feeds.allFeedsInOrder().size(),
                feeds.selectedIds().size(),
                repo.newCount(),
                repo.lastFetchTime(),
                bySource
        );
    }

    public Set<String> uniqueSourceIds() {
        return repo.uniqueSourceIds();
    }

    public void markSeen(Collection<NewsItem> items) {
        repo.markSeen(items);
    }

    public boolean isSeen(NewsItem it) {
        return repo.isSeen(it);
    }

    public void clearSeen() {
        repo.clearSeen();
    }

    private static String safeLower(String s) {
        return (s == null ? "" : s).toLowerCase(Locale.ROOT);
    }

    public record Page(List<NewsItem> items, int page, int totalPages, int totalItems) {}

    public record Stats(
            int totalNews,
            int totalFeeds,
            int selectedFeeds,
            int newCount,
            Instant lastFetchTime,
            Map<String, Long> newsBySource
    ) {}

    public sealed interface RefreshResult permits RefreshResult.Ok, RefreshResult.Wait {
        static Ok ok(int added, List<String> errors) { return new Ok(added, errors == null ? List.of() : errors); }
        static Wait waitMinutes(long minutes) { return new Wait(minutes); }

        record Ok(int added, List<String> errors) implements RefreshResult {}
        record Wait(long minutesLeft) implements RefreshResult {}
    }
}
