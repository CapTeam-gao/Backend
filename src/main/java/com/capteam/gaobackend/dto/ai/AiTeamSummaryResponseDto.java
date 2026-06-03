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

    @JsonProperty("total_students")
    private int totalStudents;

    @JsonProperty("total_teams")
    private int totalTeams;

    private List<TeamDto> teams;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TeamDto {
        @JsonProperty("team_name")
        private String teamName;

        @JsonProperty("total_people")
        private int totalPeople;

        @JsonProperty("role_counts")
        private List<RoleCountDto> roleCounts;

        private String leader;

        @JsonProperty("top_stack_scores")
        private List<TeamTopStackScoreDto> topStackScores;

        @JsonProperty("matching_reason")
        private String matchingReason;

        private String strengths;
        private String weaknesses;

        @JsonProperty("skill_level_counts")
        private Map<String, Integer> skillLevelCounts;

        private List<MemberDto> members;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoleCountDto {
        @JsonProperty("role_group")
        private String roleGroup;

        private int count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TeamTopStackScoreDto {
        private String student;
        private String stack;
        private int score;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MemberDto {
        private String name;
        private String role;

        @JsonProperty("role_group")
        private String roleGroup;

        @JsonProperty("skill_level")
        private String skillLevel;

        private double score;
        private String strength;

        @JsonProperty("top_stack_scores")
        private List<MemberTopStackScoreDto> topStackScores;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MemberTopStackScoreDto {
        private String stack;
        private int score;
    }
}
