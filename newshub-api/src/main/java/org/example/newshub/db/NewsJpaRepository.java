package org.example.newshub.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, Long>,
        JpaSpecificationExecutor<NewsEntity> {

    boolean existsByDedupKey(String dedupKey);

    @Query("select count(n) from NewsEntity n where n.seen = false")
    long countUnseen();

    @Query("select distinct n.sourceId from NewsEntity n where n.sourceId is not null and n.sourceId <> ''")
    List<String> distinctSourceIds();

    Page<NewsEntity> findBySourceId(String sourceId, Pageable pageable);

    Page<NewsEntity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrSourceNameContainingIgnoreCase(
            String title, String description, String sourceName, Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("update NewsEntity n set n.seen = true where n.id in :ids")
    int markSeen(@Param("ids") Collection<Long> ids);

    @Modifying
    @Transactional
    @Query("update NewsEntity n set n.seen = false")
    int markAllUnseen();

    @Query("select n.id from NewsEntity n order by n.addedAt asc")
    List<Long> findOldestIds(Pageable pageable);

    @Modifying
    @Transactional
    @Query("delete from NewsEntity n where n.id in :ids")
    int deleteByIds(@Param("ids") Collection<Long> ids);

    @Modifying
    @Transactional
    @Query("delete from NewsEntity n where n.sourceId is not null and n.sourceId not in :keep")
    int deleteBySourceIdNotIn(@Param("keep") Collection<String> keep);

    @Query("select max(n.sourceName) from NewsEntity n where n.sourceId = :sourceId")
    String findAnySourceName(@Param("sourceId") String sourceId);
    public interface NewsSourceView {
        String getSourceId();
        String getSourceName();

        default String id() { return getSourceId(); }
        default String name() { return getSourceName(); }
    }
    public interface SourceRow {
        String getId();
        String getName();
    }

    @org.springframework.data.jpa.repository.Query("""
  select n.sourceId as id, max(n.sourceName) as name
  from NewsEntity n
  where n.sourceId is not null and n.sourceId <> ''
  group by n.sourceId
""")
    java.util.List<SourceRow> distinctSourcesWithNames();

    @Query("""
    select distinct n.category
    from NewsEntity n
    where n.category is not null and n.category <> ''
    order by n.category
    """)
        List<String> distinctCategories();

    public interface CategoryRow {
        String getCategory();
        long getCnt();
    }

    @Query("""
select n.category as category, count(n) as cnt
from NewsEntity n
where n.category is not null and n.category <> ''
group by n.category
having count(n) >= :min
order by count(n) desc
""")
    List<CategoryRow> topCategories(@Param("min") long min, Pageable pageable);


}
