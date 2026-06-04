package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.admin.AdminJournalListResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.journal.JournalDetailResponseDto;
import com.capteam.gaobackend.service.admin.AdminJournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/journals")
@RequiredArgsConstructor
public class AdminJournalController {

    private final AdminJournalService adminJournalService;

    // 관리자가 전체 팀 일지 목록과 제출 통계를 조회하는 기능입니다.
    @GetMapping
    public ResponseEntity<ApiResponse<AdminJournalListResponseDto>> getJournalList() {
        return ApiResponse.ok(adminJournalService.getJournalList());
    }

    // 관리자가 특정 일지 상세 내용을 조회하는 기능입니다.
    @GetMapping("/{journalId}")
    public ResponseEntity<ApiResponse<JournalDetailResponseDto>> getJournalDetail(
            @PathVariable Long journalId) {
        return ApiResponse.ok(adminJournalService.getJournalDetail(journalId));
    }
}
