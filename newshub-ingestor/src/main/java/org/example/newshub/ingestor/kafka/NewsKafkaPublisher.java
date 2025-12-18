package org.example.newshub.ingestor.kafka;

import org.example.newshub.common.kafka.NewsBatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NewsKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(NewsKafkaPublisher.class);

    private final KafkaTemplate<String, NewsBatchEvent> kafkaTemplate;
    private final String topic;

    public NewsKafkaPublisher(
            KafkaTemplate<String, NewsBatchEvent> kafkaTemplate,
            @Value("${newshub.kafka.topics.news:newshub.news}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(NewsBatchEvent batch) {
        if (batch == null) return;
        String key = batch.sourceId() == null ? "unknown" : batch.sourceId();
        kafkaTemplate.send(topic, key, batch);
        if (!batch.ok()) {
            log.warn("Published error batch for {}: {}", key, batch.error());
        } else {
            log.info("Published {} items for {} (fetchId={})", batch.items() == null ? 0 : batch.items().size(), key, batch.fetchId());
        }
    }
}
