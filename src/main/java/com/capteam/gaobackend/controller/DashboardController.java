package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.dashboard.AdminDashboardResponseDto;
import com.capteam.gaobackend.dto.dashboard.UserDashboardResponseDto;
import com.capteam.gaobackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponseDto>> getAdminDashboard(Authentication authentication) {
        return ApiResponse.ok(dashboardService.getAdminDashboard(authentication.getName()));
    }

    @GetMapping("/api/user/dashboard")
    public ResponseEntity<ApiResponse<UserDashboardResponseDto>> getUserDashboard(Authentication authentication) {
        return ApiResponse.ok(dashboardService.getUserDashboard(authentication.getName()));
    }
}
