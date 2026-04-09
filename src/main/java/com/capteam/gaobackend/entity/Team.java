package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.TeamStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "team")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_name", nullable = false)
    private String teamName;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamStatus status;

    public Team(String teamName, TeamStatus status) {
        this.teamName = teamName;
        this.status = status;
    }
}
