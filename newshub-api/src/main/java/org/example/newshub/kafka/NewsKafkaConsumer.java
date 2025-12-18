package org.example.newshub.kafka;

import org.example.newshub.common.kafka.NewsBatchEvent;
import org.example.newshub.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NewsKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NewsKafkaConsumer.class);

    private final NewsService newsService;

    public NewsKafkaConsumer(NewsService newsService) {
        this.newsService = newsService;
    }

    @KafkaListener(
            topics = "${newshub.kafka.topics.news:newshub.news}",
            groupId = "${newshub.kafka.consumer-group:newshub-api}"
    )
    public void onBatch(NewsBatchEvent batch) {
        NewsService.IngestResult r = newsService.ingest(batch);
        if (batch != null && !batch.ok()) {
            log.warn("Fetch error from {}: {}", batch.sourceId(), batch.error());
        } else {
            log.info("Ingested {} items from {} (fetchId={})", r.added(), batch == null ? "?" : batch.sourceId(), batch == null ? "?" : batch.fetchId());
        }
    }
}
