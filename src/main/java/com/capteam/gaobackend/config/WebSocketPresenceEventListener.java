package com.capteam.gaobackend.config;

import com.capteam.gaobackend.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPresenceEventListener {

    // 채팅방 presence는 실제 채팅 메시지를 받는 구독만 기준으로 잡습니다.
    // /sub/presence/... 구독이나 단순 WebSocket CONNECT는 "채팅방 입장"으로 보지 않습니다.
    private static final Pattern CHAT_SUBSCRIPTION_PATTERN = Pattern.compile("^/sub/chat/(\\d+)$");

    private final ChatPresenceService chatPresenceService;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();

        if (user == null || accessor.getSessionId() == null) {
            log.warn("[WS CONNECT SKIP] sessionId={}, user={}", accessor.getSessionId(), user);
            return;
        }

        log.info("[WS CONNECT] sessionId={}, userId={}", accessor.getSessionId(), user.getName());

        // CONNECT 인증이 성공하면 JwtChannelInterceptor가 Principal을 넣어둡니다.
        // 여기서는 sessionId와 userId만 연결해둡니다. online 처리는 /sub/chat/{channelId} 구독 시점에 합니다.
        chatPresenceService.connect(accessor.getSessionId(), user.getName());
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        Long channelId = parseChatChannelId(accessor.getDestination());

        if (user == null || sessionId == null || subscriptionId == null || channelId == null) {
            return;
        }

        log.info(
                "[WS CHAT SUBSCRIBE] sessionId={}, subscriptionId={}, userId={}, channelId={}",
                sessionId,
                subscriptionId,
                user.getName(),
                channelId
        );

        // 사용자가 채팅 화면에서 실제 메시지 채널을 구독하면 채팅방에 들어온 것으로 봅니다.
        // 프론트가 채팅방을 나갈 때 이 구독을 unsubscribe 해야 offline으로 바뀝니다.
        chatPresenceService.enterChat(sessionId, subscriptionId, user.getName(), channelId);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getSessionId() == null || accessor.getSubscriptionId() == null) {
            log.warn(
                    "[WS UNSUBSCRIBE SKIP] sessionId={}, subscriptionId={}",
                    accessor.getSessionId(),
                    accessor.getSubscriptionId()
            );
            return;
        }

        log.info(
                "[WS CHAT UNSUBSCRIBE] sessionId={}, subscriptionId={}",
                accessor.getSessionId(),
                accessor.getSubscriptionId()
        );

        // 채팅 메시지 구독을 해제하면 해당 subscription만 presence에서 제거합니다.
        chatPresenceService.leaveChat(accessor.getSessionId(), accessor.getSubscriptionId());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.info("[WS DISCONNECT] sessionId={}", event.getSessionId());

        // DISCONNECT에서는 Principal이 없을 수도 있으므로 sessionId 기준으로 채팅방 presence와 연결 정보를 정리합니다.
        chatPresenceService.disconnect(event.getSessionId());
    }

    private Long parseChatChannelId(String destination) {
        if (destination == null) {
            return null;
        }

        Matcher matcher = CHAT_SUBSCRIPTION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }

        return Long.parseLong(matcher.group(1));
    }
}
