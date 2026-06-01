package com.capteam.gaobackend.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // CONNECT 시점에만 인증 처리 (이후 세션에서 유지됨)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // WebSocket은 HTTP 요청처럼 매 메시지마다 Authorization 헤더가 자동으로 붙지 않습니다.
            // 그래서 STOMP CONNECT 프레임의 native header에 JWT를 직접 실어 보내고, 여기서 한 번 검증합니다.
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[WS] Missing or invalid Authorization header");
                throw new IllegalArgumentException("WebSocket 인증 토큰이 없습니다.");
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtTokenProvider.validateToken(token)) {
                    throw new IllegalArgumentException("만료되었거나 검증에 실패한 토큰입니다.");
                }

                Authentication auth = jwtTokenProvider.getAuthentication(token);

                // HTTP SecurityContext는 WebSocket 메시지마다 자동으로 이어지지 않습니다.
                // 대신 STOMP 세션의 Principal로 넣어두면 @MessageMapping 메서드에서 Principal로 꺼낼 수 있습니다.
                accessor.setUser(auth);
                log.info("[WS] CONNECT - user: {}", auth.getName());

            } catch (Exception e) {
                log.error("[WS] 토큰 검증 실패: {}", e.getMessage());
                throw new IllegalArgumentException("유효하지 않은 WebSocket 토큰입니다.");
            }
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String userId = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
            log.info("[WS] DISCONNECT - user: {}", userId);
        }

        return message;
    }
}
