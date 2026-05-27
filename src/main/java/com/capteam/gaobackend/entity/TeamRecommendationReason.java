package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

// AI가 생성한 팀 배정 이유 카드 (팀 추천안 하나에 여러 개 존재)
// ex) 제목: "프론트엔드 담당 실력 우수 및 리더쉽 좋음", 설명: "백엔드와 AI 분야 팀원이..."
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recommendation_reasons")
public class TeamRecommendationReason extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private TeamRecommendation recommendation; // 어떤 추천안에 속하는지

    @Column(nullable = false)
    private String title; // 배정 이유 제목 (짧게)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description; // 배정 이유 설명 (길게)

    @Builder
    public TeamRecommendationReason(TeamRecommendation recommendation, String title, String description) {
        this.recommendation = recommendation;
        this.title = title;
        this.description = description;
    }
}
