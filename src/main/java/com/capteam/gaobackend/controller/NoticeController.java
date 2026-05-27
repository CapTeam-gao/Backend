package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.notice.NoticeDetailResponseDto;
import com.capteam.gaobackend.dto.notice.NoticeResponseDto;
import com.capteam.gaobackend.service.admin.AdminNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final AdminNoticeService adminNoticeService;

    // 공지 목록 조회
    // GET /api/admin/notices
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeResponseDto>>> getNoticeList() {
        return ApiResponse.ok(adminNoticeService.getNoticeList());
    }

    // 공지 상세 조회
    // GET /api/admin/notices/{noticeId}
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponseDto>> getNoticeDetail(@PathVariable Long noticeId) {
        return ApiResponse.ok(adminNoticeService.getNoticeDetail(noticeId));
    }
}
