package org.example.newshub.db;

import jakarta.persistence.*;
import org.example.newshub.model.FeedCategory;

import java.time.Instant;

@Entity
@Table(name = "feeds")
public class FeedEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedCategory category;

    @Column(nullable = false)
    private boolean selected;

    @Column(nullable = false)
    private Instant createdAt;

    public FeedEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public FeedCategory getCategory() { return category; }
    public void setCategory(FeedCategory category) { this.category = category; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
