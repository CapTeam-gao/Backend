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

    // FastAPI GET /teams/summary 결과를 조회합니다.
    @GetMapping("/teams/summary")
    public ResponseEntity<ApiResponse<AiTeamSummaryResponseDto>> getTeamSummary() {
        return ApiResponse.ok(aiTeamMatchingService.getTeamSummary());
    }

    // FastAPI POST /matching/run 결과를 반환합니다. AI 쪽 API가 완성되면 바로 사용할 수 있습니다.
    @PostMapping("/matching/run")
    public ResponseEntity<ApiResponse<AiTeamSummaryResponseDto>> runMatching() {
        return ApiResponse.ok(aiTeamMatchingService.runMatching());
    }
}
