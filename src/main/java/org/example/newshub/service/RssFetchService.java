package org.example.newshub.service;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import org.example.newshub.model.Feed;
import org.example.newshub.model.NewsItem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RssFetchService {

    private final RssReader reader = new RssReader();

    public FetchResult fetch(List<Feed> selectedFeeds) {
        List<NewsItem> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Feed feed : selectedFeeds) {
            try {
                List<Item> read = reader.read(feed.url()).collect(Collectors.toList());
                for (Item it : read) {
                    String title = it.getTitle().orElse("без названия");
                    String desc  = it.getDescription().orElse("нет описания");
                    String link  = it.getLink().orElse("#");
                    String dateRaw = it.getPubDate().orElse("");
                    Instant publishedAt = DateParsing.tryParseInstant(dateRaw);
                    Instant addedAt = Instant.now();
                    String guid = it.getGuid().orElse(UUID.randomUUID().toString());

                    items.add(new NewsItem(
                            title,
                            desc,
                            link,
                            dateRaw,
                            publishedAt,
                            addedAt,
                            guid,
                            feed.id(),
                            feed.name(),
                            feed.url()
                    ));
                }
            } catch (Exception ex) {
                errors.add(feed.name() + " (" + feed.url() + "): " + ex.getMessage());
            }
        }

        return new FetchResult(items, errors);
    }

    public record FetchResult(List<NewsItem> items, List<String> errors) {}
}
