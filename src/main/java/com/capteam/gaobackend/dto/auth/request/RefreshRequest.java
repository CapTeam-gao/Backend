package com.capteam.gaobackend.dto.auth.request;

// refresh token 재발급 요청 바디를 받는 DTO입니다.
public record RefreshRequest(String refreshToken) {
}
