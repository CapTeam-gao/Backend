package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.notice.*;
import com.capteam.gaobackend.service.admin.AdminNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

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

    // 공지 생성
    // POST /api/admin/notices
    @PostMapping
    public ResponseEntity<ApiResponse<NoticeDetailResponseDto>> createNotice(
            @RequestBody @Valid NoticeCreateRequestDto dto) {
        return ApiResponse.ok(adminNoticeService.createNotice(dto));
    }

    // 공지 수정
    // PUT /api/admin/notices/{noticeId}
    @PutMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponseDto>> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody NoticeUpdateRequestDto dto) {
        return ApiResponse.ok(adminNoticeService.updateNotice(noticeId, dto));
    }

    // 공지 삭제
    // DELETE /api/admin/notices/{noticeId}
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable Long noticeId) {
        adminNoticeService.deleteNotice(noticeId);
        return ApiResponse.ok("공지가 삭제되었습니다.");
    }
}
