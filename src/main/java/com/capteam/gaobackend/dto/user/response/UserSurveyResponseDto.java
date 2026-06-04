package com.capteam.gaobackend.dto.user.response;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserDevelopmentScore;
import com.capteam.gaobackend.entity.UserPersonalityScore;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserSurveyResponseDto {

    // 설문 응답자의 userId를 내려주는 필드입니다.
    private String userId;

    // 설문 응답자의 이름을 내려주는 필드입니다.
    private String name;

    // 설문 완료 여부를 내려주는 필드입니다.
    private boolean surveyCompleted;

    // 학생이 희망하는 개발 역할을 내려주는 필드입니다.
    private StudentRole studentRole;

    // 학생이 입력한 기술 스택 목록을 내려주는 필드입니다.
    private List<String> skill;

    // 학생이 입력한 구현 경험 목록을 내려주는 필드입니다.
    private List<String> experience;

    // 팀장 희망 여부를 내려주는 필드입니다.
    private boolean wantsLeader;

    // 선호 팀원 userId 목록을 내려주는 필드입니다.
    private List<String> preferredTeammates;

    // 성격 성향 항목별 평균 점수를 내려주는 필드입니다.
    private PersonalityScoresDto personalityScores;

    // 개발 성향 항목별 평균 점수를 내려주는 필드입니다.
    private DevelopmentScoresDto developmentScores;

    // User 엔티티를 설문 응답 DTO로 변환하는 기능입니다.
    public static UserSurveyResponseDto from(User user) {
        return UserSurveyResponseDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .surveyCompleted(user.isSurveyCompleted())
                .studentRole(user.getStudentRole())
                .skill(user.getSkill())
                .experience(user.getExperience())
                .wantsLeader(user.isWantsLeader())
                .preferredTeammates(user.getPreferredTeammates())
                .personalityScores(PersonalityScoresDto.from(user.getPersonalityScores()))
                .developmentScores(DevelopmentScoresDto.from(user.getDevelopmentScores()))
                .build();
    }

    @Getter
    @Builder
    public static class PersonalityScoresDto {

        // 소통 성향 점수를 내려주는 필드입니다.
        private Double communication;

        // 책임감 성향 점수를 내려주는 필드입니다.
        private Double responsibility;

        // 협업 성향 점수를 내려주는 필드입니다.
        private Double collaboration;

        // 유연성 성향 점수를 내려주는 필드입니다.
        private Double flexibility;

        // 감정 안정성 점수를 내려주는 필드입니다.
        private Double emotionalStability;

        // 성격 성향 엔티티 값을 응답 DTO로 변환하는 기능입니다.
        public static PersonalityScoresDto from(UserPersonalityScore score) {
            UserPersonalityScore safeScore = score == null ? new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0) : score;
            return PersonalityScoresDto.builder()
                    .communication(safeScore.getCommunication())
                    .responsibility(safeScore.getResponsibility())
                    .collaboration(safeScore.getCollaboration())
                    .flexibility(safeScore.getFlexibility())
                    .emotionalStability(safeScore.getEmotionalStability())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DevelopmentScoresDto {

        // 리더십 성향 점수를 내려주는 필드입니다.
        private Double leadership;

        // 문제 해결력 점수를 내려주는 필드입니다.
        private Double problemSolving;

        // 구현 실행력 점수를 내려주는 필드입니다.
        private Double implementation;

        // 학습 성장성 점수를 내려주는 필드입니다.
        private Double learningAbility;

        // 기획 정리력 점수를 내려주는 필드입니다.
        private Double planning;

        // 개발 성향 엔티티 값을 응답 DTO로 변환하는 기능입니다.
        public static DevelopmentScoresDto from(UserDevelopmentScore score) {
            UserDevelopmentScore safeScore = score == null ? new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0) : score;
            return DevelopmentScoresDto.builder()
                    .leadership(safeScore.getLeadership())
                    .problemSolving(safeScore.getProblemSolving())
                    .implementation(safeScore.getImplementation())
                    .learningAbility(safeScore.getLearningAbility())
                    .planning(safeScore.getPlanning())
                    .build();
        }
    }
}
