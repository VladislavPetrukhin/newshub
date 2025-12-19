package org.example.newshub.ingestor.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.example.newshub.common.dto.FeedDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RssFetchServiceTest {

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
    void fetch_parses_items_and_maps_fields() {
        String rss = """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <rss version=\"2.0\">
                  <channel>
                    <title>Test</title>
                    <item>
                      <title>Hello Linux</title>
                      <description><![CDATA[Linux is great]]></description>
                      <link>https://example.com/a</link>
                      <guid>guid-a</guid>
                      <category>Tech</category>
                      <pubDate>Tue, 03 Dec 2024 14:05:00 GMT</pubDate>
                    </item>
                    <item>
                      <title>No Link</title>
                      <description>desc</description>
                      <link></link>
                      <pubDate>2024-12-03T14:05:00Z</pubDate>
                    </item>
                  </channel>
                </rss>
                """;

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/rss+xml; charset=utf-8")
                .setBody(rss));

        String url = server.url("/rss").toString();
        FeedDto feed = new FeedDto("test-id", "Test Feed", url);

        RssFetchService svc = new RssFetchService();
        var events = svc.fetch("fetch-1", List.of(feed));

        assertThat(events).hasSize(1);
        var ev = events.get(0);
        assertThat(ev.ok()).isTrue();
        assertThat(ev.items()).hasSize(2);

        var a = ev.items().get(0);
        assertThat(a.title()).contains("Hello");
        assertThat(a.category()).isEqualTo("Tech");
        assertThat(a.link()).isEqualTo("https://example.com/a");
        assertThat(a.guid()).isEqualTo("guid-a");
        assertThat(a.sourceId()).isEqualTo("test-id");

        var b = ev.items().get(1);
        assertThat(b.link()).isNull();
        assertThat(b.guid()).isNotBlank();
    }
}
