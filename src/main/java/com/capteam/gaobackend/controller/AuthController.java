package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.auth.request.ChangePasswordRequestDto;
import com.capteam.gaobackend.dto.auth.request.LoginRequestDto;
import com.capteam.gaobackend.dto.auth.response.SessionUserDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.service.AuthService;
import jakarta.servlet.http.HttpSession;
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

    @PostMapping("/login")
    public ResponseEntity<SessionUserDto> doLogin(@RequestBody LoginRequestDto dto, HttpSession session) {
        SessionUserDto user = authService.doLogin(dto);
        session.setAttribute("LOGIN_USER", user.getUserId());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequestDto dto) {
        authService.changePassword(dto);
        return ApiResponse.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAuthStatus(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            return ResponseEntity.ok(Map.of(
                    "isLoggedIn", true,
                    "username", userDetails.getUsername()
            ));
        }
        return ResponseEntity.ok(Map.of("isLoggedIn", false));
    }
}
