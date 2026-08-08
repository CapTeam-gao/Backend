package com.capteam.gaobackend.service;

import com.capteam.gaobackend.config.JournalReminderProperties;
import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.NotificationStatus;
import com.capteam.gaobackend.enums.NotificationType;
import com.capteam.gaobackend.enums.TeamStatus;
import com.capteam.gaobackend.repository.JournalEntryRepository;
import com.capteam.gaobackend.repository.NotificationLogRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserFcmTokenRepository;
import com.capteam.gaobackend.service.push.PushDispatchResult;
import com.capteam.gaobackend.service.push.PushMessageRequest;
import com.capteam.gaobackend.service.push.PushNotificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalDeadlineReminderService {

    // 마감 시각, 제목, 본문 같은 운영 설정을 코드 수정 없이 바꾸기 위해 분리한 프로퍼티입니다.
    private final JournalReminderProperties reminderProperties;

    // 승인된 실제 팀만 마감 알림 대상으로 삼기 위해 팀 저장소를 조회합니다.
    private final TeamRepository teamRepository;

    // 팀별 학생 목록을 순회해 미제출자를 찾기 위해 팀원 저장소를 사용합니다.
    private final TeamUserRepository teamUserRepository;

    // 이미 오늘 일지를 제출한 학생은 알림 대상에서 제외하기 위해 일지 저장소를 조회합니다.
    private final JournalEntryRepository journalEntryRepository;

    // 사용자에게 연결된 등록 토큰 전체를 읽어 FCM 발송 대상으로 사용합니다.
    private final UserFcmTokenRepository userFcmTokenRepository;

    // 학생별 발송 이력을 남기고 중복 발송을 차단하기 위해 알림 로그 저장소를 사용합니다.
    private final NotificationLogRepository notificationLogRepository;

    // 실제 FCM 전송을 Firebase 구현체에 위임해 서비스 로직과 외부 연동을 분리합니다.
    private final PushNotificationGateway pushNotificationGateway;

    // 매 분마다 현재 시각이 "마감 30분 전"인지 확인하고, 조건이 맞으면 미제출 학생에게 자동 발송하는 기능입니다.
    @Scheduled(cron = "${journal.reminder.cron:0 * * * * *}", zone = "${journal.reminder.zone:Asia/Seoul}")
    @Transactional
    public void sendJournalDeadlineReminders() {
        if (!reminderProperties.enabled()) {
            return;
        }

        // 서버 시간대가 달라져도 운영 기준 시각이 흔들리지 않게 프로퍼티 zone으로 현재 시각을 고정합니다.
        ZoneId zoneId = ZoneId.of(reminderProperties.zone());
        LocalDateTime now = LocalDateTime.now(zoneId).withSecond(0).withNano(0);

        // 캡스톤 일지 작성 요일(기본 수요일)에만 발송 — 요일 조건이 없으면 매일 발송되는 버그가 있었음.
        if (now.getDayOfWeek().getValue() != reminderProperties.dayOfWeek()) {
            return;
        }

        LocalDateTime reminderAt = LocalDateTime.of(
                now.toLocalDate(),
                reminderProperties.deadlineTime().minusMinutes(reminderProperties.minutesBefore())
        );

        if (!now.equals(reminderAt)) {
            return;
        }

        LocalDate targetDate = now.toLocalDate();
        List<Team> approvedTeams = teamRepository.findAll().stream()
                .filter(team -> team.getStatus() == TeamStatus.APPROVED)
                .toList();

        for (Team team : approvedTeams) {
            for (TeamUser teamUser : teamUserRepository.findByTeamId(team.getId())) {
                User user = teamUser.getUser();
                if (journalEntryRepository.existsByJournalTeamIdAndJournalDateAndWriterUserId(
                        team.getId(),
                        targetDate,
                        user.getUserId()
                )) {
                    continue;
                }

                sendReminderToUser(team, user, targetDate, now);
            }
        }
    }

    // 한 학생에게 한 번의 일지 마감 알림을 만들고, 중복이면 건너뛰고, 실패 원인까지 로그로 남기는 기능입니다.
    private void sendReminderToUser(Team team, User user, LocalDate targetDate, LocalDateTime scheduledAt) {
        // 사용자 기준 중복 방지를 위해 팀/날짜 조합을 문자열 targetId로 통일합니다.
        String targetId = buildTargetId(team.getId(), targetDate);
        if (notificationLogRepository.existsByUserUserIdAndTypeAndTargetId(
                user.getUserId(),
                NotificationType.JOURNAL_DEADLINE,
                targetId
        )) {
            return;
        }

        // 같은 사용자가 여러 환경에서 등록한 토큰이 있을 수 있어 distinct로 정리한 뒤 한 번에 발송합니다.
        List<String> registeredTokens = userFcmTokenRepository.findAllByUserUserId(user.getUserId())
                .stream()
                .map(token -> token.getToken())
                .distinct()
                .toList();

        if (registeredTokens.isEmpty()) {
            notificationLogRepository.save(NotificationLog.builder()
                    .user(user)
                    .team(team)
                    .type(NotificationType.JOURNAL_DEADLINE)
                    .targetId(targetId)
                    .status(NotificationStatus.FAILED)
                    .sentAt(scheduledAt)
                    .scheduledAt(scheduledAt)
                    .targetDate(targetDate)
                    .title(reminderProperties.title())
                    .body(reminderProperties.body())
                    .targetTokenCount(0)
                    .errorMessage("등록된 FCM 토큰이 없습니다.")
                    .legacyType("JOURNAL_DEADLINE_REMINDER")
                    .build());
            return;
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", NotificationType.JOURNAL_DEADLINE.name());
        data.put("targetId", targetId);
        data.put("clickUrl", "/journals");

        PushDispatchResult result = pushNotificationGateway.sendToTokens(
                registeredTokens,
                new PushMessageRequest(reminderProperties.title(), reminderProperties.body(), data)
        );

        String errorMessage = result.failureReasons().isEmpty()
                ? null
                : String.join(" | ", result.failureReasons());

        NotificationLog notificationLog = NotificationLog.builder()
                .user(user)
                .team(team)
                .type(NotificationType.JOURNAL_DEADLINE)
                .targetId(targetId)
                .status(NotificationStatus.FAILED)
                .sentAt(scheduledAt)
                .scheduledAt(scheduledAt)
                .targetDate(targetDate)
                .title(reminderProperties.title())
                .body(reminderProperties.body())
                .targetTokenCount(registeredTokens.size())
                .errorMessage(errorMessage)
                .legacyType("JOURNAL_DEADLINE_REMINDER")
                .build();

        if (result.successCount() > 0) {
            notificationLog.markSent(scheduledAt, result.successCount(), result.failureCount());
        } else {
            notificationLog.markFailed(scheduledAt, errorMessage);
        }

        notificationLogRepository.save(notificationLog);
    }

    // 같은 날짜 같은 팀에 대한 일지 마감 알림을 하나의 대상 값으로 묶기 위한 식별자입니다.
    private String buildTargetId(Long teamId, LocalDate targetDate) {
        return teamId + ":" + targetDate;
    }
}
