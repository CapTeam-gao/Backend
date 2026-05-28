package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.admin.AdminTeamDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminTeamListResponseDto;
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
}
