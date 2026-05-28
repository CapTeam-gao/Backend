package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.entity.TeamUser;
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
public class AdminStudentDetailResponseDto {

    private String userId;
    private String name;
    private Grade grade;
    private String teamName;
    private StudentRole studentRole;
    private LeaderRole leaderRole;
    private StudentLevel studentLevel;
    private List<String> skill;
    private List<String> experience;
    private List<String> preferredTeammates;
    private boolean wantsLeader;
    private String analysisResult;

    public static AdminStudentDetailResponseDto from(TeamUser teamUser, UserAnalysis userAnalysis) {
        return AdminStudentDetailResponseDto.builder()
                .userId(teamUser.getUser().getUserId())
                .name(teamUser.getUser().getName())
                .grade(teamUser.getUser().getGrade())
                .teamName(teamUser.getTeam().getTeamName())
                .studentRole(teamUser.getStudentRole())
                .leaderRole(teamUser.getLeaderRole())
                .studentLevel(userAnalysis == null ? null : userAnalysis.getStudentLevel())
                .skill(teamUser.getUser().getSkill())
                .experience(teamUser.getUser().getExperience())
                .preferredTeammates(teamUser.getUser().getPreferredTeammates())
                .wantsLeader(teamUser.getUser().isWantsLeader())
                .analysisResult(userAnalysis == null ? null : userAnalysis.getAnalysisResult())
                .build();
    }
}
