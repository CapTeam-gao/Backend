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
public class MyTeamResponseDto {
    // 로그인한 학생이 소속된 팀 고유 id를 내려주는 필드입니다.
    private Long teamId;

    // 로그인한 학생이 소속된 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 로그인한 학생이 소속된 팀 학년을 내려주는 필드입니다.
    private Grade grade;

    // 팀 승인 여부를 화면에서 판단할 수 있도록 내려주는 필드입니다.
    private String status;

    // 팀 프로젝트 기획서 정보를 내려주는 필드입니다.
    private TeamProjectDto project;

    // 팀 안의 개발 역할별 인원 수를 내려주는 필드입니다.
    private Map<StudentRole, Long> roleCount;

    // 로그인한 학생의 팀원 정보를 내려주는 필드입니다.
    private TeamMemberDto myMember;

    // 팀원 목록을 내려주는 필드입니다.
    private List<TeamMemberDto> members;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class TeamProjectDto {
        // 팀 프로젝트 기획서 고유 id를 내려주는 필드입니다.
        private Long projectId;

        // 기획서에 저장된 팀명을 내려주는 필드입니다.
        private String teamName;

        // 기획서에 저장된 서비스명을 내려주는 필드입니다.
        private String serviceName;

        // 기획서에 저장된 서비스 소개를 내려주는 필드입니다.
        private String serviceIntro;

        // 기획서에 저장된 주요 기능을 내려주는 필드입니다.
        private String mainFeatures;

        // TeamProject 엔티티를 내 팀 프로젝트 응답 DTO로 변환하는 기능입니다.
        public static TeamProjectDto from(TeamProject teamProject) {
            if (teamProject == null) {
                return null;
            }

            return TeamProjectDto.builder()
                    .projectId(teamProject.getId())
                    .teamName(teamProject.getTeamName())
                    .serviceName(teamProject.getServiceName())
                    .serviceIntro(teamProject.getServiceIntro())
                    .mainFeatures(teamProject.getMainFeatures())
                    .build();
        }
    }

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

        // TeamUser 엔티티를 내 팀 팀원 응답 DTO로 변환하는 기능입니다.
        public static TeamMemberDto from(TeamUser teamUser) {
            return TeamMemberDto.builder()
                    .userId(teamUser.getUser().getUserId())
                    .name(teamUser.getUser().getName())
                    .studentRole(teamUser.getStudentRole())
                    .leaderRole(teamUser.getLeaderRole())
                    .skill(teamUser.getUser().getSkill())
                    .build();
        }
    }

    // Team, TeamProject, TeamUser 목록을 로그인한 학생의 내 팀 상세 응답 DTO로 변환하는 기능입니다.
    public static MyTeamResponseDto from(Team team, TeamProject teamProject, TeamUser myTeamUser, List<TeamUser> teamUsers) {
        List<TeamMemberDto> members = teamUsers.stream()
                .sorted(Comparator
                        .comparing(TeamUser::getLeaderRole)
                        .thenComparing(teamUser -> teamUser.getUser().getName()))
                .map(TeamMemberDto::from)
                .toList();

        return MyTeamResponseDto.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .grade(team.getGrade())
                .status(team.getStatus().name())
                .project(TeamProjectDto.from(teamProject))
                .roleCount(teamUsers.stream()
                        .collect(Collectors.groupingBy(
                                TeamUser::getStudentRole,
                                Collectors.counting())))
                .myMember(TeamMemberDto.from(myTeamUser))
                .members(members)
                .build();
    }
}
