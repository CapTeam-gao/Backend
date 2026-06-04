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

    // 관리자 대시보드 통계 데이터를 조회하는 기능입니다.
    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponseDto>> getAdminDashboard(Authentication authentication) {
        return ApiResponse.ok(dashboardService.getAdminDashboard(authentication.getName()));
    }

    // 학생 대시보드에 필요한 내 팀/채팅/일지/공지 상태를 조회하는 기능입니다.
    @GetMapping("/api/user/dashboard")
    public ResponseEntity<ApiResponse<UserDashboardResponseDto>> getUserDashboard(Authentication authentication) {
        return ApiResponse.ok(dashboardService.getUserDashboard(authentication.getName()));
    }
}
