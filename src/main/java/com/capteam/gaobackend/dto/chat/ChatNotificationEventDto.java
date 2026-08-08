package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatNotificationEventDto {

    // 토스트를 클릭했을 때 이동할 채널 id입니다.
    private Long channelId;

    // 토스트에 표시할 채널 이름입니다.
    private String channelName;

    // 토스트에 표시할 팀 이름입니다.
    private String teamName;

    // 새 메시지를 보낸 사람 이름입니다.
    private String senderName;

    // 토스트 본문에 표시할 메시지 미리보기입니다.
    private String messagePreview;

    private LocalDateTime createdAt;

    public static ChatNotificationEventDto of(
            Long channelId,
            String channelName,
            String teamName,
            String senderName,
            String messagePreview,
            LocalDateTime createdAt
    ) {
        return ChatNotificationEventDto.builder()
                .channelId(channelId)
                .channelName(channelName)
                .teamName(teamName)
                .senderName(senderName)
                .messagePreview(messagePreview)
                .createdAt(createdAt)
                .build();
    }
}
