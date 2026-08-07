package com.capteam.gaobackend.service;

import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.*;
import com.capteam.gaobackend.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNotificationLogServiceTest {

    @Mock private NotificationLogRepository notificationLogRepository;

    private AdminNotificationLogService adminNotificationLogService;

    @BeforeEach
    void setUp() {
        adminNotificationLogService = new AdminNotificationLogService(notificationLogRepository);
    }

    @Test
    void getLogsMapsRepositoryResult() {
        NotificationLog log = notificationLog(1L, "1:2026-08-07");
        when(notificationLogRepository.searchLogs(
                eq(NotificationType.JOURNAL_DEADLINE),
                eq(NotificationStatus.SENT),
                eq("1:2026-08-07"),
                eq("stu2301")
        )).thenReturn(List.of(log));

        var response = adminNotificationLogService.getLogs(
                NotificationType.JOURNAL_DEADLINE,
                NotificationStatus.SENT,
                "1:2026-08-07",
                "stu2301"
        );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getUserId()).isEqualTo("stu2301");
        assertThat(response.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void getLogDetailReturnsSingleLog() {
        NotificationLog log = notificationLog(10L, "1:2026-08-07");
        when(notificationLogRepository.findById(10L)).thenReturn(Optional.of(log));

        var response = adminNotificationLogService.getLogDetail(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTargetId()).isEqualTo("1:2026-08-07");
    }

    private NotificationLog notificationLog(Long id, String targetId) {
        User user = User.builder()
                .userId("stu2301")
                .name("장준민")
                .accountRole(AccountRole.STUDENT)
                .grade(Grade.GRADE_2)
                .build();

        NotificationLog log = NotificationLog.builder()
                .user(user)
                .type(NotificationType.JOURNAL_DEADLINE)
                .targetId(targetId)
                .status(NotificationStatus.FAILED)
                .sentAt(LocalDateTime.of(2026, 8, 7, 23, 0))
                .errorMessage("초기 실패")
                .build();
        log.markSent(LocalDateTime.of(2026, 8, 7, 23, 0));

        ReflectionTestUtils.setField(log, "id", id);
        ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 8, 7, 23, 0));
        return log;
    }
}
