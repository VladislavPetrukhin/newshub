package org.example.newshub.ingestor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshScheduler.class);

    private final IngestorService ingestor;
    private final boolean enabled;

    public RefreshScheduler(
            IngestorService ingestor,
            @Value("${newshub.refresh.schedule.enabled:false}") boolean enabled
    ) {
        this.ingestor = ingestor;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${newshub.refresh.schedule.fixed-delay-seconds:300}000")
    public void tick() {
        if (!enabled) return;
        var r = ingestor.refreshOnce();
        if (!r.ok()) {
            log.warn("Scheduled refresh failed: {}", r.error());
        } else {
            log.info("Scheduled refresh done: fetchId={}, batches={}", r.fetchId(), r.batches());
        }
    }
}
