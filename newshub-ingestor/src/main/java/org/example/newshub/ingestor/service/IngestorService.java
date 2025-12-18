package org.example.newshub.ingestor.service;

import org.example.newshub.common.dto.FeedDto;
import org.example.newshub.common.kafka.NewsBatchEvent;
import org.example.newshub.ingestor.kafka.NewsKafkaPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IngestorService {

    private static final Logger log = LoggerFactory.getLogger(IngestorService.class);

    private final FeedApiClient feedApiClient;
    private final RssFetchService fetchService;
    private final NewsKafkaPublisher publisher;

    public IngestorService(FeedApiClient feedApiClient, RssFetchService fetchService, NewsKafkaPublisher publisher) {
        this.feedApiClient = feedApiClient;
        this.fetchService = fetchService;
        this.publisher = publisher;
    }

    public RefreshRun refreshOnce() {
        String fetchId = UUID.randomUUID().toString();

        List<FeedDto> feeds;
        try {
            feeds = feedApiClient.selectedFeeds();
        } catch (Exception ex) {
            log.error("Failed to get selected feeds from API", ex);
            return new RefreshRun(fetchId, 0, "API error: " + ex.getMessage());
        }

        List<NewsBatchEvent> batches = fetchService.fetch(fetchId, feeds);
        for (NewsBatchEvent b : batches) {
            publisher.publish(b);
        }

        return new RefreshRun(fetchId, batches.size(), null);
    }

    public record RefreshRun(String fetchId, int batches, String error) {
        public boolean ok() { return error == null || error.isBlank(); }
    }
}
