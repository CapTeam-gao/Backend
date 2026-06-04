package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatChannelPresenceResponseDto {

    // 프론트는 이 값으로 /sub/presence/teams/{teamId}를 구독하면 됩니다.
    private Long teamId;

    // 해당 팀 채팅방의 팀원별 온라인 상태 목록을 내려주는 필드입니다.
    private List<ChatMemberPresenceResponseDto> members;
}
