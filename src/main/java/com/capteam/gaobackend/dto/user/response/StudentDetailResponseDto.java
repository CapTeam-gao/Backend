package com.capteam.gaobackend.dto.user.response;


import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.StudentLevel;
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
    private StudentLevel studentLevel; // AI 분석 실력 (상/중/하), 어드민만 조회 가능

    public static StudentDetailResponseDto from(TeamUser teamUser, UserAnalysis userAnalysis) {
        return StudentDetailResponseDto.builder()
                .userId(teamUser.getUser().getUserId())
                .studentRole(teamUser.getStudentRole())
                .skill(teamUser.getUser().getSkill())
                .teamName(teamUser.getTeam().getTeamName() != null ? teamUser.getTeam().getTeamName() : null)
                .analysisResult(userAnalysis.getAnalysisResult() != null ? userAnalysis.getAnalysisResult() : null)
                .studentLevel(userAnalysis.getStudentLevel())
                .build();
    }
}
