package com.capteam.gaobackend.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminChatRoomCreateRequestDto {

    // 채팅방을 만들 대상 팀 id를 받는 필드입니다.
    private Long teamId;

    // 채팅방 생성 시 함께 만들 기본 채널 이름을 받는 필드입니다.
    private String channelName;
}
