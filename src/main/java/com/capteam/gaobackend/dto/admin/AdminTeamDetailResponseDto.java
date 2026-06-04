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
    // 팀 고유 id를 내려주는 필드입니다.
    private Long teamId;

    // 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 팀 학년을 내려주는 필드입니다.
    private Grade grade;

    // 팀 프로젝트 서비스 이름을 내려주는 필드입니다.
    private String serviceName;

    // 팀 안의 개발 역할별 인원 수를 내려주는 필드입니다.
    private Map<StudentRole, Long> roleCount;

    // 팀 프로젝트 서비스 소개를 내려주는 필드입니다.
    private String serviceIntro;

    // 팀 프로젝트 주요 기능을 내려주는 필드입니다.
    private String mainFeatures;

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

        // 팀원의 기술 스택 목록을 내려주는 필드입니다.
        private List<String> skill;
    }

    // Team, TeamProject, TeamUser 목록을 관리자 팀 상세 응답 DTO로 변환하는 기능입니다.
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
