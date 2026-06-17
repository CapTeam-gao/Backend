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

    // 팀 추천안 고유 id를 내려주는 필드입니다.
    private Long id;

    // 추천 대상 학년을 내려주는 필드입니다.
    private Grade grade;

    // 추천안 승인 상태를 내려주는 필드입니다.
    private RecommendationStatus status;

    // AI가 생성한 팀 강점 설명을 내려주는 필드입니다.
    private String strengths;

    // AI가 생성한 팀 보완점 설명을 내려주는 필드입니다.
    private String weaknesses;

    // 추천된 팀원 목록을 내려주는 필드입니다.
    private List<MemberDto> members;

    // AI 배정 이유 카드 목록을 내려주는 필드입니다.
    private List<ReasonDto> reasons;

    // 추천 팀원 정보
    @Getter
    @Builder
    public static class MemberDto {
        // 추천 팀원 userId를 내려주는 필드입니다.
        private String userId;

        // 추천 팀원 이름을 내려주는 필드입니다.
        private String name;

        // AI가 배정한 개발 역할을 내려주는 필드입니다.
        private StudentRole studentRole;

        // AI가 해당 학생을 팀장으로 추천했는지 내려주는 필드입니다.
        private boolean isRecommendedLeader;

        // 대표 기술 스택을 내려주는 필드입니다. User.skill의 첫 번째 값을 사용합니다.
        private String skill;

        // AI가 분석한 실력 수준을 내려주는 필드입니다.
        private StudentLevel studentLevel;

        // TeamRecommendationMember 엔티티를 추천 팀원 응답 DTO로 변환하는 기능입니다.
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
        // AI 배정 이유 카드 제목을 내려주는 필드입니다.
        private String title;

        // AI 배정 이유 상세 설명을 내려주는 필드입니다.
        private String description;

        // TeamRecommendationReason 엔티티를 배정 이유 응답 DTO로 변환하는 기능입니다.
        public static ReasonDto from(TeamRecommendationReason reason) {
            return ReasonDto.builder()
                    .title(reason.getTitle())
                    .description(reason.getDescription())
                    .build();
        }
    }

    // 추천안 엔티티와 멤버/이유 목록을 추천 상세 응답 DTO로 변환하는 기능입니다.
    // levelMap은 userId를 key로 하고 AI 분석 실력 수준을 value로 가지는 맵입니다.
    public static TeamRecommendationDetailResponseDto from(
            TeamRecommendation recommendation,
            List<TeamRecommendationMember> members,
            List<TeamRecommendationReason> reasons,
            Map<String, StudentLevel> levelMap) {
        return TeamRecommendationDetailResponseDto.builder()
                .id(recommendation.getId())
                .grade(recommendation.getGrade())
                .status(recommendation.getStatus())
                .strengths(recommendation.getStrengths())
                .weaknesses(recommendation.getWeaknesses())
                .members(members.stream()
                        .map(m -> MemberDto.from(m, levelMap.get(m.getUser().getUserId())))
                        .collect(Collectors.toList()))
                .reasons(reasons.stream().map(ReasonDto::from).collect(Collectors.toList()))
                .build();
    }
}
