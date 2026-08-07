package com.capteam.gaobackend.dto.notification.response;

import com.capteam.gaobackend.entity.UserFcmToken;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FcmTokenResponseDto {

    // 토큰 재등록과 삭제 검증 시 프론트나 테스트에서 row를 식별할 수 있게 둔 PK입니다.
    private Long tokenId;

    // 로그에 원문 토큰이 남지 않도록 응답에는 일부만 마스킹해서 내려줍니다.
    private String tokenPreview;

    // BaseTimeEntity의 updatedAt은 마지막 등록 시점으로도 읽히므로 디버깅 확인값으로 내려줍니다.
    private LocalDateTime updatedAt;

    public static FcmTokenResponseDto from(UserFcmToken token) {
        return FcmTokenResponseDto.builder()
                .tokenId(token.getId())
                .tokenPreview(maskToken(token.getToken()))
                .updatedAt(token.getUpdatedAt())
                .build();
    }

    private static String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return token;
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
