package org.example.newshub.model;

public enum FeedCategory {
    RUS("российские"),
    INTL("международные"),
    BY("белорусские"),
    KZ("казахстанские"),
    CUSTOM("свои");

    private final String title;

    FeedCategory(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
