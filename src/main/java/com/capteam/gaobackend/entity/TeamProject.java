package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

// 팀 기획서 (팀명, 서비스명, 서비스 소개, 주요 기능)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "team_projects")
public class TeamProject extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String teamName;        // 팀명

    @Column(nullable = false)
    private String serviceName;     // 서비스명

    @Column(columnDefinition = "TEXT", nullable = false)
    private String serviceIntro;    // 서비스 소개 (기획의도/주제)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String mainFeatures;    // 주요 기능

    @Builder
    public TeamProject(Team team, String teamName, String serviceName,
                       String serviceIntro, String mainFeatures) {
        this.team = team;
        this.teamName = teamName;
        this.serviceName = serviceName;
        this.serviceIntro = serviceIntro;
        this.mainFeatures = mainFeatures;
    }

    public void update(String teamName, String serviceName, String serviceIntro, String mainFeatures) {
        this.teamName = teamName;
        this.serviceName = serviceName;
        this.serviceIntro = serviceIntro;
        this.mainFeatures = mainFeatures;
    }
}
