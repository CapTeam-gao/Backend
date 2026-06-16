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

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Builder
    public TeamRecommendation(Grade grade, String strengths, String weaknesses) {
        this.grade = grade;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.status = RecommendationStatus.PENDING;
    }

    public void accept() {
        this.status = RecommendationStatus.ACCEPTED;
    }

}
