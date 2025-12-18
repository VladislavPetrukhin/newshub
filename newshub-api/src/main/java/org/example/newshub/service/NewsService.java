package org.example.newshub.service;

import org.example.newshub.common.kafka.NewsBatchEvent;
import org.example.newshub.common.kafka.NewsItemPayload;
import org.example.newshub.db.NewsEntity;
import org.example.newshub.db.NewsJpaRepository;
import org.example.newshub.model.NewsItem;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private static final int MAX_NEWS = 500;
    private static final int MAX_ERRORS = 25;

    private final FeedRegistry feeds;
    private final NewsJpaRepository repo;
    private final RefreshClient refreshClient;

    private volatile Instant lastFetchTime;
    private final Deque<String> lastErrors = new ConcurrentLinkedDeque<>();

    public NewsService(FeedRegistry feeds, NewsJpaRepository repo, RefreshClient refreshClient) {
        this.feeds = feeds;
        this.repo = repo;
        this.refreshClient = refreshClient;
    }

    public TriggerResult triggerRefresh() {
        return refreshClient.triggerRefresh();
    }

    @Transactional
    public IngestResult ingest(NewsBatchEvent batch) {
        if (batch == null) return new IngestResult(0, List.of());

        if (!batch.ok()) {
            pushError(batch.sourceName() + " (" + batch.sourceUrl() + "): " + batch.error());
            this.lastFetchTime = batch.fetchedAt();
            return new IngestResult(0, List.of(batch.error()));
        }

        int added = saveFresh(batch.items());
        this.lastFetchTime = batch.fetchedAt();
        return new IngestResult(added, List.of());
    }

    private void pushError(String msg) {
        if (msg == null || msg.isBlank()) return;
        lastErrors.addFirst(msg);
        while (lastErrors.size() > MAX_ERRORS) lastErrors.removeLast();
    }

    public List<String> lastErrors() {
        return List.copyOf(lastErrors);
    }

    @Transactional
    protected int saveFresh(List<NewsItemPayload> incoming) {
        if (incoming == null || incoming.isEmpty()) return 0;

        Set<String> batchKeys = new HashSet<>();
        List<NewsEntity> toSave = new ArrayList<>();

        for (NewsItemPayload it : incoming) {
            String key = DedupKey.of(it);
            if (!batchKeys.add(key)) continue;
            if (repo.existsByDedupKey(key)) continue;

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

        if (!toSave.isEmpty()) repo.saveAll(toSave);
        trimToMax();

        return toSave.size();
    }

    @Transactional
    protected void trimToMax() {
        long total = repo.count();
        if (total <= MAX_NEWS) return;

        int overflow = (int) Math.min(Integer.MAX_VALUE, total - MAX_NEWS);
        List<Long> ids = repo.findOldestIds(PageRequest.of(0, overflow));
        if (!ids.isEmpty()) repo.deleteByIds(ids);
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
                bySource,
                lastErrors()
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
        if (!ids.isEmpty()) repo.markSeen(ids);
    }

    @Transactional
    public void clearSeen() {
        repo.markAllUnseen();
    }

    @Transactional
    public void keepOnlySelectedSources() {
        Set<String> keep = feeds.selectedIds();
        if (keep == null || keep.isEmpty()) {
            repo.deleteAll();
        } else {
            repo.deleteBySourceIdNotIn(keep);
        }
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
            Map<String, Long> newsBySource,
            List<String> lastErrors
    ) {}

    public record TriggerResult(boolean ok, String message) {}

    public record IngestResult(int added, List<String> errors) {}

    public Optional<String> lookupSourceName(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) return Optional.empty();
        return Optional.ofNullable(repo.findAnySourceName(sourceId))
                .filter(s -> !s.isBlank());
    }

    public record SourceInfo(String id, String name) {}

    public List<SourceInfo> activeSources() {
        return repo.distinctSourcesWithNames().stream()
                .map(r -> new SourceInfo(
                        r.getId(),
                        (r.getName() == null || r.getName().isBlank()) ? r.getId() : r.getName()
                ))
                .sorted(Comparator.comparing(SourceInfo::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
