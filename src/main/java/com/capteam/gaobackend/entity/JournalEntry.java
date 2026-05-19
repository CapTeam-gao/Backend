package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "journal_entries")
public class JournalEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;    // 항목 작성한 팀원

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder
    public JournalEntry(Journal journal, User author, String content) {
        this.journal = journal;
        this.author = author;
        this.content = content;
    }

    public void update(String content) {
        this.content = content;
    }
}
