package com.capteam.gaobackend.dto.auth.response;

import lombok.Builder;

@Builder
// 로그인과 토큰 재발급 성공 시 JWT와 사용자 상태를 내려주는 응답 DTO입니다.
public record AuthResponse(
        // API 인증에 사용할 access token입니다.
        String accessToken,
        // access token 재발급에 사용할 refresh token입니다.
        String refreshToken,
        // 프론트 권한 분기에 사용할 사용자 권한 문자열입니다.
        String role,
        // 설문 완료 여부에 따라 첫 진입 화면을 분기하기 위한 값입니다.
        boolean surveyCompleted
) {}
