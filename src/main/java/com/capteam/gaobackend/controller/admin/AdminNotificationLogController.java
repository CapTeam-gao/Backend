package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.notification.response.NotificationLogResponseDto;
import com.capteam.gaobackend.enums.NotificationStatus;
import com.capteam.gaobackend.enums.NotificationType;
import com.capteam.gaobackend.service.AdminNotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notification-logs")
public class AdminNotificationLogController {

    // 관리자 화면에서 발송 이력 목록/상세를 조회할 때 사용하는 서비스입니다.
    private final AdminNotificationLogService adminNotificationLogService;

    // 타입, 상태, 대상 식별자, 사용자 ID 조건으로 알림 로그 목록을 조회하는 기능입니다.
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationLogResponseDto>>> getNotificationLogs(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String userId
    ) {
        return ApiResponse.ok(adminNotificationLogService.getLogs(type, status, targetId, userId));
    }

    // 관리자 화면에서 특정 로그 한 건의 상세 정보를 조회하는 기능입니다.
    @GetMapping("/{logId}")
    public ResponseEntity<ApiResponse<NotificationLogResponseDto>> getNotificationLogDetail(@PathVariable Long logId) {
        return ApiResponse.ok(adminNotificationLogService.getLogDetail(logId));
    }
}
