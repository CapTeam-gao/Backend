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

    // 추천안 단건을 식별하는 PK입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 버전 묶음에 속한 추천안인지 연결해 버전 조회와 적용 대상을 나눕니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_version_id")
    private TeamMatchingVersion matchingVersion;

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
    public TeamRecommendation(TeamMatchingVersion matchingVersion, Grade grade, String strengths, String weaknesses) {
        this.matchingVersion = matchingVersion;
        this.grade = grade;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.status = RecommendationStatus.PENDING;
    }

    public void accept() {
        this.status = RecommendationStatus.ACCEPTED;
    }

    // 저장 직전에 버전 관계를 교체해야 할 때 명시적으로 연결합니다.
    public void assignMatchingVersion(TeamMatchingVersion matchingVersion) {
        this.matchingVersion = matchingVersion;
    }

}
