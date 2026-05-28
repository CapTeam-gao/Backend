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
public class AdminStudentListResponseDto {

    private String userId;
    private String name;
    private Grade grade;
    private String teamName;
    private StudentRole studentRole;
    private LeaderRole leaderRole;
    private StudentLevel studentLevel;
    private List<String> skill;

    public static AdminStudentListResponseDto from(TeamUser teamUser) {
        return from(teamUser, null);
    }

    public static AdminStudentListResponseDto from(TeamUser teamUser, UserAnalysis userAnalysis) {
        return AdminStudentListResponseDto.builder()
                .userId(teamUser.getUser().getUserId())
                .name(teamUser.getUser().getName())
                .grade(teamUser.getUser().getGrade())
                .teamName(teamUser.getTeam().getTeamName())
                .studentRole(teamUser.getStudentRole())
                .leaderRole(teamUser.getLeaderRole())
                .studentLevel(userAnalysis == null ? null : userAnalysis.getStudentLevel())
                .skill(teamUser.getUser().getSkill())
                .build();
    }
}
