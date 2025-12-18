package org.example.newshub.web;

import org.example.newshub.service.FeedRegistry;
import org.example.newshub.service.KeywordService;
import org.example.newshub.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Controller
public class NewsController {

    private static final int PAGE_SIZE = 99;

    private final FeedRegistry feedRegistry;
    private final NewsService newsService;
    private final HtmlRenderer renderer;
    private final KeywordService keywordService;

    public NewsController(FeedRegistry feedRegistry, NewsService newsService, HtmlRenderer renderer,
                          KeywordService keywordService) {
        this.feedRegistry = feedRegistry;
        this.newsService = newsService;
        this.renderer = renderer;
        this.keywordService = keywordService;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String index(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(name = "mykw", defaultValue = "0") int mykw
    ) {
        source = (source == null || source.isBlank()) ? null : source;
        category = (category == null || category.isBlank()) ? null : category;
        q = (q == null || q.isBlank()) ? null : q;

        boolean useMyKeywords = (mykw == 1);

        NewsService.Page p = newsService.list(sort, source, category, q, useMyKeywords, page, PAGE_SIZE);
        List<String> categories = newsService.distinctCategories();
        String html = renderer.renderIndex(p, sort, source, category, q, useMyKeywords, categories);

        newsService.markSeen(p.items());
        return html;
    }


    @PostMapping("/fetch")
    @ResponseBody
    public ResponseEntity<Void> fetch() {
        newsService.keepOnlySelectedSources();
        newsService.triggerRefresh();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("/?sort=date"))
                .build();
    }



    @GetMapping(value = "/search", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String search(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) return "<script>location.href='/'</script>";
        return renderer.renderSearch(q, newsService.search(q));
    }

    @GetMapping(value = "/stats", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String stats() {
        return renderer.renderStats(newsService.stats());
    }

    @GetMapping(value = "/select-sources", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String selectSources() {
        return renderer.renderSelectSources();
    }

    @PostMapping(value = "/update-sources", produces = MediaType.TEXT_HTML_VALUE)
    public String updateSources(@RequestParam(value = "source", required = false) List<String> sources) {
        feedRegistry.updateSelected(sources);
        newsService.keepOnlySelectedSources();
        return "redirect:/";
    }

    @GetMapping(value = "/add-custom-feed", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String addCustomFeedPage() {
        return renderer.renderAddCustomFeedPage();
    }

    @PostMapping(value = "/add-custom-feed", produces = MediaType.TEXT_HTML_VALUE)
    public String addCustomFeed(@RequestParam String name, @RequestParam String url) {
        feedRegistry.addCustom(name, url);
        return "redirect:/select-sources";
    }

    @GetMapping(value = "/clear-cache", produces = MediaType.TEXT_HTML_VALUE)
    public String clearCache() {
        newsService.clearSeen();
        return "redirect:/?notify";
    }
    @GetMapping(value="/keywords", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String keywords() {
        return renderer.renderKeywords(keywordService.list());
    }

    @PostMapping("/keywords")
    public String addKeywords(@RequestParam String keywords) {
        keywordService.addMany(keywords);
        return "redirect:/keywords";
    }

    @PostMapping("/keywords/delete")
    public String deleteKeyword(@RequestParam Long id) {
        keywordService.delete(id);
        return "redirect:/keywords";
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
