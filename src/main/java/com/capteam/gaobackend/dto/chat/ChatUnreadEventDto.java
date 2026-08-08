package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatUnreadEventDto {

    // unread 상태가 바뀐 이벤트 종류입니다. 예: MESSAGE_CREATED, CHANNEL_READ
    private String type;

    // unread 상태가 바뀐 채팅방 id입니다.
    private Long roomId;

    // unread 상태가 바뀐 채널 id입니다.
    private Long channelId;

    // 해당 채널의 unreadCount입니다.
    private long unreadCount;

    // 로그인 사용자 기준 전체 unreadCount입니다.
    private long totalUnreadCount;

    public static ChatUnreadEventDto of(
            String type,
            Long roomId,
            Long channelId,
            long unreadCount,
            long totalUnreadCount
    ) {
        return ChatUnreadEventDto.builder()
                .type(type)
                .roomId(roomId)
                .channelId(channelId)
                .unreadCount(unreadCount)
                .totalUnreadCount(totalUnreadCount)
                .build();
    }
}
