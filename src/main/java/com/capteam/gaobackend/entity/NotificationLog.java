package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.NotificationStatus;
import com.capteam.gaobackend.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notification_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_log_user_type_target",
                        columnNames = {"user_id", "notification_type", "target_id"}
                )
        }
)
public class NotificationLog extends BaseTimeEntity {

    // 알림 이력 개별 row를 조회 API에서 식별하기 위한 PK입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 학생에게 보낸 알림인지 남겨야 관리자 이력 조회와 중복 방지가 가능합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 알림 종류별로 후속 동작과 클릭 이동 경로가 달라지므로 type을 enum으로 분리합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType type;

    // 같은 사용자에게 같은 대상에 대한 알림을 다시 보내지 않기 위한 대상 식별자입니다.
    // 예: 공지면 noticeId, 일지 알림이면 팀/날짜 조합 문자열을 저장합니다.
    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    // 발송 성공/실패 여부를 저장해 관리자 이력 화면과 재시도 판단에 사용합니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    // 실제 발송을 시도한 시각을 저장해 "언제 보냈는지"를 사용자 이벤트와 대조할 수 있게 합니다.
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    // FCM 실패 메시지나 내부 예외 원인을 남겨 운영 중 실패 원인 추적에 사용합니다.
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    // 아래 호환 필드들은 현재 브랜치 DB에 이미 존재하는 NOT NULL 제약을 흡수하기 위한 내부 저장값입니다.
    // 상세 설계서의 외부 계약은 user/type/targetId/status/sentAt/errorMessage만 사용합니다.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "notification_key", nullable = false, length = 255)
    private String notificationKey;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(name = "target_token_count", nullable = false)
    private int targetTokenCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "type", length = 50)
    private String legacyType;

    @Builder
    public NotificationLog(
            User user,
            NotificationType type,
            String targetId,
            NotificationStatus status,
            LocalDateTime sentAt,
            String errorMessage,
            Team team,
            String notificationKey,
            LocalDateTime scheduledAt,
            LocalDate targetDate,
            String title,
            String body,
            int targetTokenCount,
            int successCount,
            int failureCount,
            String failureReason,
            String channel,
            String legacyType
    ) {
        this.user = user;
        this.type = type;
        this.targetId = targetId;
        this.status = status;
        this.sentAt = sentAt;
        this.errorMessage = errorMessage;
        this.team = team;
        this.notificationKey = notificationKey == null ? type.name() + ":" + user.getUserId() + ":" + targetId : notificationKey;
        this.scheduledAt = scheduledAt == null ? sentAt : scheduledAt;
        this.targetDate = targetDate == null ? sentAt.toLocalDate() : targetDate;
        this.title = title == null ? "" : title;
        this.body = body == null ? "" : body;
        this.targetTokenCount = targetTokenCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.failureReason = failureReason;
        this.channel = channel == null ? "FCM" : channel;
        this.legacyType = legacyType;
    }

    // 알림 발송이 성공한 뒤 성공 상태와 발송 시각을 기록하는 기능입니다.
    public void markSent(LocalDateTime sentAt) {
        markSent(sentAt, 1, 0);
    }

    // 멀티 토큰 발송처럼 성공/실패 개수를 함께 남겨야 할 때 사용하는 기능입니다.
    public void markSent(LocalDateTime sentAt, int successCount, int failureCount) {
        this.status = NotificationStatus.SENT;
        this.sentAt = sentAt;
        this.errorMessage = null;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.failureReason = null;
    }

    // 알림 발송 실패 시 상태와 원인을 함께 남겨 후속 분석에 사용합니다.
    public void markFailed(LocalDateTime sentAt, String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.sentAt = sentAt;
        this.errorMessage = errorMessage;
        this.successCount = 0;
        this.failureCount = Math.max(this.targetTokenCount, 1);
        this.failureReason = errorMessage;
    }
}
