package org.example.newshub.ingestor.service;

import org.example.newshub.common.dto.FeedDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class FeedApiClient {

    private final RestClient rest;

    public FeedApiClient(
            @Value("${newshub.api.base-url:http://localhost:8080}") String baseUrl,
            RestClient.Builder builder
    ) {
        this.rest = builder.baseUrl(baseUrl).build();
    }

    public List<FeedDto> selectedFeeds() {
        return rest.get()
                .uri("/api/feeds/selected")
                .retrieve()
                .body(new ParameterizedTypeReference<List<FeedDto>>() {});
    }
}
