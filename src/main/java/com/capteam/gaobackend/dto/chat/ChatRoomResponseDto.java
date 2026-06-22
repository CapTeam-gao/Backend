package com.capteam.gaobackend.dto.chat;

import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.enums.LeaderRole;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    // 로그인한 사용자의 팀 내 역할 정보를 내려주는 필드입니다.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MyChatMemberDto myMember;

    // 채팅방 안에 있는 채널 목록을 내려주는 필드입니다.
    private List<ChatChannelResponseDto> channels;

    @Getter
    @Builder
    public static class MyChatMemberDto {

        // 로그인한 팀원의 사용자 id를 내려주는 필드입니다.
        private String userId;

        // 로그인한 팀원의 이름을 내려주는 필드입니다.
        private String name;

        // 로그인한 팀원이 팀장인지 일반 팀원인지 내려주는 필드입니다.
        private LeaderRole leaderRole;

        // TeamUser 엔티티를 내 채팅방 팀원 응답 DTO로 변환하는 기능입니다.
        public static MyChatMemberDto from(TeamUser teamUser) {
            return MyChatMemberDto.builder()
                    .userId(teamUser.getUser().getUserId())
                    .name(teamUser.getUser().getName())
                    .leaderRole(teamUser.getLeaderRole())
                    .build();
        }
    }

    // ChatRoom 엔티티와 채널 목록을 프론트 응답 DTO로 변환하는 기능입니다.
    public static ChatRoomResponseDto from(ChatRoom room, List<ChatChannel> channels) {
        return from(room, channels, null);
    }

    // ChatRoom 엔티티, 채널 목록, 로그인한 팀원 정보를 프론트 응답 DTO로 변환하는 기능입니다.
    public static ChatRoomResponseDto from(ChatRoom room, List<ChatChannel> channels, TeamUser myMember) {
        return ChatRoomResponseDto.builder()
                .id(room.getId())
                .teamId(room.getTeam().getId())
                .teamName(room.getTeam().getTeamName())
                .myMember(myMember == null ? null : MyChatMemberDto.from(myMember))
                .channels(channels.stream()
                        .map(ChatChannelResponseDto::from)
                        .toList())
                .build();
    }
}
