package com.capteam.gaobackend.dto.notification.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenRegisterRequestDto {

    // 프론트가 로그인 직후 등록하는 실제 FCM 토큰 문자열입니다.
    // 토큰 자체가 재발급될 수 있어서 userId가 아니라 이 값을 unique 기준으로 저장합니다.
    @NotBlank
    private String fcmToken;
}
