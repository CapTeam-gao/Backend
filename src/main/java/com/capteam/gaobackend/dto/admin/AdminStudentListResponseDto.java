package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminStudentListResponseDto {

    // 학생 userId를 내려주는 필드입니다.
    private String userId;

    // 학생 이름을 내려주는 필드입니다.
    private String name;

    // 학생 학년을 내려주는 필드입니다.
    private Grade grade;

    // 학생이 소속된 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 팀 프로젝트 기획서에 작성된 팀 이름을 내려주는 필드입니다.
    private String projectTeamName;

    // 학생의 팀 내 개발 역할을 내려주는 필드입니다.
    private StudentRole studentRole;

    // 학생의 팀장/팀원 역할을 내려주는 필드입니다.
    private LeaderRole leaderRole;

    // AI가 분석한 학생 실력 수준을 내려주는 필드입니다.
    private StudentLevel studentLevel;

    // 학생의 기술 스택 목록을 내려주는 필드입니다.
    private List<String> skill;

    // 학생이 설문조사를 완료했는지 관리자 목록에서 표시하기 위한 필드입니다.
    private boolean surveyCompleted;

    // 구현 실행력 점수를 내려주는 필드입니다.
    private Double implementation;

    // 문제 해결력 점수를 내려주는 필드입니다.
    private Double problemSolving;

    // 완성도 점수를 내려주는 필드입니다.
    private Double completionQuality;

    // 발표/전달력 점수를 내려주는 필드입니다.
    private Double presentation;

    // 리더십 성향 점수를 내려주는 필드입니다.
    private Double leadership;

    // 아이디어 기획 성향 점수를 내려주는 필드입니다.
    private Double ideaPlanning;

    // 소통 성향 점수를 내려주는 필드입니다.
    private Double communication;

    // 역할 유연성 점수를 내려주는 필드입니다.
    private Double roleFlexibility;

    // 시간 압박 대응 점수를 내려주는 필드입니다.
    private Double timePressure;

    // 체력/집중 유지 점수를 내려주는 필드입니다.
    private Double staminaFocus;

    // AI 분석 정보 없이 TeamUser만으로 학생 목록 응답 DTO를 만드는 기능입니다.
    public static AdminStudentListResponseDto from(TeamUser teamUser) {
        return from(teamUser, null);
    }

    // TeamUser와 UserAnalysis 엔티티를 관리자 학생 목록 응답 DTO로 변환하는 기능입니다.
    public static AdminStudentListResponseDto from(TeamUser teamUser, UserAnalysis userAnalysis) {
        return from(teamUser.getUser(), teamUser, userAnalysis);
    }

    // User와 선택적인 팀원/AI 분석 정보를 관리자 학생 목록 응답 DTO로 변환하는 기능입니다.
    public static AdminStudentListResponseDto from(User user, TeamUser teamUser, UserAnalysis userAnalysis) {
        return from(user, teamUser, userAnalysis, null);
    }

    // User와 선택적인 팀원/AI 분석/프로젝트 팀명을 관리자 학생 목록 응답 DTO로 변환하는 기능입니다.
    public static AdminStudentListResponseDto from(User user, TeamUser teamUser, UserAnalysis userAnalysis, String projectTeamName) {
        var developmentScore = user.getDevelopmentScores();
        var safeDevelopmentScore = developmentScore == null
                ? new com.capteam.gaobackend.entity.UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0)
                : developmentScore;
        var personalityScore = user.getPersonalityScores();
        var safePersonalityScore = personalityScore == null
                ? new com.capteam.gaobackend.entity.UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0)
                : personalityScore;

        return AdminStudentListResponseDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .grade(user.getGrade())
                .teamName(teamUser == null ? "미배정" : teamUser.getTeam().getTeamName())
                .projectTeamName(projectTeamName)
                .studentRole(teamUser == null ? user.getStudentRole() : teamUser.getStudentRole())
                .leaderRole(teamUser == null ? null : teamUser.getLeaderRole())
                .studentLevel(userAnalysis == null ? null : userAnalysis.getStudentLevel())
                .skill(user.getSkill())
                .surveyCompleted(user.isSurveyCompleted())
                .implementation(safeDevelopmentScore.getImplementation())
                .problemSolving(safeDevelopmentScore.getProblemSolving())
                .completionQuality(safeDevelopmentScore.getCompletionQuality())
                .presentation(safeDevelopmentScore.getPresentation())
                .leadership(safeDevelopmentScore.getLeadership())
                .ideaPlanning(safePersonalityScore.getIdeaPlanning())
                .communication(safePersonalityScore.getCommunication())
                .roleFlexibility(safePersonalityScore.getRoleFlexibility())
                .timePressure(safePersonalityScore.getTimePressure())
                .staminaFocus(safePersonalityScore.getStaminaFocus())
                .build();
    }
}
