package com.capteam.gaobackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(nullable = false)
    private String user_id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    private String department;  //역할

    private String skill;   //스택

    private String experience;  //경험

    private String profileImage;    //프로필 이미지

    private Boolean isLogin;    //로그인 한번 되있는지

    private String user_analyze;    //학생 점수








}
