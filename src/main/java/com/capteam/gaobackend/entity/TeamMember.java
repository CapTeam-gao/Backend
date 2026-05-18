package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "team_members")
public class TeamMember extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_role", nullable = false)
    private StudentRole studentRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "leader_role",nullable = false)
    private LeaderRole leaderRole;

    @Builder
    public TeamMember(Team team, User user, StudentRole studentRole, LeaderRole leaderRole) {
        this.team = team;
        this.user = user;
        this.studentRole = studentRole;
        this.leaderRole = leaderRole;
    }
}