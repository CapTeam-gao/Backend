package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.TeamStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class TeamAssignmentNoticeServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private AdminNoticeService adminNoticeService;

    private TeamAssignmentNoticeService teamAssignmentNoticeService;

    @BeforeEach
    void setUp() {
        teamAssignmentNoticeService = new TeamAssignmentNoticeService(
                teamRepository,
                teamUserRepository,
                adminNoticeService
        );
    }

    @Test
    void createsGradeSpecificMarkdownWithLeaderFirstAndRoleSummary() {
        Team team = Team.builder()
                .teamName("1팀")
                .grade(Grade.GRADE_2)
                .status(TeamStatus.APPROVED)
                .build();
        ReflectionTestUtils.setField(team, "id", 1L);

        TeamUser frontendMember = member(team, "장준민", StudentRole.FRONTEND, LeaderRole.MEMBER);
        TeamUser leader = member(team, "허재원", StudentRole.FRONTEND, LeaderRole.LEADER);
        TeamUser backendMember = member(team, "양원우", StudentRole.BACKEND, LeaderRole.MEMBER);

        when(teamRepository.findByGrade(Grade.GRADE_2)).thenReturn(List.of(team));
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(frontendMember, leader, backendMember));

        teamAssignmentNoticeService.createNotice(Grade.GRADE_2);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(adminNoticeService).createTeamAssignmentNotice(eq(Grade.GRADE_2), contentCaptor.capture());
        assertThat(contentCaptor.getValue())
                .startsWith("# 캡스톤 2학년 팀 배정 결과 안내")
                .contains("## 확인 사항")
                .contains("### 1팀")
                .contains("허재원 팀장 · 장준민 · 양원우")
                .contains("프론트엔드 2명 · 백엔드 1명")
                .doesNotContain("- 학년:");
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
        when(teamUserRepository.findByTeamId(2L)).thenReturn(List.of());

        teamAssignmentNoticeService.createNotice(Grade.GRADE_3);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(adminNoticeService).createTeamAssignmentNotice(eq(Grade.GRADE_3), contentCaptor.capture());
        assertThat(contentCaptor.getValue())
                .contains("### 2팀")
                .startsWith("# 캡스톤 3학년 팀 배정 결과 안내");
    }

    private TeamUser member(Team team, String name, StudentRole role, LeaderRole leaderRole) {
        User user = User.builder()
                .userId(name)
                .name(name)
                .accountRole(AccountRole.STUDENT)
                .build();
        return TeamUser.builder()
                .team(team)
                .user(user)
                .studentRole(role)
                .leaderRole(leaderRole)
                .build();
    }
}
