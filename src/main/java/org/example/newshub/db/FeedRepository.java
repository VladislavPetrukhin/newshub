package org.example.newshub.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedRepository extends JpaRepository<FeedEntity, String> {
    List<FeedEntity> findAllByOrderByCreatedAtAsc();
}
