package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.TeamRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "team_member")
public class TeamMember {

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
    @Column(name = "team_role", nullable = false)
    private TeamRole teamRole;

    public TeamMember(Team team, User user, TeamRole teamRole) {
        this.team = team;
        this.user = user;
        this.teamRole = teamRole;
    }
}