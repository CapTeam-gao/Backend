package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.team.MyTeamResponseDto;
import com.capteam.gaobackend.dto.team.PreferredTeammateRequestDto;
import com.capteam.gaobackend.dto.team.PreferredTeammateResponseDto;
import com.capteam.gaobackend.dto.team.TeamDetailResponseDto;
import com.capteam.gaobackend.dto.team.TeamProjectRequestDto;
import com.capteam.gaobackend.dto.team.TeamSummaryResponseDto;
import com.capteam.gaobackend.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    // 로그인한 사용자의 선호 팀원 목록을 조회하는 기능입니다.
    @GetMapping("/preference")
    public ResponseEntity<ApiResponse<PreferredTeammateResponseDto>> getPreferences(Authentication authentication) {
        return ApiResponse.ok(teamService.getPreferences(authentication.getName()));
    }

    // 로그인한 사용자의 선호 팀원 목록을 최대 3명까지 저장하는 기능입니다.
    @PostMapping("/preference")
    public ResponseEntity<ApiResponse<PreferredTeammateResponseDto>> updatePreferences(
            @RequestBody @Valid PreferredTeammateRequestDto request,
            Authentication authentication) {
        return ApiResponse.ok(teamService.updatePreferences(authentication.getName(), request));
    }

    // 로그인한 학생의 소속 팀 상세 정보를 조회하는 기능입니다.
    @GetMapping("/my-team")
    public ResponseEntity<ApiResponse<MyTeamResponseDto>> getMyTeam(Authentication authentication) {
        return ApiResponse.ok(teamService.getMyTeam(authentication.getName()));
    }

    // 로그인한 학생의 소속 팀 요약 정보를 조회하는 기능입니다.
    @GetMapping("/my-team/summary")
    public ResponseEntity<ApiResponse<TeamSummaryResponseDto>> getMyTeamSummary(Authentication authentication) {
        return ApiResponse.ok(teamService.getMyTeamSummary(authentication.getName()));
    }

    // 로그인한 학생이 소속된 특정 팀 상세 정보를 조회하는 기능입니다.
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponseDto>> getTeamDetail(
            @PathVariable Long teamId,
            Authentication authentication) {
        return ApiResponse.ok(teamService.getTeamDetail(authentication.getName(), teamId));
    }

    // 로그인한 학생이 소속된 팀의 프로젝트 기획서를 조회하는 기능입니다.
    @GetMapping("/project")
    public ResponseEntity<ApiResponse<MyTeamResponseDto.TeamProjectDto>> getMyTeamProject(Authentication authentication) {
        return ApiResponse.ok(teamService.getMyTeamProject(authentication.getName()));
    }

    // 로그인한 학생이 소속된 팀의 프로젝트 기획서를 생성하거나 수정하는 기능입니다.
    @PostMapping("/project")
    public ResponseEntity<ApiResponse<MyTeamResponseDto.TeamProjectDto>> createMyTeamProject(
            @RequestBody @Valid TeamProjectRequestDto request,
            Authentication authentication) {
        return ApiResponse.ok(teamService.upsertMyTeamProject(authentication.getName(), request));
    }

    // 로그인한 학생이 소속된 팀의 프로젝트 기획서를 생성하거나 수정하는 기능입니다.
    @PutMapping("/project")
    public ResponseEntity<ApiResponse<MyTeamResponseDto.TeamProjectDto>> upsertMyTeamProject(
            @RequestBody @Valid TeamProjectRequestDto request,
            Authentication authentication) {
        return ApiResponse.ok(teamService.upsertMyTeamProject(authentication.getName(), request));
    }
}
