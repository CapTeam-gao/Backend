package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTeamServiceTest {

    @Mock private TeamUserRepository teamUserRepository;
    @Mock private TeamProjectRepository teamProjectRepository;
    @Mock private TeamRepository teamRepository;

    private AdminTeamService adminTeamService;

    @BeforeEach
    void setUp() {
        adminTeamService = new AdminTeamService(
                teamUserRepository,
                teamProjectRepository,
                teamRepository
        );
    }

    @Test
    void getsTeamDetailWithTeamIdQueriesInsteadOfFullTableScans() {
        Long teamId = 1L;
        Team team = org.mockito.Mockito.mock(Team.class);
        TeamProject teamProject = org.mockito.Mockito.mock(TeamProject.class);
        List<TeamUser> teamUsers = List.of();
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamProjectRepository.findByTeamId(teamId)).thenReturn(Optional.of(teamProject));
        when(teamUserRepository.findByTeamId(teamId)).thenReturn(teamUsers);

        adminTeamService.getTeamDetail(teamId);

        verify(teamProjectRepository).findByTeamId(teamId);
        verify(teamUserRepository).findByTeamId(teamId);
        verify(teamProjectRepository, never()).findAll();
        verify(teamUserRepository, never()).findAll();
    }
}
