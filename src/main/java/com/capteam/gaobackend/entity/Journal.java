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
    private LocalDate date;     // 일지 날짜

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalStatus status;

    @Builder
    public Journal(Team team, String title, LocalDate date) {
        this.team = team;
        this.title = title;
        this.date = date;
        this.status = JournalStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = JournalStatus.COMPLETED;
    }

    public void update(String title, LocalDate date) {
        this.title = title;
        this.date = date;
    }
}
