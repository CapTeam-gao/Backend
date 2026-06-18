package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.ResponseReliability;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminStudentDetailResponseDto {

    // 학생 userId를 내려주는 필드입니다.
    private String userId;

    // 학생 이름을 내려주는 필드입니다.
    private String name;

    // 학생 학년을 내려주는 필드입니다.
    private Grade grade;

    // 학생이 소속된 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 학생의 팀 내 개발 역할을 내려주는 필드입니다.
    private StudentRole studentRole;

    // 학생의 팀장/팀원 역할을 내려주는 필드입니다.
    private LeaderRole leaderRole;

    // AI가 분석한 학생 실력 수준을 내려주는 필드입니다.
    private StudentLevel studentLevel;

    // 학생의 기술 스택 목록을 내려주는 필드입니다.
    private List<String> skill;

    // 학생의 구현 경험 목록을 내려주는 필드입니다.
    private List<String> experience;

    // 학생이 선호하는 팀원 userId 목록을 내려주는 필드입니다.
    private List<String> preferredTeammates;

    // 학생의 팀장 희망 여부를 내려주는 필드입니다.
    private boolean wantsLeader;

    // AI가 생성한 학생 분석 설명을 내려주는 필드입니다.
    private String analysisResult;

    // 설문 응답 일관성 기반 신뢰도를 내려주는 필드입니다.
    private ResponseReliability responseReliability;

    // 설문 전체 불일치 응답 수를 내려주는 필드입니다.
    private Integer inconsistentAnswers;

    private Double leadership;

    // 문제 해결력 점수를 저장하는 필드입니다.
    private Double problemSolving;

    // 구현 실행력 점수를 저장하는 필드입니다.
    private Double implementation;

    // 학습 성장성 점수를 저장하는 필드입니다.
    private Double learningAbility;

    // 기획 정리력 점수를 저장하는 필드입니다.
    private Double planning;

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



    // TeamUser와 UserAnalysis 엔티티를 관리자 학생 상세 응답 DTO로 변환하는 기능입니다.
    public static AdminStudentDetailResponseDto from(TeamUser teamUser, UserAnalysis userAnalysis,UserDevelopmentScore userDevelopmentScore, UserPersonalityScore userPersonalityScore) {
        return from(teamUser.getUser(), teamUser, userAnalysis,userDevelopmentScore,userPersonalityScore);
    }

    // User와 선택적인 팀원/AI 분석 정보를 관리자 학생 상세 응답 DTO로 변환하는 기능입니다.
    public static AdminStudentDetailResponseDto from(User user, TeamUser teamUser, UserAnalysis userAnalysis, UserDevelopmentScore userDevelopmentScore, UserPersonalityScore userPersonalityScore) {


        return AdminStudentDetailResponseDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .grade(user.getGrade())
                .teamName(teamUser == null ? "미배정" : teamUser.getTeam().getTeamName())
                .studentRole(teamUser == null ? user.getStudentRole() : teamUser.getStudentRole())
                .leaderRole(teamUser == null ? null : teamUser.getLeaderRole())
                .studentLevel(userAnalysis == null ? null : userAnalysis.getStudentLevel())
                .skill(user.getSkill())
                .experience(user.getExperience())
                .preferredTeammates(user.getPreferredTeammates())
                .wantsLeader(user.isWantsLeader())
                .analysisResult(resolveAnalysisResult(userAnalysis))
                .responseReliability(userAnalysis == null ? null : userAnalysis.getResponseReliability())
                .inconsistentAnswers(userAnalysis == null ? null : userAnalysis.getInconsistentAnswers())
                .leadership(userDevelopmentScore.getLeadership())
                .problemSolving(userDevelopmentScore.getProblemSolving())
                .implementation(userDevelopmentScore.getImplementation())
                .learningAbility(userDevelopmentScore.getLearningAbility())
                .planning(userDevelopmentScore.getPlanning())
                .communication(userPersonalityScore.getCommunication())
                .responsibility(userPersonalityScore.getResponsibility())
                .collaboration(userPersonalityScore.getCollaboration())
                .flexibility(userPersonalityScore.getFlexibility())
                .emotionalStability(userPersonalityScore.getEmotionalStability())
                .build();
    }

    // 과거 매칭 로직이 실력 등급을 분석 설명에 저장한 데이터는 화면에 노출하지 않습니다.
    private static String resolveAnalysisResult(UserAnalysis userAnalysis) {
        if (userAnalysis == null || userAnalysis.getAnalysisResult() == null) {
            return null;
        }

        String analysisResult = userAnalysis.getAnalysisResult().trim();
        return switch (analysisResult) {
            case "상", "중", "하", "높음", "낮음" -> null;
            default -> analysisResult;
        };
    }
}
