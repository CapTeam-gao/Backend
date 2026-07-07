package com.capteam.gaobackend.dto.ai;

import com.capteam.gaobackend.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.capteam.gaobackend.entity.UserAnalysis;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

// 백엔드 User 데이터를 AI 서버로 전송할 때 사용하는 DTO입니다.
@Getter
@Builder
public class AiStudentPayloadDto {

    @JsonProperty("user_id")
    private String userId;

    private String name;
    private String role;
    private List<String> stack;
    private List<String> experience;
    private String grade;

    @JsonProperty("wants_leader")
    private boolean wantsLeader;

    @JsonProperty("preferred_members")
    private List<String> preferredMembers;

    @JsonProperty("analysis_result")
    private String analysisResult;

    @JsonProperty("student_level")
    private String studentLevel;

    // 성격 성향 점수
    private Double ideaPlanning;
    private Double communication;
    private Double roleFlexibility;
    private Double timePressure;
    private Double staminaFocus;

    // 개발 성향 점수
    private Double implementation;
    private Double problemSolving;
    private Double completionQuality;
    private Double presentation;
    private Double leadership;

    // User 엔티티를 AI 전송용 DTO로 변환하는 기능입니다.
    public static AiStudentPayloadDto from(User user) {
        return from(user, null);
    }

    public static AiStudentPayloadDto from(User user, UserAnalysis analysis) {
        return AiStudentPayloadDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .role(user.getStudentRole() != null ? user.getStudentRole().name() : null)
                .stack(copyList(user.getSkill()))
                .experience(copyList(user.getExperience()))
                .grade(user.getGrade() != null ? user.getGrade().name() : null)
                .wantsLeader(user.isWantsLeader())
                .preferredMembers(copyList(user.getPreferredTeammates()))
                .analysisResult(analysis != null ? analysis.getAnalysisResult() : null)
                .studentLevel(analysis != null && analysis.getStudentLevel() != null
                        ? analysis.getStudentLevel().name()
                        : null)
                .ideaPlanning(user.getPersonalityScores() != null ? user.getPersonalityScores().getIdeaPlanning() : null)
                .communication(user.getPersonalityScores() != null ? user.getPersonalityScores().getCommunication() : null)
                .roleFlexibility(user.getPersonalityScores() != null ? user.getPersonalityScores().getRoleFlexibility() : null)
                .timePressure(user.getPersonalityScores() != null ? user.getPersonalityScores().getTimePressure() : null)
                .staminaFocus(user.getPersonalityScores() != null ? user.getPersonalityScores().getStaminaFocus() : null)
                .implementation(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getImplementation() : null)
                .problemSolving(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getProblemSolving() : null)
                .completionQuality(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getCompletionQuality() : null)
                .presentation(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getPresentation() : null)
                .leadership(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getLeadership() : null)
                .build();
    }

    private static List<String> copyList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(values);
    }
}
