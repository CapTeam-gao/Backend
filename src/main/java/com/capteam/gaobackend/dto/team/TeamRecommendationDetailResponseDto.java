package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.entity.TeamRecommendationMember;
import com.capteam.gaobackend.entity.TeamRecommendationReason;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.RecommendationStatus;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 추천 상세 조회 시 반환하는 DTO (멤버 목록 + 배정 이유 포함)
@Getter
@Builder
public class TeamRecommendationDetailResponseDto {

    private Long id;
    private Grade grade;
    private RecommendationStatus status;
    private List<MemberDto> members;  // 추천된 팀원 목록
    private List<ReasonDto> reasons;  // AI 배정 이유 카드 목록

    // 추천 팀원 정보
    @Getter
    @Builder
    public static class MemberDto {
        private String userId;
        private String name;
        private StudentRole studentRole;     // AI가 배정한 역할
        private boolean isRecommendedLeader; // AI가 추천한 팀장 여부
        private String skill;                // 대표 기술스택 (User.skill 첫 번째)
        private StudentLevel studentLevel;   // AI가 분석한 실력 (상/중/하), 어드민만 조회 가능

        public static MemberDto from(TeamRecommendationMember member, StudentLevel studentLevel) {
            List<String> skills = member.getUser().getSkill();
            return MemberDto.builder()
                    .userId(member.getUser().getUserId())
                    .name(member.getUser().getName())
                    .studentRole(member.getStudentRole())
                    .isRecommendedLeader(member.isRecommendedLeader())
                    .skill(skills != null && !skills.isEmpty() ? skills.get(0) : null)
                    .studentLevel(studentLevel)
                    .build();
        }
    }

    // AI 배정 이유 카드
    @Getter
    @Builder
    public static class ReasonDto {
        private String title;
        private String description;

        public static ReasonDto from(TeamRecommendationReason reason) {
            return ReasonDto.builder()
                    .title(reason.getTitle())
                    .description(reason.getDescription())
                    .build();
        }
    }

    // levelMap: userId → studentLevel (서비스에서 UserAnalysis 조회 후 전달)
    public static TeamRecommendationDetailResponseDto from(
            TeamRecommendation recommendation,
            List<TeamRecommendationMember> members,
            List<TeamRecommendationReason> reasons,
            Map<String, StudentLevel> levelMap) {
        return TeamRecommendationDetailResponseDto.builder()
                .id(recommendation.getId())
                .grade(recommendation.getGrade())
                .status(recommendation.getStatus())
                .members(members.stream()
                        .map(m -> MemberDto.from(m, levelMap.get(m.getUser().getUserId())))
                        .collect(Collectors.toList()))
                .reasons(reasons.stream().map(ReasonDto::from).collect(Collectors.toList()))
                .build();
    }
}
