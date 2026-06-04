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

    // 관리자가 특정 학년의 AI 팀 추천안을 생성하는 기능입니다.
    @PostMapping
    public ResponseEntity<ApiResponse<TeamRecommendationResponseDto>> createRecommendation(
            @RequestBody @Valid TeamRecommendationRequestDto dto) {
        return ApiResponse.ok(adminTeamRecommendationService.createRecommendation(dto));
    }

    // 관리자가 생성된 팀 추천안 목록을 조회하는 기능입니다.
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamRecommendationResponseDto>>> getRecommendationList() {
        return ApiResponse.ok(adminTeamRecommendationService.getRecommendationList());
    }

    // 관리자가 특정 팀 추천안의 멤버와 배정 이유를 상세 조회하는 기능입니다.
    @GetMapping("/{recommendationId}")
    public ResponseEntity<ApiResponse<TeamRecommendationDetailResponseDto>> getRecommendationDetail(
            @PathVariable Long recommendationId) {
        return ApiResponse.ok(adminTeamRecommendationService.getRecommendationDetail(recommendationId));
    }

    // 관리자가 특정 추천안을 수락해 실제 팀과 팀원을 생성하는 기능입니다.
    @PostMapping("/{recommendationId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptRecommendation(@PathVariable Long recommendationId) {
        adminTeamRecommendationService.acceptRecommendation(recommendationId);
        return ApiResponse.ok("팀이 생성되었습니다.");
    }

}
