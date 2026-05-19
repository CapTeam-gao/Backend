package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "user_skill")
@Getter //게터만
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSkill  extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    @JoinColumn(name = "skill_id")
    private Skill skill;


    @ManyToOne
    @JoinColumn(name = "skill_id")
    private Skill experience;
}
