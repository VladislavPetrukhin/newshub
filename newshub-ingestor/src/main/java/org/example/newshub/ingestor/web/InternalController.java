package org.example.newshub.ingestor.web;

import org.example.newshub.ingestor.service.IngestorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalController {

    private final IngestorService ingestor;

    public InternalController(IngestorService ingestor) {
        this.ingestor = ingestor;
    }

    @PostMapping("/internal/refresh")
    public IngestorService.RefreshRun refresh() {
        return ingestor.refreshOnce();
    }
}
