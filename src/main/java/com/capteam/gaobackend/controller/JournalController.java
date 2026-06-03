package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.journal.JournalCreateRequestDto;
import com.capteam.gaobackend.dto.journal.JournalDetailResponseDto;
import com.capteam.gaobackend.dto.journal.JournalResponseDto;
import com.capteam.gaobackend.dto.journal.JournalUpdateRequestDto;
import com.capteam.gaobackend.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @PostMapping
    public ResponseEntity<ApiResponse<JournalDetailResponseDto>> createJournal(
            @RequestBody @Valid JournalCreateRequestDto dto) {
        return ApiResponse.ok(journalService.createMyJournalEntry(dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JournalResponseDto>>> getMyTeamJournalList() {
        return ApiResponse.ok(journalService.getMyTeamJournalList());
    }

    @GetMapping("/{journalId}")
    public ResponseEntity<ApiResponse<JournalDetailResponseDto>> getMyTeamJournalDetail(
            @PathVariable Long journalId) {
        return ApiResponse.ok(journalService.getMyTeamJournalDetail(journalId));
    }

    @PatchMapping("/{journalId}")
    public ResponseEntity<ApiResponse<JournalDetailResponseDto>> updateJournal(
            @PathVariable Long journalId,
            @RequestBody @Valid JournalUpdateRequestDto dto) {
        return ApiResponse.ok(journalService.updateMyJournalEntry(journalId, dto));
    }
}
