package org.example.newshub.db;

import org.springframework.data.jpa.domain.Specification;

public final class NewsSpecs {
    private NewsSpecs() {}

    public static Specification<NewsEntity> sourceId(String sourceId) {
        return (root, q, cb) ->
                (sourceId == null || sourceId.isBlank())
                        ? cb.conjunction()
                        : cb.equal(root.get("sourceId"), sourceId);
    }

    public static Specification<NewsEntity> category(String category) {
        return (root, q, cb) ->
                (category == null || category.isBlank())
                        ? cb.conjunction()
                        : cb.equal(root.get("category"), category);
    }

    public static Specification<NewsEntity> textQuery(String text) {
        return (root, q, cb) -> {
            if (text == null || text.isBlank()) return cb.conjunction();

            String like = "%" + text.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("sourceName")), like)
            );
        };
    }
}
