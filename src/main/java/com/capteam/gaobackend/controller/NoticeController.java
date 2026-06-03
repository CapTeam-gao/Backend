package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.notice.NoticeDetailResponseDto;
import com.capteam.gaobackend.dto.notice.NoticeResponseDto;
import com.capteam.gaobackend.service.NoticeService;
import com.capteam.gaobackend.service.admin.AdminNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final AdminNoticeService adminNoticeService;
    private final NoticeService noticeService;

    // 공지 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeResponseDto>>> getNoticeList() {
        return ApiResponse.ok(adminNoticeService.getNoticeList());
    }

    // 공지 상세 조회
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponseDto>> getNoticeDetail(@PathVariable Long noticeId) {
        return ApiResponse.ok(adminNoticeService.getNoticeDetail(noticeId));
    }

    // 공지 상세 조회 시 현재 로그인한 유저의 읽음 상태를 기록합니다.
    @PostMapping("/{noticeId}/read")
    public ResponseEntity<ApiResponse<Void>> markNoticeAsRead(
            @PathVariable Long noticeId,
            Authentication authentication) {
        noticeService.markAsRead(noticeId, authentication.getName());
        return ApiResponse.ok("공지 읽음 처리되었습니다.");
    }
}
