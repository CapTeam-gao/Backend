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
    // 로그인 ID로 사용하는 학생/관리자 고유 식별자를 저장하는 필드입니다.
    private String userId;

    @Column(nullable = false)
    // 사용자 이름을 저장하는 필드입니다.
    private String name;

    @Column(nullable = false)
    // 로그인 비밀번호를 저장하는 필드입니다. 최초 기본값은 1234입니다.
    private String password = "1234";

    // 최초 로그인 비밀번호가 암호화되었는지 저장하는 필드입니다.
    private boolean passwordEncoded = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // 학생/관리자 권한을 구분하는 필드입니다.
    private AccountRole accountRole;

    // 사용자 기본 계정을 생성하는 기능입니다.
    @Builder
    public User(String userId, String name, AccountRole accountRole, Grade grade) {
        this.userId = userId;
        this.name = name;
        this.password = "1234";
        this.accountRole = accountRole;
        this.grade = grade;
    }

    @Enumerated(EnumType.STRING)
    // 학생이 희망하는 개발 역할을 저장하는 필드입니다.
    private StudentRole studentRole;

    @ElementCollection
    @Column(columnDefinition = "TEXT")
    // 학생이 보유한 기술 스택을 저장하는 필드입니다.
    private List<String> skill;

    @ElementCollection
    @Column(columnDefinition = "TEXT")
    // 학생의 구현 경험을 저장하는 필드입니다.
    private List<String> experience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    // 학생 학년을 저장하는 필드입니다. 관리자는 학년 정보가 없으므로 null을 허용합니다.
    private Grade grade;

    // 학생 계정은 반드시 학년을 갖도록 DB 저장 및 수정 직전에 검증합니다.
    @PrePersist
    @PreUpdate
    private void validateGradeByAccountRole() {
        if (accountRole == AccountRole.STUDENT && grade == null) {
            throw new IllegalArgumentException("학생 계정은 학년 정보가 필요합니다.");
        }
    }

    // 학생의 팀장 희망 여부를 저장하는 필드입니다.
    private boolean wantsLeader;

    @ElementCollection
    // 학생이 선호하는 팀원 userId를 최대 3명까지 저장하는 필드입니다.
    private List<String> preferredTeammates;

    // 학생이 설문을 완료했는지 저장하는 필드입니다.
    private boolean surveyCompleted = false;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "communication", column = @Column(name = "personality_communication")),
            @AttributeOverride(name = "responsibility", column = @Column(name = "personality_responsibility")),
            @AttributeOverride(name = "collaboration", column = @Column(name = "personality_collaboration")),
            @AttributeOverride(name = "flexibility", column = @Column(name = "personality_flexibility")),
            @AttributeOverride(name = "emotionalStability", column = @Column(name = "personality_emotional_stability"))
    })
    // 성격 성향 항목별 점수를 저장하는 필드입니다.
    private UserPersonalityScore personalityScores = new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0);

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "leadership", column = @Column(name = "development_leadership")),
            @AttributeOverride(name = "problemSolving", column = @Column(name = "development_problem_solving")),
            @AttributeOverride(name = "implementation", column = @Column(name = "development_implementation")),
            @AttributeOverride(name = "learningAbility", column = @Column(name = "development_learning_ability")),
            @AttributeOverride(name = "planning", column = @Column(name = "development_planning"))
    })
    // 개발 성향 항목별 점수를 저장하는 필드입니다.
    private UserDevelopmentScore developmentScores = new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0);

    // 비밀번호를 암호화된 값으로 변경하는 기능입니다.
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordEncoded = true;
    }

    // 마이페이지 프로필 정보를 변경하는 기능입니다.
    public void updateProfile(StudentRole studentRole, List<String> skill, List<String> experience,
                              boolean wantsLeader, List<String> preferredTeammates) {
        this.studentRole = studentRole;
        this.skill = skill;
        this.experience = experience;
        this.wantsLeader = wantsLeader;
        this.preferredTeammates = preferredTeammates;
    }

    // 설문 결과와 성향 점수를 저장하고 설문 완료 상태로 변경하는 기능입니다.
    public void completeSurvey(StudentRole studentRole, List<String> skill, List<String> experience,
                               boolean wantsLeader, List<String> preferredTeammates,
                               UserPersonalityScore personalityScores, UserDevelopmentScore developmentScores) {
        updateProfile(studentRole, skill, experience, wantsLeader, preferredTeammates);
        this.personalityScores = personalityScores;
        this.developmentScores = developmentScores;
        this.surveyCompleted = true;
    }
}
