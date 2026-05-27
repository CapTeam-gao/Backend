package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.team.TeamRecommendationDetailResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationResponseDto;
import com.capteam.gaobackend.service.admin.AdminTeamRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/team-recommendations")
@RequiredArgsConstructor
public class AdminTeamRecommendationController {

    private final AdminTeamRecommendationService adminTeamRecommendationService;

    // AI 팀 추천 요청
    // POST /api/admin/team-recommendations
    @PostMapping
    public ResponseEntity<ApiResponse<TeamRecommendationResponseDto>> createRecommendation(
            @RequestBody @Valid TeamRecommendationRequestDto dto) {
        return ApiResponse.ok(adminTeamRecommendationService.createRecommendation(dto));
    }

    // 추천 목록 조회
    // GET /api/admin/team-recommendations
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamRecommendationResponseDto>>> getRecommendationList() {
        return ApiResponse.ok(adminTeamRecommendationService.getRecommendationList());
    }

    // 추천 상세 조회
    // GET /api/admin/team-recommendations/{recommendationId}
    @GetMapping("/{recommendationId}")
    public ResponseEntity<ApiResponse<TeamRecommendationDetailResponseDto>> getRecommendationDetail(
            @PathVariable Long recommendationId) {
        return ApiResponse.ok(adminTeamRecommendationService.getRecommendationDetail(recommendationId));
    }

    // 추천 수락 → 팀 생성
    // POST /api/admin/team-recommendations/{recommendationId}/accept
    @PostMapping("/{recommendationId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptRecommendation(@PathVariable Long recommendationId) {
        adminTeamRecommendationService.acceptRecommendation(recommendationId);
        return ApiResponse.ok("팀이 생성되었습니다.");
    }

}
