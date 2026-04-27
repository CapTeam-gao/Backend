package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Entity
@Table(name = "skills")
@Getter //게터만
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String department;  //역할 벡엔드,프론트 하나씩 지정

    //여러개니까 리스트
    @ElementCollection
    private List<String> skill;   //스택    언어,프레임워크 등

    @ElementCollection
    private List<String> experience;  //경험    뭐 만들었는지

}
