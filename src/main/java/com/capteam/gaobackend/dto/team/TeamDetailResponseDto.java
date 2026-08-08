package com.capteam.gaobackend.dto.team;

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
public class TeamDetailResponseDto {
    // 팀 고유 id를 내려주는 필드입니다.
    private Long teamId;

    // 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 팀 학년을 내려주는 필드입니다.
    private Grade grade;

    // 팀 상태를 내려주는 필드입니다.
    private String status;

    // 팀 프로젝트 기획서 정보를 내려주는 필드입니다.
    private MyTeamResponseDto.TeamProjectDto project;

    // 팀 안의 개발 역할별 인원 수를 내려주는 필드입니다.
    private Map<StudentRole, Long> roleCount;

    // 팀원 상세 목록을 내려주는 필드입니다.
    private List<TeamMemberDto> members;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class TeamMemberDto {
        // 팀원 userId를 내려주는 필드입니다.
        private String userId;

        // 팀원 이름을 내려주는 필드입니다.
        private String name;

        // 팀원의 개발 역할을 내려주는 필드입니다.
        private StudentRole studentRole;

        // 팀원의 팀장/팀원 역할을 내려주는 필드입니다.
        private LeaderRole leaderRole;

        // 팀원이 보유한 기술 스택 목록을 내려주는 필드입니다.
        private List<String> skill;

        // 팀원이 직접 적은 담당 업무입니다. 아직 안 적었으면 null입니다.
        private String assignedTask;

        // TeamUser 엔티티를 팀 상세 팀원 응답 DTO로 변환하는 기능입니다.
        public static TeamMemberDto from(TeamUser teamUser) {
            return TeamMemberDto.builder()
                    .userId(teamUser.getUser().getUserId())
                    .name(teamUser.getUser().getName())
                    .studentRole(teamUser.getStudentRole())
                    .leaderRole(teamUser.getLeaderRole())
                    .skill(teamUser.getUser().getSkill())
                    .assignedTask(teamUser.getAssignedTask())
                    .build();
        }
    }

    // Team, TeamProject, TeamUser 목록을 팀 상세 응답 DTO로 변환하는 기능입니다.
    public static TeamDetailResponseDto from(Team team, TeamProject teamProject, List<TeamUser> teamUsers) {
        return TeamDetailResponseDto.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .grade(team.getGrade())
                .status(team.getStatus().name())
                .project(MyTeamResponseDto.TeamProjectDto.from(teamProject))
                .roleCount(teamUsers.stream()
                        .collect(Collectors.groupingBy(
                                TeamUser::getStudentRole,
                                Collectors.counting())))
                .members(teamUsers.stream()
                        .sorted(Comparator
                                .comparing(TeamUser::getLeaderRole)
                                .thenComparing(teamUser -> teamUser.getUser().getName()))
                        .map(TeamMemberDto::from)
                        .toList())
                .build();
    }
}
