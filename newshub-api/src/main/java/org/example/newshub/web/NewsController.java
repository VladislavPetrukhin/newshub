package org.example.newshub.web;

import org.example.newshub.service.FeedRegistry;
import org.example.newshub.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
public class NewsController {

    private static final int PAGE_SIZE = 10;

    private final FeedRegistry feedRegistry;
    private final NewsService newsService;
    private final HtmlRenderer renderer;

    public NewsController(FeedRegistry feedRegistry, NewsService newsService, HtmlRenderer renderer) {
        this.feedRegistry = feedRegistry;
        this.newsService = newsService;
        this.renderer = renderer;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(required = false) String source
    ) {
        NewsService.Page p = newsService.list(sort, source, page, PAGE_SIZE);
        String html = renderer.renderIndex(p, sort, source);
        newsService.markSeen(p.items());
        return html;
    }

    @PostMapping("/fetch")
    public ResponseEntity<Void> fetch() {
        newsService.keepOnlySelectedSources();
        newsService.triggerRefresh();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("/?sort=date"))
                .build();
    }



    @GetMapping(value = "/search", produces = MediaType.TEXT_HTML_VALUE)
    public String search(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) return "<script>location.href='/'</script>";
        return renderer.renderSearch(q, newsService.search(q));
    }

    @GetMapping(value = "/stats", produces = MediaType.TEXT_HTML_VALUE)
    public String stats() {
        return renderer.renderStats(newsService.stats());
    }

    @GetMapping(value = "/select-sources", produces = MediaType.TEXT_HTML_VALUE)
    public String selectSources() {
        return renderer.renderSelectSources();
    }

    @PostMapping(value = "/update-sources", produces = MediaType.TEXT_HTML_VALUE)
    public String updateSources(@RequestParam(value = "source", required = false) List<String> sources) {
        feedRegistry.updateSelected(sources);
        newsService.keepOnlySelectedSources();
        return "<script>alert('выбор источников сохранен'); location.href='/';</script>";
    }

    @GetMapping(value = "/add-custom-feed", produces = MediaType.TEXT_HTML_VALUE)
    public String addCustomFeedPage() {
        return renderer.renderAddCustomFeedPage();
    }

    @PostMapping(value = "/add-custom-feed", produces = MediaType.TEXT_HTML_VALUE)
    public String addCustomFeed(@RequestParam String name, @RequestParam String url) {
        feedRegistry.addCustom(name, url);
        return "<script>alert('источник добавлен'); location.href='/select-sources';</script>";
    }

    @GetMapping(value = "/clear-cache", produces = MediaType.TEXT_HTML_VALUE)
    public String clearCache() {
        newsService.clearSeen();
        return "<script>location.href='/?notify=просмотренные очищены'</script>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
