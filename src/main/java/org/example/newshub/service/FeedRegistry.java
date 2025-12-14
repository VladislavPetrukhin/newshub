package org.example.newshub.service;

import org.example.newshub.model.Feed;
import org.example.newshub.model.FeedCategory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class FeedRegistry {

    private final Map<String, Feed> feedsById = new ConcurrentHashMap<>();
    private final List<String> feedOrder = new CopyOnWriteArrayList<>();

    private final Set<String> selected = ConcurrentHashMap.newKeySet();

    public FeedRegistry() {
        initDefaults();

        // дефолтные выбранные
        selected.addAll(List.of("lenta", "ria", "bbc", "rt"));
    }

    public List<Feed> allFeedsInOrder() {
        List<Feed> out = new ArrayList<>();
        for (String id : feedOrder) {
            Feed f = feedsById.get(id);
            if (f != null) out.add(f);
        }
        return out;
    }

    public Optional<Feed> findById(String id) {
        return Optional.ofNullable(feedsById.get(id));
    }

    public Set<String> selectedIds() {
        return new LinkedHashSet<>(selected);
    }

    public void updateSelected(List<String> ids) {
        selected.clear();
        if (ids != null) selected.addAll(ids);
    }

    public Feed addCustom(String name, String url) {
        String id = "custom_" + System.currentTimeMillis();
        Feed f = new Feed(id, name, url, FeedCategory.CUSTOM);
        addFeed(f);
        selected.add(id);
        return f;
    }

    private void addFeed(Feed feed) {
        feedsById.put(feed.id(), feed);
        feedOrder.add(feed.id());
    }

    private void initDefaults() {
        // российские
        addFeed(new Feed("lenta", "Лента.ру", "https://lenta.ru/rss", FeedCategory.RUSSIAN));
        addFeed(new Feed("ria", "РИА Новости", "https://ria.ru/export/rss2/index.xml", FeedCategory.RUSSIAN));
        addFeed(new Feed("rt", "RT", "https://russian.rt.com/rss", FeedCategory.RUSSIAN));
        addFeed(new Feed("tass", "ТАСС", "https://tass.ru/rss/v2.xml", FeedCategory.RUSSIAN));
        addFeed(new Feed("kommersant", "Коммерсантъ", "https://www.kommersant.ru/RSS/news.xml", FeedCategory.RUSSIAN));
        addFeed(new Feed("rbc", "РБК", "https://rssexport.rbc.ru/rbcnews/news/30/full.rss", FeedCategory.RUSSIAN));
        addFeed(new Feed("vedomosti", "Ведомости", "https://www.vedomosti.ru/rss/news", FeedCategory.RUSSIAN));
        addFeed(new Feed("mk", "Московский комсомолец", "https://www.mk.ru/rss/index.xml", FeedCategory.RUSSIAN));
        addFeed(new Feed("gazeta", "Газета.Ru", "https://www.gazeta.ru/export/rss/lenta.xml", FeedCategory.RUSSIAN));
        addFeed(new Feed("meduza", "Meduza", "https://meduza.io/rss2/all", FeedCategory.RUSSIAN));
        addFeed(new Feed("fontanka", "Фонтанка.ру", "https://www.fontanka.ru/fontanka.rss", FeedCategory.RUSSIAN));
        addFeed(new Feed("interfax", "Интерфакс", "http://www.interfax.ru/rss.asp", FeedCategory.RUSSIAN));
        addFeed(new Feed("rg", "Российская газета", "https://rg.ru/xml/index.xml", FeedCategory.RUSSIAN));
        addFeed(new Feed("kp", "Комсомольская правда", "https://www.kp.ru/rss/allsections.xml", FeedCategory.RUSSIAN));
        addFeed(new Feed("vz", "Взгляд", "https://vz.ru/rss.xml", FeedCategory.RUSSIAN));

        // международные
        addFeed(new Feed("bbc", "BBC News", "https://feeds.bbci.co.uk/news/rss.xml", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("nytimes", "New York Times", "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("reuters", "Reuters", "http://feeds.reuters.com/reuters/topNews", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("cnn", "CNN", "http://rss.cnn.com/rss/edition.rss", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("npr", "NPR", "https://feeds.npr.org/1001/rss.xml", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("dw", "Deutsche Welle", "https://rss.dw.com/rdf/rss-en-top", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("aljazeera", "Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("bloomberg", "Bloomberg", "https://www.bloomberg.com/feeds/podcasts/etf_report.xml", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("ft", "Financial Times", "https://www.ft.com/rss/home", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("wsj", "Wall Street Journal", "https://feeds.a.dj.com/rss/RSSWorldNews.xml", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("guardian", "The Guardian", "https://www.theguardian.com/world/rss", FeedCategory.INTERNATIONAL));
        addFeed(new Feed("euronews", "Euronews", "https://www.euronews.com/rss", FeedCategory.INTERNATIONAL));

        // Беларусь
        addFeed(new Feed("belta", "БЕЛТА", "https://www.belta.by/rss/main", FeedCategory.BELARUS));
        addFeed(new Feed("tut", "TUT.BY", "https://news.tut.by/rss/index.rss", FeedCategory.BELARUS));

        // Казахстан
        addFeed(new Feed("kazinform", "Казинформ", "https://www.inform.kz/rss/ru", FeedCategory.KAZAKHSTAN));
        addFeed(new Feed("tengrinews", "Tengrinews", "https://tengrinews.kz/rss/", FeedCategory.KAZAKHSTAN));
    }
}
