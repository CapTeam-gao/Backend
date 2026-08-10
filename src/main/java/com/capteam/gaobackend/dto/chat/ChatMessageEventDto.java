package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageEventDto {

    private String type;
    private ChatMessageResponseDto message;
    private Long messageId;
    private Long channelId;

    public static ChatMessageEventDto updated(ChatMessageResponseDto message) {
        return ChatMessageEventDto.builder()
                .type("MESSAGE_UPDATED")
                .message(message)
                .build();
    }

    public static ChatMessageEventDto deleted(Long messageId, Long channelId) {
        return ChatMessageEventDto.builder()
                .type("MESSAGE_DELETED")
                .messageId(messageId)
                .channelId(channelId)
                .build();
    }

    public static ChatMessageEventDto readStatusUpdated(Long channelId) {
        return ChatMessageEventDto.builder()
                .type("READ_STATUS_UPDATED")
                .channelId(channelId)
                .build();
    }
}
