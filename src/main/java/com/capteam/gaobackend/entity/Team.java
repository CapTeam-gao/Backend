package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.TeamStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "teams")
@EntityListeners(EntityListeners.class)

public class Team extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_name", nullable = false)
    private String teamName;

// <<<<<<< jinuk
// //    @Enumerated(EnumType.STRING)
// //    @Column(nullable = false)
// //    private TeamStatus status;
// =======

//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false)
//     private TeamStatus status;
// >>>>>>> master

//    public Team(String teamName, TeamStatus status) {
//        this.teamName = teamName;
//        this.status = status;
//    }
}
