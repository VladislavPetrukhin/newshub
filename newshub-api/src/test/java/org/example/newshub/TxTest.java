package org.example.newshub;

import org.example.newshub.db.NewsJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class TxTest {

    @Autowired
    NewsJpaRepository repo;

    @Test
    void shouldFailWithoutTransaction() {
        repo.deleteByIds(List.of(1L));
    }
}
