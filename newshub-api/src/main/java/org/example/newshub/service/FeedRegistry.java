package org.example.newshub.service;

import org.example.newshub.db.FeedEntity;
import org.example.newshub.db.FeedRepository;
import org.example.newshub.model.Feed;
import org.example.newshub.model.FeedCategory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FeedRegistry {

    private final FeedRepository repo;

    private final Map<String, Feed> feedsById = new LinkedHashMap<>();
    private final List<String> feedOrder = new ArrayList<>();
    private final Set<String> selected = new LinkedHashSet<>();

    private static final Set<String> DEFAULT_SELECTED = Set.of("lenta", "ria", "bbc", "rt");

    public FeedRegistry(FeedRepository repo) {
        this.repo = repo;

        initDefaultsInMemory();
        ensureDefaultsInDb();
        loadFromDb();
    }


    public synchronized List<Feed> allFeedsInOrder() {
        return feedOrder.stream().map(feedsById::get).filter(Objects::nonNull).toList();
    }

    public synchronized List<Feed> selectedFeedsInOrder() {
        return feedOrder.stream()
                .filter(selected::contains)
                .map(feedsById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public synchronized Set<String> selectedIds() {
        return new LinkedHashSet<>(selected);
    }

    public synchronized Optional<Feed> findById(String id) {
        return Optional.ofNullable(feedsById.get(id));
    }

    public synchronized void updateSelected(List<String> ids) {
        selected.clear();
        if (ids != null) selected.addAll(ids);

        List<FeedEntity> all = repo.findAll();
        boolean changed = false;

        for (FeedEntity e : all) {
            boolean shouldBeSelected = selected.contains(e.getId());
            if (e.isSelected() != shouldBeSelected) {
                e.setSelected(shouldBeSelected);
                changed = true;
            }
        }

        if (changed) repo.saveAll(all);
    }

    public synchronized void addCustom(String name, String url) {
        String id = "custom_" + System.currentTimeMillis();

        FeedEntity e = new FeedEntity();
        e.setId(id);
        e.setName(name);
        e.setUrl(url);
        e.setCategory(FeedCategory.CUSTOM);
        e.setSelected(true);
        e.setCreatedAt(Instant.now());
        repo.save(e);

        addFeedInternal(new Feed(id, name, url, FeedCategory.CUSTOM));
        selected.add(id);
    }


    private void loadFromDb() {
        selected.clear();

        for (FeedEntity e : repo.findAllByOrderByCreatedAtAsc()) {
            if (!feedsById.containsKey(e.getId())) {
                addFeedInternal(new Feed(e.getId(), e.getName(), e.getUrl(), e.getCategory()));
            }

            if (e.isSelected()) selected.add(e.getId());
        }
    }

    private void ensureDefaultsInDb() {
        Map<String, FeedEntity> existing = repo.findAll().stream()
                .collect(Collectors.toMap(FeedEntity::getId, Function.identity(), (a, b) -> a));

        List<FeedEntity> toSave = new ArrayList<>();

        Instant base = Instant.now();
        int i = 0;

        for (String id : feedOrder) {
            Feed f = feedsById.get(id);
            if (f == null) continue;

            if (!existing.containsKey(id)) {
                FeedEntity e = new FeedEntity();
                e.setId(f.id());
                e.setName(f.name());
                e.setUrl(f.url());
                e.setCategory(f.category());
                e.setSelected(DEFAULT_SELECTED.contains(f.id()));
                e.setCreatedAt(base.plusMillis(i++));
                toSave.add(e);
            }
        }

        if (!toSave.isEmpty()) repo.saveAll(toSave);
    }

    private void addFeedInternal(Feed feed) {
        if (feedsById.containsKey(feed.id())) return;
        feedsById.put(feed.id(), feed);
        feedOrder.add(feed.id());
    }

    private void initDefaultsInMemory() {
        // RUS
        addFeedInternal(new Feed("lenta", "Лента.ру", "https://lenta.ru/rss", FeedCategory.RUS));
        addFeedInternal(new Feed("ria", "РИА Новости", "https://ria.ru/export/rss2/index.xml", FeedCategory.RUS));
        addFeedInternal(new Feed("rt", "RT", "https://russian.rt.com/rss", FeedCategory.RUS));
        addFeedInternal(new Feed("tass", "ТАСС", "https://tass.ru/rss/v2.xml", FeedCategory.RUS));
        addFeedInternal(new Feed("kommersant", "Коммерсантъ", "https://www.kommersant.ru/RSS/news.xml", FeedCategory.RUS));
        addFeedInternal(new Feed("rbc", "РБК", "https://rssexport.rbc.ru/rbcnews/news/30/full.rss", FeedCategory.RUS));
        addFeedInternal(new Feed("vedomosti", "Ведомости", "https://www.vedomosti.ru/rss/news", FeedCategory.RUS));
        addFeedInternal(new Feed("mk", "Московский комсомолец", "https://www.mk.ru/rss/index.xml", FeedCategory.RUS));
        addFeedInternal(new Feed("gazeta", "Газета.Ru", "https://www.gazeta.ru/export/rss/lenta.xml", FeedCategory.RUS));
        addFeedInternal(new Feed("meduza", "Meduza", "https://meduza.io/rss2/all", FeedCategory.RUS));
        addFeedInternal(new Feed("fontanka", "Фонтанка.ру", "https://www.fontanka.ru/fontanka.rss", FeedCategory.RUS));
        addFeedInternal(new Feed("sport_express", "Спорт-Экспресс", "https://www.sport-express.ru/services/materials/news/se/", FeedCategory.RUS));
        addFeedInternal(new Feed("interfax", "Интерфакс", "http://www.interfax.ru/rss.asp", FeedCategory.RUS));
        addFeedInternal(new Feed("rg", "Российская газета", "https://rg.ru/xml/index.xml", FeedCategory.RUS));
        addFeedInternal(new Feed("kp", "Комсомольская правда", "https://www.kp.ru/rss/allsections.xml", FeedCategory.RUS));
        addFeedInternal(new Feed("ai", "Афтершок", "https://www.aftershock.news/rss.xml", FeedCategory.RUS));
        addFeedInternal(new Feed("vz", "Взгляд", "https://vz.ru/rss.xml", FeedCategory.RUS));

        // INTL
        addFeedInternal(new Feed("bbc", "BBC News", "https://feeds.bbci.co.uk/news/rss.xml", FeedCategory.INTL));
        addFeedInternal(new Feed("nytimes", "New York Times", "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml", FeedCategory.INTL));
        addFeedInternal(new Feed("reuters", "Reuters", "http://feeds.reuters.com/reuters/topNews", FeedCategory.INTL));
        addFeedInternal(new Feed("cnn", "CNN", "http://rss.cnn.com/rss/edition.rss", FeedCategory.INTL));
        addFeedInternal(new Feed("npr", "NPR", "https://feeds.npr.org/1001/rss.xml", FeedCategory.INTL));
        addFeedInternal(new Feed("dw", "Deutsche Welle", "https://rss.dw.com/rdf/rss-en-top", FeedCategory.INTL));
        addFeedInternal(new Feed("aljazeera", "Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml", FeedCategory.INTL));
        addFeedInternal(new Feed("bloomberg", "Bloomberg", "https://www.bloomberg.com/feeds/podcasts/etf_report.xml", FeedCategory.INTL));
        addFeedInternal(new Feed("ft", "Financial Times", "https://www.ft.com/rss/home", FeedCategory.INTL));
        addFeedInternal(new Feed("wsj", "Wall Street Journal", "https://feeds.a.dj.com/rss/RSSWorldNews.xml", FeedCategory.INTL));
        addFeedInternal(new Feed("guardian", "The Guardian", "https://www.theguardian.com/world/rss", FeedCategory.INTL));
        addFeedInternal(new Feed("euronews", "Euronews", "https://www.euronews.com/rss", FeedCategory.INTL));

        // BY
        addFeedInternal(new Feed("belta", "БЕЛТА", "https://www.belta.by/rss/main", FeedCategory.BY));
        addFeedInternal(new Feed("tut", "TUT.BY", "https://news.tut.by/rss/index.rss", FeedCategory.BY));

        // KZ
        addFeedInternal(new Feed("kazinform", "Казинформ", "https://www.inform.kz/rss/ru", FeedCategory.KZ));
        addFeedInternal(new Feed("tengrinews", "Tengrinews", "https://tengrinews.kz/rss/", FeedCategory.KZ));
    }
}
