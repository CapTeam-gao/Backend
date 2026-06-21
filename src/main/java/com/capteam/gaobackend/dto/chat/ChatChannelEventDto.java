package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatChannelEventDto {

    private String type;
    private ChannelPayload channel;
    private Long channelId;

    public static ChatChannelEventDto created(ChatChannelResponseDto channel) {
        return ChatChannelEventDto.builder()
                .type("CHANNEL_CREATED")
                .channel(ChannelPayload.from(channel))
                .build();
    }

    public static ChatChannelEventDto updated(ChatChannelResponseDto channel) {
        return ChatChannelEventDto.builder()
                .type("CHANNEL_UPDATED")
                .channel(ChannelPayload.from(channel))
                .build();
    }

    public static ChatChannelEventDto deleted(Long channelId) {
        return ChatChannelEventDto.builder()
                .type("CHANNEL_DELETED")
                .channelId(channelId)
                .build();
    }

    @Getter
    @Builder
    public static class ChannelPayload {

        private Long id;
        private String channelName;

        private static ChannelPayload from(ChatChannelResponseDto channel) {
            return ChannelPayload.builder()
                    .id(channel.getId())
                    .channelName(channel.getChannelName())
                    .build();
        }
    }
}
