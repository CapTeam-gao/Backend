package com.capteam.gaobackend.dto.chat;

import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomResponseDto {

    // 채팅방 고유 id를 내려주는 필드입니다.
    private Long id;

    // 채팅방이 연결된 팀 id를 내려주는 필드입니다.
    private Long teamId;

    // 채팅방이 연결된 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 채팅방 안에 있는 채널 목록을 내려주는 필드입니다.
    private List<ChatChannelResponseDto> channels;

    // ChatRoom 엔티티와 채널 목록을 프론트 응답 DTO로 변환하는 기능입니다.
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
