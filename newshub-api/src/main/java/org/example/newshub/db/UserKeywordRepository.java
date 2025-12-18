package org.example.newshub.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserKeywordRepository extends JpaRepository<UserKeywordEntity, Long> {
    Optional<UserKeywordEntity> findByKeywordIgnoreCase(String keyword);
}
