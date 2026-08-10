package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.StudentRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "team_recommendation_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_recommendation_member_user",
                columnNames = {"recommendation_id", "user_id"}
        )
)
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
    private StudentRole studentRole;        // AI가 배정한 역할

    private boolean isRecommendedLeader;    // AI가 추천한 팀장 여부 (wantsLeader 기반)

    @Builder
    public TeamRecommendationMember(TeamRecommendation recommendation, User user,
                                    StudentRole studentRole, boolean isRecommendedLeader) {
        this.recommendation = recommendation;
        this.user = user;
        this.studentRole = studentRole;
        this.isRecommendedLeader = isRecommendedLeader;
    }
}
