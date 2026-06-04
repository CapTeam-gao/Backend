package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.service.AiTeamMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiTeamMatchingController {

    private final AiTeamMatchingService aiTeamMatchingService;

    // 현재 생성된 팀 요약 또는 AI 서버의 팀 요약 결과를 조회하는 기능입니다.
    @GetMapping("/teams/summary")
    public ResponseEntity<ApiResponse<AiTeamSummaryResponseDto>> getTeamSummary() {
        return ApiResponse.ok(aiTeamMatchingService.getTeamSummary());
    }

    // 학생 프로필 기반 팀 자동 매칭을 실행하고 결과 요약을 반환하는 기능입니다.
    @PostMapping("/matching/run")
    public ResponseEntity<ApiResponse<AiTeamSummaryResponseDto>> runMatching() {
        return ApiResponse.ok(aiTeamMatchingService.runMatching());
    }
}
