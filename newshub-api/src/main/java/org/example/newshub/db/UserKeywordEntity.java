package org.example.newshub.db;

import jakarta.persistence.*;

@Entity
@Table(name = "user_keywords", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_keywords_keyword", columnNames = "keyword")
})
public class UserKeywordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String keyword;


    public Long getId() { return id; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
