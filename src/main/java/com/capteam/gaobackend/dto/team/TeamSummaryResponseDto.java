package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@Builder
public class TeamSummaryResponseDto {
    // 팀 고유 id를 내려주는 필드입니다.
    private Long teamId;

    // 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 팀 학년을 내려주는 필드입니다.
    private Grade grade;

    // 팀 상태를 내려주는 필드입니다.
    private String status;

    // 팀 프로젝트 서비스명을 내려주는 필드입니다.
    private String serviceName;

    // 팀원 수를 내려주는 필드입니다.
    private int memberCount;

    // 팀 안의 개발 역할별 인원 수를 내려주는 필드입니다.
    private Map<StudentRole, Long> roleCount;

    // Team, TeamProject, TeamUser 목록을 팀 요약 응답 DTO로 변환하는 기능입니다.
    public static TeamSummaryResponseDto from(Team team, TeamProject teamProject, List<TeamUser> teamUsers) {
        return TeamSummaryResponseDto.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .grade(team.getGrade())
                .status(team.getStatus().name())
                .serviceName(teamProject != null ? teamProject.getServiceName() : null)
                .memberCount(teamUsers.size())
                .roleCount(teamUsers.stream()
                        .collect(Collectors.groupingBy(
                                TeamUser::getStudentRole,
                                Collectors.counting())))
                .build();
    }
}
