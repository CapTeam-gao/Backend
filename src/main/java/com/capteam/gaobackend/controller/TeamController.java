package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.team.PreferredTeammateRequestDto;
import com.capteam.gaobackend.dto.team.PreferredTeammateResponseDto;
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

    @GetMapping("/preference")
    public ResponseEntity<ApiResponse<PreferredTeammateResponseDto>> getPreferences(Authentication authentication) {
        return ApiResponse.ok(teamService.getPreferences(authentication.getName()));
    }

    @PostMapping("/preference")
    public ResponseEntity<ApiResponse<PreferredTeammateResponseDto>> updatePreferences(
            @RequestBody @Valid PreferredTeammateRequestDto request,
            Authentication authentication) {
        return ApiResponse.ok(teamService.updatePreferences(authentication.getName(), request));
    }
}
