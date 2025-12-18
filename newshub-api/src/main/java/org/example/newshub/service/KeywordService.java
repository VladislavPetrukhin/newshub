package org.example.newshub.service;

import org.example.newshub.db.UserKeywordEntity;
import org.example.newshub.db.UserKeywordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class KeywordService {

    private final UserKeywordRepository repo;

    public KeywordService(UserKeywordRepository repo) {
        this.repo = repo;
    }

    public List<UserKeywordEntity> list() {
        return repo.findAll();
    }

    @Transactional
    public void addMany(String input) {
        if (input == null) return;
        Arrays.stream(input.split("[,;\\n\\r\\t]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> s.length() >= 3)
                .distinct()
                .forEach(this::addOne);
    }

    @Transactional
    public void addOne(String kw) {
        if (kw == null) return;
        String s = kw.trim();
        if (s.isBlank()) return;

        if (repo.findByKeywordIgnoreCase(s).isPresent()) return;

        UserKeywordEntity e = new UserKeywordEntity();
        e.setKeyword(s);
        repo.save(e);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) return;
        repo.deleteById(id);
    }
}
