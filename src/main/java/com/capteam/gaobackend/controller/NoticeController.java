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

    // 학생/관리자가 볼 수 있는 공지 목록을 조회하는 기능입니다.
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeResponseDto>>> getNoticeList() {
        return ApiResponse.ok(adminNoticeService.getNoticeList());
    }

    // 특정 공지 상세 내용을 조회하고 현재 로그인한 사용자 기준으로 읽음 처리하는 기능입니다.
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponseDto>> getNoticeDetail(
            @PathVariable Long noticeId,
            Authentication authentication) {
        noticeService.markAsRead(noticeId, authentication.getName());
        return ApiResponse.ok(adminNoticeService.getNoticeDetail(noticeId));
    }

    // 특정 공지를 현재 로그인한 사용자 기준으로 읽음 처리하는 기능입니다.
    @PostMapping("/{noticeId}/read")
    public ResponseEntity<ApiResponse<Void>> markNoticeAsRead(
            @PathVariable Long noticeId,
            Authentication authentication) {
        noticeService.markAsRead(noticeId, authentication.getName());
        return ApiResponse.ok("공지 읽음 처리되었습니다.");
    }
}
