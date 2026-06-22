package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatAdminUnreadEventDto {

    // 관리자 unread 상태가 바뀐 이벤트 종류를 내려주는 필드입니다.
    private String type;

    // unread 상태가 바뀐 채팅방 id를 내려주는 필드입니다.
    private Long roomId;

    // unread 상태가 바뀐 채널 id를 내려주는 필드입니다.
    private Long channelId;

    // 해당 채널의 관리자 unreadCount를 내려주는 필드입니다.
    private long unreadCount;

    // 전체 관리자 unreadCount를 내려주는 필드입니다.
    private long totalUnreadCount;

    // 관리자 unread 이벤트 응답 DTO를 만드는 기능입니다.
    public static ChatAdminUnreadEventDto of(
            String type,
            Long roomId,
            Long channelId,
            long unreadCount,
            long totalUnreadCount
    ) {
        return ChatAdminUnreadEventDto.builder()
                .type(type)
                .roomId(roomId)
                .channelId(channelId)
                .unreadCount(unreadCount)
                .totalUnreadCount(totalUnreadCount)
                .build();
    }
}
