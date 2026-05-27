package com.capteam.gaobackend.dto.user.response;


import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class StudentDetailResponseDto {
    private String userId;
    private StudentRole studentRole;
    private List<String> skill;
    private String teamName;
    private String analysisResult;


    public static StudentDetailResponseDto from(TeamUser teamUser, UserAnalysis userAnalysis) {
        return StudentDetailResponseDto.builder()
                .userId(teamUser.getUser().getUserId())
                .studentRole(teamUser.getStudentRole())
                .skill(teamUser.getUser().getSkill())
                .teamName(teamUser.getTeam().getTeamName() != null ? teamUser.getTeam().getTeamName() : null )
                .analysisResult(userAnalysis.getAnalysisResult() != null ? userAnalysis.getAnalysisResult() : null)
                .build();
    }
}
