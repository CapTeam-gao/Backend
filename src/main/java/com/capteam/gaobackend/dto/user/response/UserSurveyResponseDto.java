package com.capteam.gaobackend.dto.user.response;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserSurveyResponseDto {

    private String userId;
    private String name;
    private boolean surveyCompleted;
    private StudentRole studentRole;
    private List<String> skill;
    private List<String> experience;
    private boolean wantsLeader;
    private List<String> preferredTeammates;
    private List<Integer> personalityScores;
    private List<Integer> developmentScores;

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
                .personalityScores(user.getPersonalityScores())
                .developmentScores(user.getDevelopmentScores())
                .build();
    }
}
