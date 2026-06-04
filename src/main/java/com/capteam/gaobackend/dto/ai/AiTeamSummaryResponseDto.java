package com.capteam.gaobackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class AiTeamSummaryResponseDto {

    // AI 팀 매칭 대상 학생 전체 수를 내려주는 필드입니다.
    // JSON의 "total_students"을 Java의 "totalStudents" 필드에 매핑
    @JsonProperty("total_students")
    private int totalStudents;

    // 생성되었거나 요약된 팀 전체 수를 내려주는 필드입니다.
    @JsonProperty("total_teams")
    private int totalTeams;

    // 팀별 매칭 요약 목록을 내려주는 필드입니다.ㅌ
    private List<TeamDto> teams;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TeamDto {
        // 팀 이름을 내려주는 필드입니다.
        @JsonProperty("team_name")
        private String teamName;

        // 팀 전체 인원 수를 내려주는 필드입니다.
        @JsonProperty("total_people")
        private int totalPeople;

        // 역할군별 인원 수를 내려주는 필드입니다.
        @JsonProperty("role_counts")
        private List<RoleCountDto> roleCounts;

        // 추천 팀장 이름을 내려주는 필드입니다.
        private String leader;

        // 팀 기준 상위 기술 스택 점수 목록을 내려주는 필드입니다.
        @JsonProperty("top_stack_scores")
        private List<TeamTopStackScoreDto> topStackScores;

        // 이 팀이 이렇게 구성된 이유를 내려주는 필드입니다.
        @JsonProperty("matching_reason")
        private String matchingReason;

        // 팀 강점 설명을 내려주는 필드입니다.
        private String strengths;

        // 팀 보완점 설명을 내려주는 필드입니다.
        private String weaknesses;

        // 팀 안의 상/중/하 실력 분포를 내려주는 필드입니다.
        @JsonProperty("skill_level_counts")
        private Map<String, Integer> skillLevelCounts;

        // 팀원별 상세 매칭 정보를 내려주는 필드입니다.
        private List<MemberDto> members;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoleCountDto {
        // backend/frontend 같은 역할군 이름을 내려주는 필드입니다.
        @JsonProperty("role_group")
        private String roleGroup;

        // 해당 역할군 인원 수를 내려주는 필드입니다.
        private int count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TeamTopStackScoreDto {
        // 기술 스택을 가진 학생 이름을 내려주는 필드입니다.
        private String student;

        // 팀 대표 기술 스택 이름을 내려주는 필드입니다.
        private String stack;

        // 기술 스택 점수를 내려주는 필드입니다.
        private int score;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MemberDto {
        // 팀원 이름을 내려주는 필드입니다.
        private String name;

        // 팀원의 세부 개발 역할을 내려주는 필드입니다.
        private String role;

        // 팀원의 역할군을 내려주는 필드입니다.
        @JsonProperty("role_group")
        private String roleGroup;

        // 팀원의 상/중/하 실력 수준을 내려주는 필드입니다.
        @JsonProperty("skill_level")
        private String skillLevel;

        // 팀원의 매칭 점수를 내려주는 필드입니다.
        private double score;

        // 팀원의 강점 설명을 내려주는 필드입니다.
        private String strength;

        // 팀원의 상위 기술 스택 점수 목록을 내려주는 필드입니다.
        @JsonProperty("top_stack_scores")
        private List<MemberTopStackScoreDto> topStackScores;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MemberTopStackScoreDto {
        // 팀원 개인의 대표 기술 스택 이름을 내려주는 필드입니다.
        private String stack;

        // 팀원 개인의 대표 기술 스택 점수를 내려주는 필드입니다.
        private int score;
    }
}
