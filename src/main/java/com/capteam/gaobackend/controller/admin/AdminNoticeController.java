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



    // 관리자가 새 공지를 생성하는 기능입니다.
    @PostMapping
    public ResponseEntity<ApiResponse<NoticeDetailResponseDto>> createNotice(
            @RequestBody @Valid NoticeCreateRequestDto dto) {
        return ApiResponse.ok(adminNoticeService.createNotice(dto));
    }

    // 관리자가 특정 공지 제목/내용/중요 여부를 수정하는 기능입니다.
    @PutMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponseDto>> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody NoticeUpdateRequestDto dto) {
        return ApiResponse.ok(adminNoticeService.updateNotice(noticeId, dto));
    }

    // 관리자가 특정 공지를 삭제하는 기능입니다.
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable Long noticeId) {
        adminNoticeService.deleteNotice(noticeId);
        return ApiResponse.ok("공지가 삭제되었습니다.");
    }
}
