package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.admin.AdminTeamDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminTeamListResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.team.ManualTeamRecommendationRequestDto;
import com.capteam.gaobackend.dto.team.TeamMemberUpdateRequestDto;
import com.capteam.gaobackend.service.TeamService;
import jakarta.validation.Valid;
import com.capteam.gaobackend.service.admin.AdminTeamService;
import com.capteam.gaobackend.service.admin.ManualTeamRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teams")
@RequiredArgsConstructor
public class AdminTeamController {


    private final AdminTeamService adminTeamService;
    private final TeamService teamService;
    private final ManualTeamRecommendationService manualTeamRecommendationService;


    // 관리자가 전체 팀 목록과 팀별 멤버 요약을 조회하는 기능입니다.
    @GetMapping
    public ResponseEntity<List<AdminTeamListResponseDto>> getTeamList() {
        var response = adminTeamService.getTeamList();
        return ResponseEntity.ok(response);
    }

    // 관리자가 특정 팀의 상세 정보와 팀원 목록을 조회하는 기능입니다.
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<AdminTeamDetailResponseDto>> getTeamDetail(@PathVariable Long teamId) {
        return ApiResponse.ok(adminTeamService.getTeamDetail(teamId));
    }

    // 관리자가 특정 팀의 팀원 역할과 팀장 여부를 수정하는 기능입니다.
    @PatchMapping("/{teamId}/members")
    public ResponseEntity<ApiResponse<Void>> updateTeamMember(
            @PathVariable Long teamId,
            @RequestBody @Valid TeamMemberUpdateRequestDto request) {
        if (!teamId.equals(request.getTargetTeamId())) {
            throw new IllegalArgumentException("경로의 teamId와 targetTeamId가 일치하지 않습니다.");
        }

        teamService.updateTeamMember(request);
        return ApiResponse.ok("팀원이 수정되었습니다.");
    }

    // 프론트 직접 구성 화면의 /api/admin/teams/manual 호출을 추천안 저장 흐름에 연결합니다.
    @PostMapping("/manual")
    public ResponseEntity<ApiResponse<Void>> createManualTeams(
            @RequestBody ManualTeamRecommendationRequestDto request) {
        manualTeamRecommendationService.createAndAcceptManualTeams(request);
        return ApiResponse.ok("팀이 생성되었습니다.");
    }
}
