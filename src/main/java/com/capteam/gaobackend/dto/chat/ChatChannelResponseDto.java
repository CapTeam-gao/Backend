package com.capteam.gaobackend.dto.chat;

import com.capteam.gaobackend.entity.ChatChannel;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatChannelResponseDto {

    private Long id;
    private Long roomId;
    private String channelName;
    private String createdByUserId;
    private String createdByName;
    private LocalDateTime createdAt;

    public static ChatChannelResponseDto from(ChatChannel channel) {
        return ChatChannelResponseDto.builder()
                .id(channel.getId())
                .roomId(channel.getChatRoom().getId())
                .channelName(channel.getChannelName())
                .createdByUserId(channel.getCreatedBy().getUserId())
                .createdByName(channel.getCreatedBy().getName())
                .createdAt(channel.getCreatedAt())
                .build();
    }
}
