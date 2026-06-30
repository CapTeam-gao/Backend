package com.capteam.gaobackend.dto.user.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UserSurveyRequestDto {

    // 학생이 희망하는 개발 역할을 저장하는 필드입니다.
    private String studentRole;

    // 프론트 선택 UI에서 넘어오는 역할 문자열 목록을 받는 필드입니다.
    private List<String> selectedRoles;

    // 학생이 입력한 기술 스택 목록을 저장하는 필드입니다.
    private List<String> skill;

    // 쉼표로 입력된 기술 스택 문자열을 받는 필드입니다.
    private String stackText;

    // 학생이 입력한 구현 경험 목록을 저장하는 필드입니다.
    private List<String> experience;

    // 프론트 경험 입력 컴포넌트에서 넘어오는 경험 객체 목록을 받는 필드입니다.
    private List<ExperienceItemDto> experiences;

    // 팀장 희망 여부를 boolean으로 받는 필드입니다.
    private Boolean wantsLeader;

    // 팀장 희망 여부를 O/X 문자열로 받는 필드입니다.
    private String leaderPreference;

    // 선호 팀원 userId 목록을 저장하는 필드입니다.
    private List<String> preferredTeammates;

    // 프론트 선택 UI에서 넘어오는 선호 팀원 문자열 목록을 받는 필드입니다.
    private List<String> preferredMembers;

    // 성격 성향 항목별 평균 점수를 받는 필드입니다.
    private PersonalityScoresDto personalityScores;

    // 개발 성향 항목별 평균 점수를 받는 필드입니다.
    private DevelopmentScoresDto developmentScores;

    // 개발 성향 항목별 평균 점수의 기존 프론트 별칭을 받는 필드입니다.
    private DevelopmentScoresDto devScores;

    // 성격 성향 10개 문항 원점수를 받는 필드입니다. 2문항씩 묶어 5개 항목 평균으로 계산합니다.
    private List<Integer> personalityScoreAnswers;

    // 개발 성향 10개 문항 원점수를 받는 필드입니다. 2문항씩 묶어 5개 항목 평균으로 계산합니다.
    private List<Integer> developmentScoreAnswers;

    // 설문 응답 일관성 기반 신뢰도를 받는 필드입니다.
    private String responseReliability;

    // 설문 전체 불일치 응답 수를 받는 필드입니다.
    private Integer inconsistentAnswers;

    // 성격 성향 문항 불일치 수를 받는 필드입니다.
    private Integer personalityInconsistentCount;

    // 개발 성향 문항 불일치 수를 받는 필드입니다.
    private Integer developmentInconsistentCount;


    @Getter
    @NoArgsConstructor
    public static class ExperienceItemDto {

        // 구현 경험 입력값을 저장하는 필드입니다.
        private String value;
    }

    @Getter
    @NoArgsConstructor
    public static class PersonalityScoresDto {

        // 소통 성향 점수를 저장하는 필드입니다.
        private Double communication;

        // 책임감 성향 점수를 저장하는 필드입니다.
        private Double responsibility;

        // 협업 성향 점수를 저장하는 필드입니다.
        private Double collaboration;

        // 유연성 성향 점수를 저장하는 필드입니다.
        private Double flexibility;

        // 감정 안정성 점수를 저장하는 필드입니다.
        private Double emotionalStability;
    }

    @Getter
    @NoArgsConstructor
    public static class DevelopmentScoresDto {

        // 리더십 성향 점수를 저장하는 필드입니다.
        private Double leadership;

        // 문제 해결력 점수를 저장하는 필드입니다.
        private Double problemSolving;

        // 구현 실행력 점수를 저장하는 필드입니다.
        private Double implementation;

        // 학습 성장성 점수를 저장하는 필드입니다.
        private Double learningAbility;

        // 기획 정리력 점수를 저장하는 필드입니다.
        private Double planning;
    }
}
