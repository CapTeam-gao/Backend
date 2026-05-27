package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.admin.AdminTeamDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminTeamListResponseDto;
import com.capteam.gaobackend.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teams")
@RequiredArgsConstructor
public class AdminTeamController {


    private final AdminService adminService;


    @GetMapping
    public ResponseEntity<List<AdminTeamListResponseDto>> getTeamList() {
        var response = adminService.getTeamList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<AdminTeamDetailResponseDto> getTeamDetail(@PathVariable Long teamId) {
        var response = adminService.getTeamDetail(teamId);
        return ResponseEntity.ok(response);
    }
}
