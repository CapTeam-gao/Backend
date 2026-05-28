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

    @GetMapping
    public ResponseEntity<ApiResponse<AdminJournalListResponseDto>> getJournalList() {
        return ApiResponse.ok(adminJournalService.getJournalList());
    }

    @GetMapping("/{journalId}")
    public ResponseEntity<ApiResponse<JournalDetailResponseDto>> getJournalDetail(
            @PathVariable Long journalId) {
        return ApiResponse.ok(adminJournalService.getJournalDetail(journalId));
    }
}
