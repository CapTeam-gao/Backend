package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.admin.AdminTeamDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminTeamListResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.team.TeamMemberUpdateRequestDto;
import com.capteam.gaobackend.service.TeamService;
import jakarta.validation.Valid;
import com.capteam.gaobackend.service.admin.AdminTeamService;
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


    @GetMapping
    public ResponseEntity<List<AdminTeamListResponseDto>> getTeamList() {
        var response = adminTeamService.getTeamList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<AdminTeamDetailResponseDto> getTeamDetail(@PathVariable Long teamId) {
        var response = adminTeamService.getTeamDetail(teamId);
        return ResponseEntity.ok(response);
    }

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
}
