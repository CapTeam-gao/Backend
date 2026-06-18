package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.journal.JournalCreateRequestDto;
import com.capteam.gaobackend.dto.journal.JournalDetailResponseDto;
import com.capteam.gaobackend.dto.journal.JournalResponseDto;
import com.capteam.gaobackend.dto.journal.JournalTodayResponseDto;
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

    // 로그인한 학생이 오늘 내 팀 일지를 생성하고 개인 제출 내용을 저장하는 기능입니다.
    @PostMapping
    public ResponseEntity<ApiResponse<JournalDetailResponseDto>> createJournal(
            @RequestBody @Valid JournalCreateRequestDto dto) {
        return ApiResponse.ok(journalService.createMyJournalEntry(dto));
    }

    // 로그인한 학생이 속한 팀의 일지 목록을 조회하는 기능입니다.
    @GetMapping
    public ResponseEntity<ApiResponse<List<JournalResponseDto>>> getMyTeamJournalList() {
        return ApiResponse.ok(journalService.getMyTeamJournalList());
    }

    // 로그인한 학생이 오늘 내 팀 일지와 본인 제출 상태를 조회하는 기능입니다.
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<JournalTodayResponseDto>> getTodayJournal() {
        return ApiResponse.ok(journalService.getTodayJournal());
    }

    // 로그인한 학생이 속한 팀의 특정 일지 상세를 조회하는 기능입니다.
    @GetMapping("/{journalId}")
    public ResponseEntity<ApiResponse<JournalDetailResponseDto>> getMyTeamJournalDetail(
            @PathVariable Long journalId) {
        return ApiResponse.ok(journalService.getMyTeamJournalDetail(journalId));
    }

    // 로그인한 학생이 본인이 제출한 특정 일지 내용을 수정하는 기능입니다.
    @PatchMapping("/{journalId}")
    public ResponseEntity<ApiResponse<JournalDetailResponseDto>> updateJournal(
            @PathVariable Long journalId,
            @RequestBody @Valid JournalUpdateRequestDto dto) {
        return ApiResponse.ok(journalService.updateMyJournalEntry(journalId, dto));
    }
}
