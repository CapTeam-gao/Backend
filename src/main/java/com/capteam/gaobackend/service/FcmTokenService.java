package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.notification.request.FcmTokenRegisterRequestDto;
import com.capteam.gaobackend.dto.notification.response.FcmTokenResponseDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserFcmToken;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.UserFcmTokenRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmTokenService {

    // 서버가 여러 시간대에서 실행되더라도 등록 시각을 일관되게 남기기 위한 기준 시간대입니다.
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    // 토큰 등록 시 인증 사용자 소유권을 검증하기 위해 사용자 저장소가 필요합니다.
    private final UserRepository userRepository;

    // 토큰 unique 재등록과 로그아웃 삭제를 처리하기 위해 토큰 저장소를 사용합니다.
    private final UserFcmTokenRepository userFcmTokenRepository;

    // 로그인 직후 전달받은 FCM 토큰을 사용자 계정에 연결하는 기능입니다.
    // 이미 존재하는 토큰이면 중복 row를 만들지 않고 현재 로그인 사용자에게 다시 귀속시킵니다.
    @Transactional
    public FcmTokenResponseDto registerToken(FcmTokenRegisterRequestDto request) {
        // 인증 문맥의 사용자와 토큰 row의 소유권을 연결해야 이후 공지/채팅 푸시 대상을 정확히 찾을 수 있습니다.
        User user = getAuthenticatedUser();
        // 공백 차이 때문에 동일 토큰이 다른 값으로 저장되는 일을 막기 위해 먼저 정규화합니다.
        String normalizedFcmToken = normalizeRequiredToken(request.getFcmToken());
        // 기존 테이블의 last_registered_at 호환 컬럼과 updatedAt을 모두 현재 시각으로 맞추기 위해 사용합니다.
        LocalDateTime registeredAt = LocalDateTime.now(SEOUL_ZONE);

        UserFcmToken token = userFcmTokenRepository.findByToken(normalizedFcmToken)
                .map(existing -> {
                    existing.reassign(user, registeredAt);
                    return existing;
                })
                .orElseGet(() -> userFcmTokenRepository.save(UserFcmToken.builder()
                        .user(user)
                        .token(normalizedFcmToken)
                        .lastRegisteredAt(registeredAt)
                        .build()));

        return FcmTokenResponseDto.from(token);
    }

    // 로그아웃 시 현재 사용자에게 연결된 FCM 토큰을 모두 제거하는 기능입니다.
    // 설계서의 DELETE /api/user/fcm-token는 body 없이 호출되므로 사용자 기준 일괄 삭제로 맞춥니다.
    @Transactional
    public void deleteMyTokens() {
        User user = getAuthenticatedUser();
        userFcmTokenRepository.deleteAllByUserUserId(user.getUserId());
    }

    // SecurityContext에서 현재 로그인 사용자를 읽어 토큰 등록/삭제 요청이 본인 계정에만 적용되게 합니다.
    private User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    // 빈 문자열 토큰이 저장되면 푸시 발송 시점까지 오류가 늦게 드러나므로 등록 단계에서 바로 차단합니다.
    private String normalizeRequiredToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("FCM 토큰은 필수입니다.");
        }
        return token.trim();
    }
}
