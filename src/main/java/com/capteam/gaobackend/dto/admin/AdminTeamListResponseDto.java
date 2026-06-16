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
    // 팀 고유 id를 내려주는 필드입니다.
    private Long teamId;

    // 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 프로젝트 기획서에 작성한 팀명을 내려주는 필드입니다.
    private String projectTeamName;

    // 팀 학년을 내려주는 필드입니다.
    private Grade grade;

    // 팀 프로젝트 서비스 이름을 내려주는 필드입니다.
    private String serviceName;

    // 역할별 인원 수를 내려주는 필드입니다. 예: {FRONTEND=1, BACKEND=2}
    private Map<StudentRole, Long> roleCount;

    // 팀 목록 카드에 표시할 멤버 요약 목록을 내려주는 필드입니다.
    private List<TeamMemberDto> members;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class TeamMemberDto { //div안에 넣을 정보들
        // 팀원 이름을 내려주는 필드입니다.
        private String name;

        // 팀원의 개발 역할을 내려주는 필드입니다.
        private StudentRole studentRole;

        // 팀원의 팀장/팀원 역할을 내려주는 필드입니다.
        private LeaderRole leaderRole;
    }

    // Team, TeamProject, TeamUser 목록을 관리자 팀 목록 응답 DTO로 변환하는 기능입니다.
    public static AdminTeamListResponseDto from(Team team, TeamProject teamProject, List<TeamUser> teamUsers) {
        return AdminTeamListResponseDto.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .projectTeamName(teamProject != null ? teamProject.getTeamName() : null)
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
