package com.capteam.gaobackend.dto.ai;

import com.capteam.gaobackend.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

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

    // 성격 성향 점수
    private Double communication;
    private Double responsibility;
    private Double collaboration;
    private Double flexibility;

    @JsonProperty("emotionalStability")
    private Double emotionalStability;

    // 개발 성향 점수
    private Double leadership;
    private Double problemSolving;
    private Double implementation;
    private Double learningAbility;
    private Double planning;

    // User 엔티티를 AI 전송용 DTO로 변환하는 기능입니다.
    public static AiStudentPayloadDto from(User user) {
        return AiStudentPayloadDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .role(user.getStudentRole() != null ? user.getStudentRole().name() : null)
                .stack(user.getSkill())
                .experience(user.getExperience())
                .grade(user.getGrade() != null ? user.getGrade().name() : null)
                .wantsLeader(user.isWantsLeader())
                .preferredMembers(user.getPreferredTeammates())
                .communication(user.getPersonalityScores() != null ? user.getPersonalityScores().getCommunication() : null)
                .responsibility(user.getPersonalityScores() != null ? user.getPersonalityScores().getResponsibility() : null)
                .collaboration(user.getPersonalityScores() != null ? user.getPersonalityScores().getCollaboration() : null)
                .flexibility(user.getPersonalityScores() != null ? user.getPersonalityScores().getFlexibility() : null)
                .emotionalStability(user.getPersonalityScores() != null ? user.getPersonalityScores().getEmotionalStability() : null)
                .leadership(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getLeadership() : null)
                .problemSolving(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getProblemSolving() : null)
                .implementation(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getImplementation() : null)
                .learningAbility(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getLearningAbility() : null)
                .planning(user.getDevelopmentScores() != null ? user.getDevelopmentScores().getPlanning() : null)
                .build();
    }
}
