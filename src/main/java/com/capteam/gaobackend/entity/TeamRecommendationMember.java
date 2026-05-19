package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.StudentRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "team_recommendation_members")
public class TeamRecommendationMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private TeamRecommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_role", nullable = false)
    private StudentRole studentRole;    // AI가 배정한 역할

    @Builder
    public TeamRecommendationMember(TeamRecommendation recommendation, User user, StudentRole studentRole) {
        this.recommendation = recommendation;
        this.user = user;
        this.studentRole = studentRole;
    }
}
