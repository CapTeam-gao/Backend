package com.capteam.gaobackend.dto.notification.response;

import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.enums.NotificationStatus;
import com.capteam.gaobackend.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationLogResponseDto {

    // 관리자 이력 화면에서 개별 row를 여는 데 사용하는 PK입니다.
    private Long id;

    // 어떤 학생에게 보낸 기록인지 보여줘야 발송 대상을 추적할 수 있습니다.
    private String userId;

    // 운영자가 학생 이름으로도 실패 이력을 빠르게 찾을 수 있도록 함께 내려줍니다.
    private String userName;

    // 공지/채팅/일지 중 어떤 종류의 알림인지 구분하는 값입니다.
    private NotificationType type;

    // 같은 사용자에게 어떤 대상을 기준으로 중복 방지했는지 확인하려고 남기는 식별자입니다.
    private String targetId;

    // 발송 성공/실패 상태를 그대로 노출합니다.
    private NotificationStatus status;

    // 실제 발송을 시도한 시각입니다.
    private LocalDateTime sentAt;

    // 실패했을 때 원인 메시지를 그대로 내려 운영자가 원인을 파악할 수 있게 합니다.
    private String errorMessage;

    // 행이 저장된 시각을 같이 내려 정렬/감사 추적 기준으로 사용합니다.
    private LocalDateTime createdAt;

    // 엔티티를 조회 API 응답 모양으로 변환하는 기능입니다.
    public static NotificationLogResponseDto from(NotificationLog log) {
        return NotificationLogResponseDto.builder()
                .id(log.getId())
                .userId(log.getUser().getUserId())
                .userName(log.getUser().getName())
                .type(log.getType())
                .targetId(log.getTargetId())
                .status(log.getStatus())
                .sentAt(log.getSentAt())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
