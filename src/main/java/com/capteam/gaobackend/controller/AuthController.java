package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.auth.request.ChangePasswordRequestDto;
import com.capteam.gaobackend.dto.auth.request.LoginRequestDto;
import com.capteam.gaobackend.dto.auth.request.RefreshRequest;
import com.capteam.gaobackend.dto.auth.response.AuthResponse;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // 로그인 요청을 받아 비밀번호를 검증하고 JWT 토큰을 발급하는 기능입니다.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> doLogin(@Valid @RequestBody LoginRequestDto dto) {
        AuthResponse authResponse = authService.doLogin(dto);
        return ResponseEntity.ok(authResponse);
    }

    // 현재 로그인한 사용자의 비밀번호 변경 요청을 처리하는 기능입니다.
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequestDto dto) {
        authService.changePassword(dto);
        return ApiResponse.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    // 현재 SecurityContext 기준 로그인 여부와 사용자 권한을 확인하는 기능입니다.
    @GetMapping("/me")
    public ResponseEntity<?> getAuthStatus(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            return ResponseEntity.ok(Map.of(
                    "isLoggedIn", true,
                    "username", userDetails.getUsername(),
                    "role", userDetails.getAuthorities().iterator().next().getAuthority()
            ));
        }
        return ResponseEntity.ok(Map.of("isLoggedIn", false));
    }


    // refresh token을 받아 access token과 refresh token을 재발급하는 기능입니다.
    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest dto) {
        return authService.refreshToken(dto);
    }
}
