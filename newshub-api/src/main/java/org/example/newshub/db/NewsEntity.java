package org.example.newshub.db;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "news",
        indexes = {
                @Index(name = "idx_news_source_id", columnList = "sourceId"),
                @Index(name = "idx_news_published", columnList = "publishedAt"),
                @Index(name = "idx_news_added", columnList = "addedAt"),
                @Index(name = "idx_news_seen", columnList = "seen"),
                @Index(name = "idx_news_dedup", columnList = "dedupKey", unique = true)
        }
)
public class NewsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 512)
    private String title;

    @Lob
    private String description;

    @Column(length = 2048)
    private String link;

    @Column(length = 2048)
    private String guid;
    
    @Column(length = 64, nullable = false, unique = true)
    private String dedupKey;

    @Column(length = 256)
    private String pubDateRaw;

    private Instant publishedAt;

    private Instant addedAt;

    @Column(length = 64)
    private String sourceId;

    @Column(length = 256)
    private String sourceName;

    @Column(length = 2048)
    private String sourceUrl;

    private boolean seen;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }

    public String getPubDateRaw() {
        return pubDateRaw;
    }

    public void setPubDateRaw(String pubDateRaw) {
        this.pubDateRaw = pubDateRaw;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}
