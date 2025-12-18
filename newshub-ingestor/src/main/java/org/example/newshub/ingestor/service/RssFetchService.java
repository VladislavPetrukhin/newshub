package org.example.newshub.ingestor.service;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import org.example.newshub.common.dto.FeedDto;
import org.example.newshub.common.kafka.NewsBatchEvent;
import org.example.newshub.common.kafka.NewsItemPayload;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RssFetchService {

    private final RssReader reader = new RssReader();

    public List<NewsBatchEvent> fetch(String fetchId, List<FeedDto> selectedFeeds) {
        Instant fetchedAt = Instant.now();

        List<NewsBatchEvent> batches = new ArrayList<>();
        if (selectedFeeds == null) return batches;

        for (FeedDto feed : selectedFeeds) {
            try {
                List<Item> read = reader.read(feed.url()).collect(Collectors.toList());
                List<NewsItemPayload> items = new ArrayList<>();

                for (Item it : read) {
                    String title = it.getTitle().orElse("без названия");
                    String desc  = it.getDescription().orElse("нет описания");
                    String link  = it.getLink().orElse("");
                    link = link.isBlank() ? null : link;

                    String category = null;
                    try {
                        var cats = it.getCategories();
                        if (cats != null && !cats.isEmpty()) {
                            category = cats.get(0);
                            if (category != null && category.isBlank()) category = null;
                        }
                    } catch (Exception ignored) {}

                    String dateRaw = it.getPubDate().orElse("");
                    Instant publishedAt = DateParsing.tryParseInstant(dateRaw);
                    Instant addedAt = Instant.now();


                    String guid = it.getGuid().orElse("");
                    if (guid.isBlank()) {
                        String base = feed.id() + "|" + (link == null ? "" : link) + "|" + title + "|" + dateRaw;
                        guid = UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
                    }

                    items.add(new NewsItemPayload(
                            title,
                            desc,
                            link,
                            category,
                            dateRaw,
                            publishedAt,
                            addedAt,
                            guid,
                            feed.id(),
                            feed.name(),
                            feed.url()
                    ));
                }

                batches.add(new NewsBatchEvent(
                        fetchId,
                        fetchedAt,
                        feed.id(),
                        feed.name(),
                        feed.url(),
                        items,
                        null
                ));
            } catch (Exception ex) {
                batches.add(new NewsBatchEvent(
                        fetchId,
                        fetchedAt,
                        feed.id(),
                        feed.name(),
                        feed.url(),
                        List.of(),
                        ex.getMessage()
                ));
            }
        }

        return batches;
    }
}
