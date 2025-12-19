package org.example.newshub.ingestor.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.example.newshub.common.dto.FeedDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeedApiClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void selectedFeeds_parses_json_array() {
        server.enqueue(new MockResponse()
                .setBody("[" +
                        "{\"id\":\"a\",\"name\":\"A\",\"url\":\"https://a/rss\"}," +
                        "{\"id\":\"b\",\"name\":\"B\",\"url\":\"https://b/rss\"}" +
                        "]")
                .addHeader("Content-Type", "application/json"));

        String baseUrl = server.url("/").toString();
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

        FeedApiClient client = new FeedApiClient(baseUrl, RestClient.builder());

        List<FeedDto> feeds = client.selectedFeeds();
        assertThat(feeds).hasSize(2);
        assertThat(feeds).extracting(FeedDto::id).containsExactly("a", "b");
    }
}
