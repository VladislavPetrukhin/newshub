package org.example.newshub.service;

import org.example.newshub.db.NewsEntity;
import org.example.newshub.db.NewsJpaRepository;
import org.example.newshub.model.Feed;
import org.example.newshub.model.NewsItem;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private static final int MAX_NEWS = 500;

    private final FeedRegistry feeds;
    private final RssFetchService fetcher;
    private final NewsJpaRepository repo;

    // кэш-таймер обновления — просто in-memory (для лабы достаточно)
    private volatile Instant lastFetchTime;

    public NewsService(FeedRegistry feeds, RssFetchService fetcher, NewsJpaRepository repo) {
        this.feeds = feeds;
        this.fetcher = fetcher;
        this.repo = repo;
    }

    public RefreshResult refresh(Duration cacheDuration) {
        Instant now = Instant.now();

        if (lastFetchTime != null) {
            Duration since = Duration.between(lastFetchTime, now);
            if (since.compareTo(cacheDuration) < 0) {
                long minutesLeft = Math.max(1, cacheDuration.minus(since).toMinutes());
                return RefreshResult.waitMinutes(minutesLeft);
            }
        }

        List<Feed> selectedFeeds = feeds.allFeedsInOrder().stream()
                .filter(f -> feeds.selectedIds().contains(f.id()))
                .toList();

        RssFetchService.FetchResult r = fetcher.fetch(selectedFeeds);
        int added = saveFresh(r.items());

        lastFetchTime = now;

        return RefreshResult.ok(added, r.errors());
    }

    @Transactional
    protected int saveFresh(List<NewsItem> incoming) {
        if (incoming == null || incoming.isEmpty()) return 0;

        // дедуп внутри одного батча, чтобы не стрельнуть себе в ногу
        Set<String> batchKeys = new HashSet<>();
        List<NewsEntity> toSave = new ArrayList<>();

        for (NewsItem it : incoming) {
            String key = DedupKey.of(it);
            if (!batchKeys.add(key)) continue;          // дубль в батче
            if (repo.existsByDedupKey(key)) continue;   // уже в БД

            NewsEntity e = new NewsEntity();
            e.setTitle(it.title());
            e.setDescription(it.description());
            e.setLink(it.link());
            e.setGuid(it.guid());
            e.setDedupKey(key);
            e.setPubDateRaw(it.pubDateRaw());
            e.setPublishedAt(it.publishedAt());
            e.setAddedAt(it.addedAt() != null ? it.addedAt() : Instant.now());
            e.setSourceId(it.sourceId());
            e.setSourceName(it.sourceName());
            e.setSourceUrl(it.sourceUrl());
            e.setSeen(false);

            toSave.add(e);
        }

        if (!toSave.isEmpty()) {
            repo.saveAll(toSave);
        }

        trimToMax();

        return toSave.size();
    }

    @Transactional
    protected void trimToMax() {
        long total = repo.count();
        if (total <= MAX_NEWS) return;

        int overflow = (int) Math.min(Integer.MAX_VALUE, total - MAX_NEWS);
        // берём id самых старых и удаляем
        List<Long> ids = repo.findOldestIds(PageRequest.of(0, overflow));
        if (!ids.isEmpty()) {
            repo.deleteByIds(ids);
        }
    }

    public List<NewsItem> search(String q) {
        if (q == null || q.trim().isEmpty()) return List.of();

        var p = repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrSourceNameContainingIgnoreCase(
                q, q, q,
                PageRequest.of(0, 200, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("addedAt")))
        );

        return p.getContent().stream().map(NewsService::toDto).toList();
    }

    public Page list(String sort, String sourceId, int page, int pageSize) {
        Sort s = switch (sort == null ? "" : sort) {
            case "title" -> Sort.by(Sort.Order.asc("title"), Sort.Order.desc("addedAt"));
            case "source" -> Sort.by(Sort.Order.asc("sourceName"), Sort.Order.desc("addedAt"));
            case "date", "" -> Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("addedAt"));
            default -> Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("addedAt"));
        };

        int safePage = Math.max(1, page);
        var pageable = PageRequest.of(safePage - 1, pageSize, s);

        var p = (sourceId != null && !sourceId.isBlank())
                ? repo.findBySourceId(sourceId, pageable)
                : repo.findAll(pageable);

        List<NewsItem> items = p.getContent().stream().map(NewsService::toDto).toList();

        return new Page(items, safePage, Math.max(1, p.getTotalPages()), (int) p.getTotalElements());
    }

    public Stats stats() {
        // максимум 500 элементов — можно сгруппировать в памяти без боли
        Map<String, Long> bySource = repo.findAll().stream()
                .collect(Collectors.groupingBy(
                        e -> Optional.ofNullable(e.getSourceName()).orElse("неизвестно"),
                        Collectors.counting()
                ));

        return new Stats(
                (int) repo.count(),
                feeds.allFeedsInOrder().size(),
                feeds.selectedIds().size(),
                (int) repo.countUnseen(),
                lastFetchTime,
                bySource
        );
    }

    public Set<String> uniqueSourceIds() {
        return new LinkedHashSet<>(repo.distinctSourceIds());
    }

    @Transactional
    public void markSeen(Collection<NewsItem> items) {
        if (items == null || items.isEmpty()) return;
        List<Long> ids = items.stream()
                .map(NewsItem::id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!ids.isEmpty()) {
            repo.markSeen(ids);
        }
    }

    @Transactional
    public void clearSeen() {
        repo.markAllUnseen();
    }

    private static NewsItem toDto(NewsEntity e) {
        return new NewsItem(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getLink(),
                e.getPubDateRaw(),
                e.getPublishedAt(),
                e.getAddedAt(),
                e.getGuid(),
                e.getSourceId(),
                e.getSourceName(),
                e.getSourceUrl(),
                e.isSeen()
        );
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
