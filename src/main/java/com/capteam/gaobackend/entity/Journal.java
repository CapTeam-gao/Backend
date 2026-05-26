package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.JournalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "journals")
public class Journal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalStatus status;

    // 팀원 전원 작성 완료 후 AI가 병합한 최종 결과
    @Column(columnDefinition = "LONGTEXT")
    private String aiMergedContent;

    @Builder
    public Journal(Team team, String title, LocalDate date) {
        this.team = team;
        this.title = title;
        this.date = date;
        this.status = JournalStatus.IN_PROGRESS;
    }

    // 팀원 전원 완료 시 AI 병합 결과 저장하면서 상태 변경
    public void complete(String aiMergedContent) {
        this.aiMergedContent = aiMergedContent;
        this.status = JournalStatus.COMPLETED;
    }

    public void update(String title, LocalDate date) {
        this.title = title;
        this.date = date;
    }
}
