package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.RecommendationStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "team_recommendations")
public class TeamRecommendation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;            // 추천 대상 학년

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationStatus status;

    @Builder
    public TeamRecommendation(Grade grade) {
        this.grade = grade;
        this.status = RecommendationStatus.PENDING;
    }

    public void accept() {
        this.status = RecommendationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = RecommendationStatus.REJECTED;
    }
}
