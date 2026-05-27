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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final TeamUserRepository teamUserRepository;
    private final TeamProjectRepository teamProjectRepository;
    private final TeamRepository teamRepository;

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


    //팀 정보 상세 확인 (어드민)
    public AdminTeamDetailResponseDto getTeamDetail(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(TeamNotFoundException::new);

        TeamProject teamProject = teamProjectRepository.findAll()
                .stream()
                .filter(project -> project.getTeam().getId().equals(teamId)) //Stream내에서 특정 조건을 지닌 값들만 필터링하는 메소드이다.
                .findFirst()// 오름차순 정렬
                .orElse(null);  //아니면 null


        List<TeamUser> teamUsers = teamUserRepository.findAll()
                .stream()
                .filter(teamUser -> teamUser.getTeam().getId().equals(teamId))  //같은 팀 유저끼리 모아주기 같은 팀 아이디 모아서
                .toList();

        return AdminTeamDetailResponseDto.from(team, teamProject, teamUsers);
    }

}
