package com.capteam.gaobackend.dto.chat;

import com.capteam.gaobackend.entity.ChatChannel;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatChannelResponseDto {

    // 채널 고유 id를 내려주는 필드입니다.
    private Long id;

    // 채널이 속한 채팅방 id를 내려주는 필드입니다.
    private Long roomId;

    // 화면에 표시할 채널 이름을 내려주는 필드입니다.
    private String channelName;

    // 채널을 만든 사용자 id를 내려주는 필드입니다.
    private String createdByUserId;

    // 채널을 만든 사용자 이름을 내려주는 필드입니다.
    private String createdByName;

    // 채널 생성 시각을 내려주는 필드입니다.
    private LocalDateTime createdAt;

    // 채널에 고정된 메시지 id입니다. 고정된 메시지가 없으면 null입니다.
    private Long pinnedMessageId;

    // ChatChannel 엔티티를 프론트 응답 DTO로 변환하는 기능입니다.
    public static ChatChannelResponseDto from(ChatChannel channel) {
        return ChatChannelResponseDto.builder()
                .id(channel.getId())
                .roomId(channel.getChatRoom().getId())
                .channelName(channel.getChannelName())
                .createdByUserId(channel.getCreatedBy().getUserId())
                .createdByName(channel.getCreatedBy().getName())
                .createdAt(channel.getCreatedAt())
                .pinnedMessageId(channel.getPinnedMessageId())
                .build();
    }
}
