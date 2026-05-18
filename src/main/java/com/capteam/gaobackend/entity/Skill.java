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
public class Skill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String department;  //역할 벡엔드,프론트 하나씩 지정

    //여러개니까 리스트
    @ElementCollection
    private List<String> stack;   //스택    언어,프레임워크 등

    @ElementCollection
    private List<String> implementExperience;  //구현 경험    뭐 만들었는지


    @ElementCollection
    private List<String> methodExperience;  //구현 방식 어떻게 만들었는지



}
