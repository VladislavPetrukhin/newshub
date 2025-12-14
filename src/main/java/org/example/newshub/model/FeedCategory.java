package org.example.newshub.model;

public enum FeedCategory {
    RUSSIAN("российские"),
    INTERNATIONAL("международные"),
    BELARUS("белорусские"),
    KAZAKHSTAN("казахстанские"),
    CUSTOM("свои");

    private final String title;

    FeedCategory(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
