package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.chat.ChatMessageRequestDto;
import com.capteam.gaobackend.dto.chat.ChatMessageResponseDto;
import com.capteam.gaobackend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    // WebSocket으로 들어온 채팅 메시지를 저장하고 해당 채널 구독자에게 브로드캐스트하는 기능입니다.
    @MessageMapping("/chat/{channelId}/send")
    @SendTo("/sub/chat/{channelId}")
    public ChatMessageResponseDto sendMessage(  //메시지 보내는 코드
            @DestinationVariable Long channelId,    //@PathVariable랑 같은 역할
            @Payload ChatMessageRequestDto request, //@RequestBody랑 같은 역할
            Principal principal                     //누가 보냈는지 확인    시큐리티
    ) {
        // 클라이언트가 /pub/chat/{channelId}/send 로 메시지를 보내면 이 메서드가 실행됩니다.
        // /pub prefix는 WebSocketConfig#setApplicationDestinationPrefixes에서 제거되고,
        // 남은 /chat/{channelId}/send 주소가 @MessageMapping과 매칭됩니다.

        // principal은 JwtChannelInterceptor에서 accessor.setUser(auth)로 넣어둔 인증 정보입니다.
        // 여기서는 principal.getName()이 JWT subject, 즉 userId가 됩니다.
        String senderId = principal.getName();

        // 메시지를 DB에 저장한 하고 저장 결과를 /sub/chat/{channelId} 구독자(우리 팀원들) 전체에게 broadcast(메시지 날려줌 방에다) 합니다.
        return chatService.saveMessage(channelId, senderId, request);
    }
}
