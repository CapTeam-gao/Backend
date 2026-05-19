package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.Grade;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notices")
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    private Grade grade;    // null이면 전체 학년 대상

    @Builder
    public Notice(String title, String content, User author, Grade grade) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.grade = grade;
    }

    public void update(String title, String content, Grade grade) {
        this.title = title;
        this.content = content;
        this.grade = grade;
    }
}
