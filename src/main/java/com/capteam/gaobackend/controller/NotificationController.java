package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.notification.request.FcmTokenRegisterRequestDto;
import com.capteam.gaobackend.dto.notification.response.FcmTokenResponseDto;
import com.capteam.gaobackend.service.FcmTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class NotificationController {

    // 로그인 직후와 로그아웃 시점에 토큰 소유권을 갱신/삭제하는 공통 FCM 인프라 서비스입니다.
    private final FcmTokenService fcmTokenService;

    // 프론트가 로그인 직후 현재 세션의 FCM 토큰을 등록하는 기능입니다.
    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<FcmTokenResponseDto>> registerFcmToken(
            @RequestBody @Valid FcmTokenRegisterRequestDto request
    ) {
        return ApiResponse.ok(fcmTokenService.registerToken(request));
    }

    // 프론트가 로그아웃 직전에 현재 사용자에게 연결된 FCM 토큰을 제거하는 기능입니다.
    @DeleteMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> deleteMyFcmTokens() {
        fcmTokenService.deleteMyTokens();
        return ApiResponse.ok("FCM 토큰이 삭제되었습니다.");
    }
}
