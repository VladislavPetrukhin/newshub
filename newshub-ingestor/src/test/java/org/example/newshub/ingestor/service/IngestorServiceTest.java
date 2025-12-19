package org.example.newshub.ingestor.service;

import org.example.newshub.common.dto.FeedDto;
import org.example.newshub.common.kafka.NewsBatchEvent;
import org.example.newshub.common.kafka.NewsItemPayload;
import org.example.newshub.ingestor.kafka.NewsKafkaPublisher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IngestorServiceTest {

    @Test
    void refreshOnce_counts_ok_and_error_batches_and_publishes_all() {
        FeedApiClient feedApi = mock(FeedApiClient.class);
        RssFetchService fetch = mock(RssFetchService.class);
        NewsKafkaPublisher publisher = mock(NewsKafkaPublisher.class);

        IngestorService service = new IngestorService(feedApi, fetch, publisher);

        List<FeedDto> feeds = List.of(
                new FeedDto("a", "A", "https://a/rss"),
                new FeedDto("b", "B", "https://b/rss")
        );
        when(feedApi.selectedFeeds()).thenReturn(feeds);

        Instant now = Instant.parse("2025-12-18T12:00:00Z");
        List<NewsItemPayload> items = List.of(
                new NewsItemPayload(
                        "Title", "Desc", "https://example.com/a", "Cat",
                        "Wed, 18 Dec 2025 12:00:00 GMT",
                        now,
                        now,
                        "guid-1",
                        "src",
                        "Source",
                        "https://source"
                )
        );

        NewsBatchEvent okBatch = new NewsBatchEvent(
                "fetch-1",
                now,
                "src",
                "Source",
                "https://source",
                items,
                null
        );


        NewsBatchEvent errBatch = new NewsBatchEvent(
                "fetch-1",
                now,
                "src",
                "Source",
                "https://source",
                List.of(),
                "boom"
        );



        when(fetch.fetch(anyString(), eq(feeds))).thenReturn(List.of(okBatch, errBatch));

        IngestorService.RefreshRun run = service.refreshOnce();

        assertThat(run.batches()).isEqualTo(2);
        assertThat(run.ok()).isTrue();

        verify(feedApi).selectedFeeds();
        verify(fetch).fetch(anyString(), eq(feeds));
        verify(publisher).publish(okBatch);
        verify(publisher).publish(errBatch);
    }
}
