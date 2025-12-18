package org.example.newshub.web;

import org.example.newshub.common.dto.FeedDto;
import org.example.newshub.model.Feed;
import org.example.newshub.service.FeedRegistry;
import org.example.newshub.service.NewsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final FeedRegistry feedRegistry;
    private final NewsService newsService;

    public ApiController(FeedRegistry feedRegistry, NewsService newsService) {
        this.feedRegistry = feedRegistry;
        this.newsService = newsService;
    }

    @GetMapping("/news")
    public NewsService.Page list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        int safeSize = Math.max(1, Math.min(200, pageSize));
        return newsService.list(sort, source, page, safeSize);
    }

    @GetMapping("/news/search")
    public List<?> search(@RequestParam String q) {
        return newsService.search(q);
    }

    @GetMapping("/stats")
    public NewsService.Stats stats() {
        return newsService.stats();
    }

    @PostMapping("/refresh")
    public NewsService.TriggerResult refresh() {
        return newsService.triggerRefresh();
    }

    @GetMapping("/feeds")
    public FeedsResponse feeds() {
        return new FeedsResponse(feedRegistry.allFeedsInOrder(), feedRegistry.selectedIds());
    }

    @GetMapping("/feeds/selected")
    public List<FeedDto> selectedFeedsForIngestor() {
        return feedRegistry.selectedFeedsInOrder().stream()
                .map(f -> new FeedDto(f.id(), f.name(), f.url()))
                .toList();
    }

    public record FeedsResponse(List<Feed> all, java.util.Set<String> selectedIds) {}
}
