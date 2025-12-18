package org.example.newshub.model;

public record Feed(
        String id,
        String name,
        String url,
        FeedCategory category
) {}
