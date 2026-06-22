package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.dashboard.AdminDashboardResponseDto;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import com.capteam.gaobackend.repository.JournalEntryRepository;
import com.capteam.gaobackend.repository.JournalRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private JournalRepository journalRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private UserRepository userRepository;
    @Mock private NoticeService noticeService;
    @Mock private ChatPresenceService chatPresenceService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                teamRepository,
                chatRoomRepository,
                journalRepository,
                journalEntryRepository,
                teamUserRepository,
                userRepository,
                noticeService,
                chatPresenceService
        );
        when(journalRepository.countDistinctTeamByDate(any(LocalDate.class))).thenReturn(0L);
        when(userRepository.countByAccountRole(AccountRole.STUDENT)).thenReturn(0L);
        when(noticeService.hasUnreadNotice("admin")).thenReturn(false);
    }

    @Test
    void reportsEachGradeSeparatelyWhenOnlyGrade2TeamsExist() {
        when(teamRepository.count()).thenReturn(2L);
        when(teamRepository.countByGrade(Grade.GRADE_2)).thenReturn(2L);
        when(teamRepository.countByGrade(Grade.GRADE_3)).thenReturn(0L);

        AdminDashboardResponseDto response = dashboardService.getAdminDashboard("admin");

        assertThat(response.isGrade2TeamCreated()).isTrue();
        assertThat(response.isGrade3TeamCreated()).isFalse();
        assertThat(response.isTeamCreated()).isFalse();
    }

    @Test
    void marksOverallTeamCreationCompleteWhenBothGradesExist() {
        when(teamRepository.count()).thenReturn(4L);
        when(teamRepository.countByGrade(Grade.GRADE_2)).thenReturn(2L);
        when(teamRepository.countByGrade(Grade.GRADE_3)).thenReturn(2L);

        AdminDashboardResponseDto response = dashboardService.getAdminDashboard("admin");

        assertThat(response.isGrade2TeamCreated()).isTrue();
        assertThat(response.isGrade3TeamCreated()).isTrue();
        assertThat(response.isTeamCreated()).isTrue();
    }

    @Test
    void countsOnlyChatRoomsWithAtLeastOneOnlineMember() {
        when(teamRepository.count()).thenReturn(4L);
        when(chatRoomRepository.findAll()).thenReturn(List.of(
                chatRoom(1L, 101L),
                chatRoom(2L, 102L),
                chatRoom(3L, 103L),
                chatRoom(4L, 104L)
        ));
        when(chatPresenceService.countOnlineMembersByTeamId(1L)).thenReturn(0L);
        when(chatPresenceService.countOnlineMembersByTeamId(2L)).thenReturn(2L);
        when(chatPresenceService.countOnlineMembersByTeamId(3L)).thenReturn(0L);
        when(chatPresenceService.countOnlineMembersByTeamId(4L)).thenReturn(0L);

        AdminDashboardResponseDto response = dashboardService.getAdminDashboard("admin");

        assertThat(response.getActiveChatRoomCount()).isEqualTo(1L);
    }

    private ChatRoom chatRoom(Long teamId, Long roomId) {
        Team team = Team.builder()
                .teamName(teamId + "팀")
                .build();
        ReflectionTestUtils.setField(team, "id", teamId);

        ChatRoom room = ChatRoom.builder()
                .team(team)
                .build();
        ReflectionTestUtils.setField(room, "id", roomId);
        return room;
    }
}
