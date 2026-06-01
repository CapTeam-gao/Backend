package com.capteam.gaobackend.dto.chat;

import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomResponseDto {

    private Long id;
    private Long teamId;
    private String teamName;
    private List<ChatChannelResponseDto> channels;

    public static ChatRoomResponseDto from(ChatRoom room, List<ChatChannel> channels) {
        return ChatRoomResponseDto.builder()
                .id(room.getId())
                .teamId(room.getTeam().getId())
                .teamName(room.getTeam().getTeamName())
                .channels(channels.stream()
                        .map(ChatChannelResponseDto::from)
                        .toList())
                .build();
    }
}
