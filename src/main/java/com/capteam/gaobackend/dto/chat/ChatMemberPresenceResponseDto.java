package com.capteam.gaobackend.dto.chat;

import com.capteam.gaobackend.entity.TeamUser;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMemberPresenceResponseDto {

    private String userId;
    private String name;
    private boolean online;

    public static ChatMemberPresenceResponseDto of(TeamUser teamUser, boolean online) {
        return ChatMemberPresenceResponseDto.builder()
                .userId(teamUser.getUser().getUserId())
                .name(teamUser.getUser().getName())
                .online(online)
                .build();
    }
}
