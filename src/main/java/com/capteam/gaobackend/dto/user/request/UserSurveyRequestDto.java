package com.capteam.gaobackend.dto.user.request;

import com.capteam.gaobackend.enums.StudentRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UserSurveyRequestDto {

    private StudentRole studentRole;
    private List<String> selectedRoles;
    private List<String> skill;
    private String stackText;
    private List<String> experience;
    private List<ExperienceItemDto> experiences;
    private Boolean wantsLeader;
    private String leaderPreference;
    private List<String> preferredTeammates;
    private List<String> preferredMembers;
    private List<Integer> personalityScores;
    private List<Integer> developmentScores;
    private List<Integer> devScores;

    @Getter
    @NoArgsConstructor
    public static class ExperienceItemDto {
        private String value;
    }
}
