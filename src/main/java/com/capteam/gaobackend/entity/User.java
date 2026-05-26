package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @Column(nullable = false, name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password = "1234";

    private boolean passwordEncoded = false;    // 최초 로그인 시 암호화 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountRole accountRole;

    @Builder
    public User(String userId, String name, AccountRole accountRole) {
        this.userId = userId;
        this.name = name;
        this.password = "1234";
        this.accountRole = accountRole;
    }

    //    여기부터 프로필(마이페이지)에서 직접 값 넣기
    @Enumerated(EnumType.STRING)
    private StudentRole studentRole;            // 희망 역할 (마이페이지에서 설정)

    @ElementCollection
    private List<String> skill;                 // 기술스택

    @ElementCollection
    private List<String> experience;            // 경험

    @Column(columnDefinition = "LONGTEXT")
    private String profileImage;                // 프로필 이미지

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    private boolean wantsLeader;                // 팀장 희망 여부

    @ElementCollection
    private List<String> preferredTeammates;    // 선호 팀원 userId 최대 3명

    //    업데이트 메서드
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordEncoded = true;
    }

    public void updateProfile(StudentRole studentRole, List<String> skill, List<String> experience,
                              String profileImage, boolean wantsLeader, List<String> preferredTeammates) {
        this.studentRole = studentRole;
        this.skill = skill;
        this.experience = experience;
        this.profileImage = profileImage;
        this.wantsLeader = wantsLeader;
        this.preferredTeammates = preferredTeammates;
    }
}

