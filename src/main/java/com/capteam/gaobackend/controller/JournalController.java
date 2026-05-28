package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.journal.JournalDetailResponseDto;
import com.capteam.gaobackend.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @GetMapping("/{journalId}")
    public ResponseEntity<ApiResponse<JournalDetailResponseDto>> getMyTeamJournalDetail(
            @PathVariable Long journalId) {
        return ApiResponse.ok(journalService.getMyTeamJournalDetail(journalId));
    }
}
