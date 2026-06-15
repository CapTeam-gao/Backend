package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.admin.AdminTeamDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminTeamListResponseDto;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.exception.TeamNotFoundException;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTeamService {

    // 팀별 멤버 목록과 역할 정보를 조회하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 팀 프로젝트 소개/서비스명을 조회하는 Repository 필드입니다.
    private final TeamProjectRepository teamProjectRepository;

    // 팀 목록과 팀 상세 기본 정보를 조회하는 Repository 필드입니다.
    private final TeamRepository teamRepository;

//    public AdminTeamListResponseDto createTeam() {
//
//    }

    // 관리자가 전체 팀 목록을 프로젝트 정보와 팀원 요약까지 함께 조회하는 기능입니다.
    public List<AdminTeamListResponseDto> getTeamList() {

        //전체 팀 조회
        List<Team> teams = teamRepository.findAll();

        var teamUsersMap = teamUserRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(tu -> tu.getTeam().getId()));
                //Collectors.groupingBy가 학생들 아이디 즉 학번으로 같은 팀의 학생들을 모아줌

        var teamProjectMap = teamProjectRepository.findAll()
                .stream()
                .collect(Collectors.toMap(teamProject -> teamProject.getTeam().getId(), teamProject -> teamProject));


        // 스트림으로 순회 뒤 디티오로 변환
        return teams.stream()
                .map(team -> AdminTeamListResponseDto.from(
                        team,
                        teamProjectMap.get(team.getId()),
                        teamUsersMap.getOrDefault(team.getId(),List.of())
                )).toList();
    }


    // 관리자가 특정 팀의 프로젝트 정보와 팀원 상세 목록을 조회하는 기능입니다.
    public AdminTeamDetailResponseDto getTeamDetail(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(TeamNotFoundException::new);

        TeamProject teamProject = teamProjectRepository.findByTeamId(teamId)
                .orElse(null);
        List<TeamUser> teamUsers = teamUserRepository.findByTeamId(teamId);

        return AdminTeamDetailResponseDto.from(team, teamProject, teamUsers);
    }

}
