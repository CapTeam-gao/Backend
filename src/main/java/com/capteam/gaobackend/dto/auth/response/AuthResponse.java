package com.capteam.gaobackend.dto.auth.response;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        boolean surveyCompleted
) {}
