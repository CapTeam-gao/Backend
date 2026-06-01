package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatChannelPresenceResponseDto {

    // 프론트는 이 값으로 /sub/presence/teams/{teamId}를 구독하면 됩니다.
    private Long teamId;

    private List<ChatMemberPresenceResponseDto> members;
}
