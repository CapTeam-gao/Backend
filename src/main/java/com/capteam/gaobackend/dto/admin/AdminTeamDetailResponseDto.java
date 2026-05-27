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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@Builder
public class AdminTeamDetailResponseDto {
    private Long teamId;
    private String teamName;
    private Grade grade;
    private String serviceName;
    private Map<StudentRole, Long> roleCount;
    private String serviceIntro;
    private String mainFeatures;
    private List<TeamMemberDto> members;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class TeamMemberDto {
        private String userId;
        private String name;
        private StudentRole studentRole;
        private LeaderRole leaderRole;
        private List<String> skill;
    }

    public static AdminTeamDetailResponseDto from(Team team, TeamProject teamProject, List<TeamUser> teamUsers) {
        Map<StudentRole, Long> roleCount = teamUsers.stream()
                // 같은 역할끼리 묶고, 역할별 인원 수를 센다.
                .collect(Collectors.groupingBy(
                        TeamUser::getStudentRole,
                        Collectors.counting()
                ));

        return AdminTeamDetailResponseDto.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .grade(team.getGrade())
                .serviceName(teamProject != null ? teamProject.getServiceName() : null)
                .roleCount(roleCount)
                .serviceIntro(teamProject != null ? teamProject.getServiceIntro() : null)
                .mainFeatures(teamProject != null ? teamProject.getMainFeatures() : null)
                .members(teamUsers.stream()
                        // 팀장을 먼저 보여주고, 같은 역할이면 이름순으로 정렬한다. 오름/내림 차순하는 메서드
                        .sorted(Comparator
                                .comparing(TeamUser::getLeaderRole)
                                .thenComparing(teamUser -> teamUser.getUser().getName()))
                        // TeamUser 엔티티에서 화면에 필요한 멤버 정보만 꺼내 DTO로 바꾼다.
                        .map(teamUser -> TeamMemberDto.builder()
                                .userId(teamUser.getUser().getUserId())
                                .name(teamUser.getUser().getName())
                                .studentRole(teamUser.getStudentRole())
                                .leaderRole(teamUser.getLeaderRole())
                                .skill(teamUser.getUser().getSkill())
                                .build())
                        .toList())
                .build();
    }
}
