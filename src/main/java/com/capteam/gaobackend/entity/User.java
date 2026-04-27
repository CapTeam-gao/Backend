package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.StudentRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Entity
@Table(name = "users")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)  //서비스코드에서 함부로 new로 객체 생성 막하는거 방지(실수)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true,name = "student_id")
    private String stuId; //학번 stu2108으로

    @Column(nullable = false)
    private String name;    //이름 디비에 직접 넣기

    @Column(nullable = false)
    private String password = "1234";    //초기 비번 디폴트로 1234로 하고 로그인후 변경 가능하게

    private boolean passwordEncoded;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentRole studentRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountRole accountRole = getAccountRole();


    private String profileImage;    //프로필 이미지


//    //회원 생성할 때 무조건 필요한 값만 넣어야 해서 프로필 이미지 뺌
//    @Builder    //NO알규스랑 같이 있으면 생성자 충돌때문에 여기다 넣음
//    public User(String userId,String name,String password,AccountRole accountRole){
//        this.stuId = userId;
//        this.name = name;
//        this.password = password;
//        this.accountRole = accountRole;
//    }


//    여기부터 프로필(마이페이지)에서 직접 값 넣기

//
//
//    따로
//    private String user_analyze;    //ai가 분석한 학생 점수

}
