package com.capteam.gaobackend.service;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private TeamProjectRepository teamProjectRepository;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(
                userRepository,
                teamRepository,
                teamUserRepository,
                teamProjectRepository
        );
    }

    @Test
    void getMyTeamProjectReturnsNullWhenUserHasNoTeam() {
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.empty());

        var response = teamService.getMyTeamProject("stu2301");

        assertThat(response).isNull();
    }

    @Test
    void getMyTeamProjectReturnsProjectWhenUserHasTeamProject() {
        Team team = Team.builder()
                .teamName("1팀")
                .build();
        ReflectionTestUtils.setField(team, "id", 1L);

        User user = User.builder()
                .userId("stu2301")
                .name("장준민")
                .build();

        TeamUser teamUser = TeamUser.builder()
                .team(team)
                .user(user)
                .studentRole(StudentRole.BACKEND)
                .leaderRole(LeaderRole.MEMBER)
                .build();

        TeamProject teamProject = TeamProject.builder()
                .team(team)
                .teamName("가오팀")
                .serviceName("팀 빌딩")
                .serviceIntro("학생 팀 매칭")
                .mainFeatures("AI 팀 추천")
                .build();
        ReflectionTestUtils.setField(teamProject, "id", 10L);

        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));
        when(teamProjectRepository.findByTeamId(1L)).thenReturn(Optional.of(teamProject));

        var response = teamService.getMyTeamProject("stu2301");

        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getTeamName()).isEqualTo("가오팀");
        assertThat(response.getServiceName()).isEqualTo("팀 빌딩");
    }
}
