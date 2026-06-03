package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

// 팀원 개별 작성 항목
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
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String todayActivityContent;    // 오늘 진행한 상황

    @Column(columnDefinition = "TEXT", nullable = false)
    private String activityContent;         // 활동 내용

    @Column(columnDefinition = "TEXT", nullable = false)
    private String nextPlanContent;         // 다음 캡스톤 시간까지 진행할 내용

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reflectionContent;       // 오늘 프로젝트 수행 만족도 및 자기 반성

    @Builder
    public JournalEntry(Journal journal, User writer,
                        String todayActivityContent, String activityContent,
                        String nextPlanContent, String reflectionContent) {
        this.journal = journal;
        this.writer = writer;
        this.todayActivityContent = todayActivityContent;
        this.activityContent = activityContent;
        this.nextPlanContent = nextPlanContent;
        this.reflectionContent = reflectionContent;
    }

    public void update(String todayActivityContent, String activityContent,
                       String nextPlanContent, String reflectionContent) {
        this.todayActivityContent = todayActivityContent;
        this.activityContent = activityContent;
        this.nextPlanContent = nextPlanContent;
        this.reflectionContent = reflectionContent;
    }
}
