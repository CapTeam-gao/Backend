package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.notification.response.NotificationLogResponseDto;
import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.enums.NotificationStatus;
import com.capteam.gaobackend.enums.NotificationType;
import com.capteam.gaobackend.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNotificationLogService {

    // 관리자 이력 조회 API가 실제 알림 로그 row를 검색할 저장소입니다.
    private final NotificationLogRepository notificationLogRepository;

    // 타입/상태/대상/사용자 조건으로 알림 발송 이력을 조회하는 기능입니다.
    public List<NotificationLogResponseDto> getLogs(
            NotificationType type,
            NotificationStatus status,
            String targetId,
            String userId
    ) {
        return notificationLogRepository.searchLogs(
                        type,
                        status,
                        normalizeOptional(targetId),
                        normalizeOptional(userId)
                )
                .stream()
                .map(NotificationLogResponseDto::from)
                .toList();
    }

    // 관리자 상세 보기에서 개별 로그 row를 열 때 사용하는 기능입니다.
    public NotificationLogResponseDto getLogDetail(Long logId) {
        NotificationLog log = notificationLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("알림 로그를 찾을 수 없습니다."));
        return NotificationLogResponseDto.from(log);
    }

    // 빈 쿼리 파라미터를 null로 통일해 repository 조건식을 단순하게 유지합니다.
    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
