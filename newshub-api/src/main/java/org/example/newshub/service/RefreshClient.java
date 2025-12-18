package org.example.newshub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RefreshClient {

    private final RestClient rest;

    public RefreshClient(
            @Value("${newshub.ingestor.base-url:http://localhost:8081}") String baseUrl,
            RestClient.Builder builder
    ) {
        this.rest = builder.baseUrl(baseUrl).build();
    }

    public NewsService.TriggerResult triggerRefresh() {
        try {
            var resp = rest.post()
                    .uri("/internal/refresh")
                    .retrieve()
                    .toBodilessEntity();

            HttpStatusCode st = resp.getStatusCode();
            if (st.is2xxSuccessful()) {
                return new NewsService.TriggerResult(true, "refresh requested");
            }
            return new NewsService.TriggerResult(false, "ingestor returned " + st);
        } catch (Exception ex) {
            return new NewsService.TriggerResult(false, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }
}
