package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatChannelSummaryResponseDto {

    // 채널 기본 정보를 내려주는 필드입니다.
    private ChatChannelResponseDto channel;

    // 채널의 마지막 메시지를 내려주는 필드입니다.
    private ChatMessageResponseDto lastMessage;

    // 현재 로그인한 사용자가 아직 읽지 않은 메시지 수입니다.
    // 본인이 보낸 메시지는 unreadCount에 넣지 않습니다.
    private long unreadCount;
}
