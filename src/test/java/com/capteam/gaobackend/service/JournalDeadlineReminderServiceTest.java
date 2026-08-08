package com.capteam.gaobackend.service;

import com.capteam.gaobackend.config.JournalReminderProperties;
import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserFcmToken;
import com.capteam.gaobackend.enums.*;
import com.capteam.gaobackend.repository.JournalEntryRepository;
import com.capteam.gaobackend.repository.NotificationLogRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserFcmTokenRepository;
import com.capteam.gaobackend.service.push.PushDispatchResult;
import com.capteam.gaobackend.service.push.PushNotificationGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalDeadlineReminderServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private UserFcmTokenRepository userFcmTokenRepository;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private PushNotificationGateway pushNotificationGateway;

    private JournalDeadlineReminderService journalDeadlineReminderService;

    @BeforeEach
    void setUp() {
        LocalDateTime nowInSeoul = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0);
        LocalTime deadlineTime = nowInSeoul.toLocalTime().plusMinutes(30);
        JournalReminderProperties properties = new JournalReminderProperties(
                true,
                "0 * * * * *",
                "Asia/Seoul",
                nowInSeoul.getDayOfWeek().getValue(),
                deadlineTime,
                30,
                "일지 마감 30분 전입니다",
                "제출해주세요"
        );
        journalDeadlineReminderService = new JournalDeadlineReminderService(
                properties,
                teamRepository,
                teamUserRepository,
                journalEntryRepository,
                userFcmTokenRepository,
                notificationLogRepository,
                pushNotificationGateway
        );
    }

    @Test
    void sendsReminderToUnsubmittedMemberAndLogsSuccess() {
        Team team = approvedTeam(1L);
        User user = student("stu2301", "장준민");
        TeamUser teamUser = teamUser(team, user);
        UserFcmToken token = UserFcmToken.builder()
                .user(user)
                .token("token-1")
                .build();

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(teamUser));
        when(journalEntryRepository.existsByJournalTeamIdAndJournalDateAndWriterUserId(eq(1L), any(LocalDate.class), eq("stu2301")))
                .thenReturn(false);
        when(notificationLogRepository.existsByUserUserIdAndTypeAndTargetId(eq("stu2301"), eq(NotificationType.JOURNAL_DEADLINE), any()))
                .thenReturn(false);
        when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userFcmTokenRepository.findAllByUserUserId("stu2301"))
                .thenReturn(List.of(token));
        when(pushNotificationGateway.sendToTokens(eq(List.of("token-1")), any()))
                .thenReturn(new PushDispatchResult(1, 0, List.of()));

        journalDeadlineReminderService.sendJournalDeadlineReminders();

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(logCaptor.getValue().getTargetId()).startsWith("1:");
        verify(pushNotificationGateway).sendToTokens(eq(List.of("token-1")), any());
    }

    @Test
    void skipsDuplicateNotificationKey() {
        Team team = approvedTeam(1L);
        User user = student("stu2301", "장준민");
        TeamUser teamUser = teamUser(team, user);

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(teamUser));
        when(journalEntryRepository.existsByJournalTeamIdAndJournalDateAndWriterUserId(eq(1L), any(LocalDate.class), eq("stu2301")))
                .thenReturn(false);
        when(notificationLogRepository.existsByUserUserIdAndTypeAndTargetId(eq("stu2301"), eq(NotificationType.JOURNAL_DEADLINE), any()))
                .thenReturn(true);

        journalDeadlineReminderService.sendJournalDeadlineReminders();

        verify(notificationLogRepository, never()).save(any(NotificationLog.class));
        verify(pushNotificationGateway, never()).sendToTokens(any(), any());
    }

    @Test
    void logsFailureWhenNoActiveTokenExists() {
        Team team = approvedTeam(1L);
        User user = student("stu2301", "장준민");
        TeamUser teamUser = teamUser(team, user);

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(teamUser));
        when(journalEntryRepository.existsByJournalTeamIdAndJournalDateAndWriterUserId(eq(1L), any(LocalDate.class), eq("stu2301")))
                .thenReturn(false);
        when(notificationLogRepository.existsByUserUserIdAndTypeAndTargetId(eq("stu2301"), eq(NotificationType.JOURNAL_DEADLINE), any()))
                .thenReturn(false);
        when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userFcmTokenRepository.findAllByUserUserId("stu2301"))
                .thenReturn(List.of());

        journalDeadlineReminderService.sendJournalDeadlineReminders();

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(logCaptor.getValue().getErrorMessage()).isEqualTo("등록된 FCM 토큰이 없습니다.");
        verify(pushNotificationGateway, never()).sendToTokens(any(), any());
    }

    private Team approvedTeam(Long id) {
        Team team = Team.builder()
                .teamName(id + "팀")
                .status(TeamStatus.APPROVED)
                .grade(Grade.GRADE_2)
                .build();
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }

    private TeamUser teamUser(Team team, User user) {
        return TeamUser.builder()
                .team(team)
                .user(user)
                .studentRole(StudentRole.BACKEND)
                .leaderRole(LeaderRole.MEMBER)
                .build();
    }

    private User student(String userId, String name) {
        return User.builder()
                .userId(userId)
                .name(name)
                .accountRole(AccountRole.STUDENT)
                .grade(Grade.GRADE_2)
                .build();
    }
}
