package org.example.newshub.service;

import org.example.newshub.db.UserKeywordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(KeywordService.class)
class KeywordServiceJpaTest {

    @Autowired KeywordService keywordService;
    @Autowired UserKeywordRepository repo;

    @Test
    void addMany_splitsTrims_andDedupsIgnoringCase() {
        keywordService.addMany(" linux,security\nPostgres; LINUX \t  security  ");

        var all = repo.findAll();
        assertThat(all).hasSize(3);
        assertThat(all).extracting("keyword")
                .containsExactlyInAnyOrder("linux", "security", "Postgres");
    }

    @Test
    void delete_removesKeyword() {
        keywordService.addOne("linux");
        var id = repo.findByKeywordIgnoreCase("LINUX").orElseThrow().getId();

        keywordService.delete(id);

        assertThat(repo.findAll()).isEmpty();
    }
}
