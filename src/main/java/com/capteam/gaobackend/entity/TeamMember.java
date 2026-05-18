package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "team_member")
public class TeamMember  extends BaseTimeEntity{

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
    @Column(nullable = false)
    private LeaderRole leaderRole;

    public TeamMember(Team team, User user, StudentRole studentRole) {
        this.team = team;
        this.user = user;
        this.studentRole = studentRole;
    }
}