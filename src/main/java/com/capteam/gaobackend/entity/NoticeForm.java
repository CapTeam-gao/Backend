package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice_forms")
public class NoticeForm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;    // 어드민

    @Builder
    public NoticeForm(String title, String description, User author) {
        this.title = title;
        this.description = description;
        this.author = author;
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
