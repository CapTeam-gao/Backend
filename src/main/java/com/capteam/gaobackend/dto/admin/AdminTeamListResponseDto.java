package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
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
public class AdminTeamListResponseDto {
    private Long teamId;
    private String teamName;
    private Grade grade;    //학년
    private String serviceName;
    private Map<StudentRole, Long> roleCount;   //역할별 인원수 예시) {FRONTEND=1, BACKEND=2}
    private List<TeamMemberDto> members;    //멤버 카드 목록 div같은걸로 묶어 놓은데 데이터 넣을거임

    @Getter
    @AllArgsConstructor
    @Builder
    public static class TeamMemberDto { //div안에 넣을 정보들
        private String name;
        private StudentRole studentRole;
        private LeaderRole leaderRole;
    }

    public static AdminTeamListResponseDto from(Team team, TeamProject teamProject, List<TeamUser> teamUsers) {
        return AdminTeamListResponseDto.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .grade(team.getGrade())
                .serviceName(teamProject != null ? teamProject.getServiceName() : null)
                .roleCount(teamUsers.stream()
                        .collect(Collectors.groupingBy(
                                TeamUser::getStudentRole,
                                Collectors.counting())))
                .members(teamUsers.stream() //리스트니까 스트림 순회
                        .map(teamUser -> TeamMemberDto.builder()
                                .name(teamUser.getUser().getName())
                                .studentRole(teamUser.getStudentRole())
                                .leaderRole(teamUser.getLeaderRole())
                                .build())
                        .toList())
                .build();
    }
}
