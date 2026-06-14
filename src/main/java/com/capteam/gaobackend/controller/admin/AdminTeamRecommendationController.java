package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.team.MatchingJobResponseDto;
import com.capteam.gaobackend.dto.team.SwapRecommendationMembersRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationDetailResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationResponseDto;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.service.MatchingJobService;
import com.capteam.gaobackend.service.admin.AdminTeamRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/team-recommendations")
@RequiredArgsConstructor
public class AdminTeamRecommendationController {

    private final AdminTeamRecommendationService adminTeamRecommendationService;
    private final MatchingJobService matchingJobService;

    // 팀 매칭 작업을 등록하고 실제 처리는 백그라운드에서 수행합니다.
    @PostMapping("/matching/run")
    public ResponseEntity<ApiResponse<MatchingJobResponseDto>> startMatching(
            @RequestBody @Valid TeamRecommendationRequestDto dto) {
        MatchingJobResponseDto job = matchingJobService.start(dto.getGrade());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(true, "팀 매칭 작업이 등록되었습니다.", job));
    }

    // 프론트에서 jobId로 작업 진행 상태와 실패 사유를 조회합니다.
    @GetMapping("/matching/jobs/{jobId}")
    public ResponseEntity<ApiResponse<MatchingJobResponseDto>> getMatchingJob(
            @PathVariable String jobId) {
        return ApiResponse.ok(matchingJobService.get(jobId));
    }

    // 실행 대기 또는 AI 호출 중인 작업을 취소합니다.
    @DeleteMapping("/matching/jobs/{jobId}")
    public ResponseEntity<ApiResponse<MatchingJobResponseDto>> cancelMatchingJob(
            @PathVariable String jobId) {
        return ApiResponse.ok(matchingJobService.cancel(jobId));
    }

    // 관리자가 특정 학년의 팀 추천안을 일괄 생성하는 기능입니다.
    @PostMapping
    public ResponseEntity<ApiResponse<List<TeamRecommendationResponseDto>>> createRecommendation(
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

    // 관리자가 학년별 팀 추천 상세 목록을 조회하는 기능입니다.
    @GetMapping("/grade/{grade}")
    public ResponseEntity<ApiResponse<List<TeamRecommendationDetailResponseDto>>> getRecommendationsByGrade(
            @PathVariable Grade grade) {
        return ApiResponse.ok(adminTeamRecommendationService.getRecommendationsByGrade(grade));
    }

    // 관리자가 두 추천안의 학생을 교환하는 기능입니다.
    @PostMapping("/swap")
    public ResponseEntity<ApiResponse<Void>> swapMembers(
            @RequestBody @Valid SwapRecommendationMembersRequestDto dto) {
        adminTeamRecommendationService.swapMembers(dto);
        return ApiResponse.ok("팀원이 교환되었습니다.");
    }

    // 관리자가 특정 학년의 전체 추천안을 일괄 수락해 팀을 생성하는 기능입니다.
    @PostMapping("/accept-all/{grade}")
    public ResponseEntity<ApiResponse<Void>> acceptAllByGrade(@PathVariable Grade grade) {
        adminTeamRecommendationService.acceptAllByGrade(grade);
        return ApiResponse.ok("모든 팀이 생성되었습니다.");
    }

}
