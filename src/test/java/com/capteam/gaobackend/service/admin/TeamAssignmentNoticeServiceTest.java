package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.TeamStatus;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamAssignmentNoticeServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private TeamProjectRepository teamProjectRepository;
    @Mock private AdminNoticeService adminNoticeService;

    private TeamAssignmentNoticeService teamAssignmentNoticeService;

    @BeforeEach
    void setUp() {
        teamAssignmentNoticeService = new TeamAssignmentNoticeService(
                teamRepository,
                teamUserRepository,
                teamProjectRepository,
                adminNoticeService
        );
    }

    @Test
    void createsMarkdownNoticeUsingProjectTeamNameFirst() {
        Team team = Team.builder()
                .teamName("1팀")
                .grade(Grade.GRADE_2)
                .status(TeamStatus.APPROVED)
                .build();
        ReflectionTestUtils.setField(team, "id", 1L);

        TeamProject project = TeamProject.builder()
                .team(team)
                .teamName("가오팀")
                .serviceName("서비스")
                .serviceIntro("소개")
                .mainFeatures("기능")
                .build();
        TeamUser frontendMember = member(team, "허재원", StudentRole.FRONTEND);
        TeamUser backendMember = member(team, "양원우", StudentRole.BACKEND);

        when(teamRepository.findByGrade(Grade.GRADE_2)).thenReturn(List.of(team));
        when(teamProjectRepository.findByTeamId(1L)).thenReturn(Optional.of(project));
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(frontendMember, backendMember));

        teamAssignmentNoticeService.createNotice(Grade.GRADE_2);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(adminNoticeService).createTeamAssignmentNotice(contentCaptor.capture());
        assertThat(contentCaptor.getValue())
                .contains("### 가오팀")
                .contains("- 학년: 2학년")
                .contains("  - 허재원 / 프론트엔드")
                .contains("  - 양원우 / 백엔드")
                .doesNotContain("### 1팀");
    }

    @Test
    void fallsBackToTeamNameWhenProjectDoesNotExist() {
        Team team = Team.builder()
                .teamName("2팀")
                .grade(Grade.GRADE_3)
                .status(TeamStatus.APPROVED)
                .build();
        ReflectionTestUtils.setField(team, "id", 2L);

        when(teamRepository.findByGrade(Grade.GRADE_3)).thenReturn(List.of(team));
        when(teamProjectRepository.findByTeamId(2L)).thenReturn(Optional.empty());
        when(teamUserRepository.findByTeamId(2L)).thenReturn(List.of());

        teamAssignmentNoticeService.createNotice(Grade.GRADE_3);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(adminNoticeService).createTeamAssignmentNotice(contentCaptor.capture());
        assertThat(contentCaptor.getValue())
                .contains("### 2팀")
                .contains("- 학년: 3학년");
    }

    private TeamUser member(Team team, String name, StudentRole role) {
        User user = User.builder()
                .userId(name)
                .name(name)
                .accountRole(AccountRole.STUDENT)
                .build();
        return TeamUser.builder()
                .team(team)
                .user(user)
                .studentRole(role)
                .leaderRole(LeaderRole.MEMBER)
                .build();
    }
}
