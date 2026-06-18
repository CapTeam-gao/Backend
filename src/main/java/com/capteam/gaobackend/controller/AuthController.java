package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.auth.request.ChangePasswordRequestDto;
import com.capteam.gaobackend.dto.auth.request.LoginRequestDto;
import com.capteam.gaobackend.dto.auth.request.RefreshRequest;
import com.capteam.gaobackend.dto.auth.response.AuthResponse;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.config.JwtTokenProvider;
import com.capteam.gaobackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
    private final JwtTokenProvider jwtTokenProvider;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    @Value("${jwt.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${jwt.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    // 로그인 요청을 받아 비밀번호를 검증하고 JWT 토큰을 발급하는 기능입니다.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> doLogin(@Valid @RequestBody LoginRequestDto dto) {
        AuthResponse authResponse = authService.doLogin(dto);
        return withRefreshTokenCookie(authResponse);
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

    // HttpOnly Cookie의 refresh token으로 access token과 refresh token을 재발급하는 기능입니다.
    @PostMapping("/reissue")
    public ResponseEntity<AuthResponse> reissue(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        return withRefreshTokenCookie(authResponse);
    }

    // HttpOnly Cookie의 refresh token을 삭제하고 DB에 저장된 refresh token도 무효화하는 기능입니다.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie().toString())
                .body(new ApiResponse<>(true, "success", null));
    }

    private ResponseEntity<AuthResponse> withRefreshTokenCookie(AuthResponse authResponse) {
        ResponseCookie refreshTokenCookie = ResponseCookie.from(
                        REFRESH_TOKEN_COOKIE,
                        authResponse.refreshToken()
                )
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/auth")
                .maxAge(jwtTokenProvider.getRefreshTokenExpirationSeconds())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(authResponse);
    }

    private ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/auth")
                .maxAge(0)
                .build();
    }
}
