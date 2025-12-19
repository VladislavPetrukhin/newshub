package org.example.newshub.service;

import org.example.newshub.common.kafka.NewsBatchEvent;
import org.example.newshub.common.kafka.NewsItemPayload;
import org.example.newshub.db.NewsEntity;
import org.example.newshub.db.NewsJpaRepository;
import org.example.newshub.db.UserKeywordEntity;
import org.example.newshub.db.UserKeywordRepository;
import org.example.newshub.model.NewsItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(NewsService.class)
class NewsServiceJpaTest {

    @Autowired NewsService newsService;
    @Autowired NewsJpaRepository repo;
    @Autowired UserKeywordRepository keywordRepo;

    @MockBean FeedRegistry feeds;
    @MockBean RefreshClient refreshClient;

    @Test
    void ingest_deduplicatesByDedupKey_andSkipsExisting() {
        Instant now = Instant.parse("2025-12-18T12:00:00Z");

        var it1 = new NewsItemPayload(
                "Title", "Desc", "https://example.com/a", "Tech",
                "Wed, 18 Dec 2025 12:00:00 GMT",
                now, now,
                "guid-1",
                "src", "Source", "https://source"
        );
        var it1dup = new NewsItemPayload(
                "Title", "Desc", "https://example.com/a", "Tech",
                "Wed, 18 Dec 2025 12:00:00 GMT",
                now, now,
                "guid-1",
                "src", "Source", "https://source"
        );
        var it2 = new NewsItemPayload(
                "Other", "...", "https://example.com/b", "World",
                "Wed, 18 Dec 2025 12:01:00 GMT",
                now.plusSeconds(60), now.plusSeconds(60),
                "guid-2",
                "src", "Source", "https://source"
        );
        now = Instant.now();
        var batch = new NewsBatchEvent(
                "fetch-1",
                now,
                "src",
                "Source",
                "https://source",
                List.of(it1, it2),
                null
        );


        var r1 = newsService.ingest(batch);
        assertThat(r1.added()).isEqualTo(2);
        assertThat(repo.count()).isEqualTo(2);

        var r2 = newsService.ingest(batch);
        assertThat(r2.added()).isEqualTo(0);
        assertThat(repo.count()).isEqualTo(2);
    }

    @Test
    void list_filtersByCategory_queryAndMyKeywords_withWordBoundaries() {
        seed("linux kernel release", "New linux kernel", "Tech", "src1", "Lenta");
        seed("caterpillar", "about caterpillar", "Tech", "src1", "Lenta");
        seed("Reuters: markets", "stocks", "World", "src2", "Reuters");

        UserKeywordEntity kw = new UserKeywordEntity();
        kw.setKeyword("linux");
        keywordRepo.save(kw);

        var p = newsService.list("date", null, "Tech", null, true, 1, 100);
        assertThat(p.items()).extracting(NewsItem::title)
                .containsExactly("linux kernel release");

        var q = newsService.list("date", null, null, "reuters", false, 1, 100);
        assertThat(q.items()).extracting(NewsItem::sourceName)
                .containsExactly("Reuters");
    }

    private void seed(String title, String desc, String category, String sourceId, String sourceName) {
        NewsEntity e = new NewsEntity();
        e.setTitle(title);
        e.setDescription(desc);
        e.setCategory(category);
        e.setLink("https://example.com/" + title.replace(" ", "-"));
        e.setGuid("g-" + title.hashCode());
        e.setDedupKey("k-" + System.nanoTime());
        e.setPubDateRaw("now");
        e.setPublishedAt(Instant.now());
        e.setAddedAt(Instant.now());
        e.setSourceId(sourceId);
        e.setSourceName(sourceName);
        e.setSourceUrl("https://" + sourceId);
        e.setSeen(false);
        repo.save(e);
    }
}
