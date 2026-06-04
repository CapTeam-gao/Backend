package com.capteam.gaobackend.dto.chat;

import com.capteam.gaobackend.entity.TeamUser;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMemberPresenceResponseDto {

    // 온라인 상태를 표시할 팀원 userId를 내려주는 필드입니다.
    private String userId;

    // 온라인 상태를 표시할 팀원 이름을 내려주는 필드입니다.
    private String name;

    // true면 온라인, false면 오프라인 상태를 의미하는 필드입니다.
    private boolean online;

    // TeamUser 엔티티와 온라인 여부를 화면 응답 DTO로 변환하는 기능입니다.
    public static ChatMemberPresenceResponseDto of(TeamUser teamUser, boolean online) {
        return ChatMemberPresenceResponseDto.builder()
                .userId(teamUser.getUser().getUserId())
                .name(teamUser.getUser().getName())
                .online(online)
                .build();
    }
}
