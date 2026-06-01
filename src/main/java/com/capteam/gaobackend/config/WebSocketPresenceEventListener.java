package com.capteam.gaobackend.config;

import com.capteam.gaobackend.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceEventListener {

    private final ChatPresenceService chatPresenceService;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();

        if (user == null || accessor.getSessionId() == null) {
            return;
        }

        // CONNECT 인증이 성공하면 JwtChannelInterceptor가 Principal을 넣어둡니다.
        // 이 시점부터 사용자를 online으로 표시할 수 있습니다.
        chatPresenceService.connect(accessor.getSessionId(), user.getName());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        // DISCONNECT에서는 Principal이 없을 수도 있으므로 sessionId 기준으로 offline 처리를 합니다.
        chatPresenceService.disconnect(event.getSessionId());
    }
}
