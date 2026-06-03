package com.capteam.gaobackend.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminChatRoomCreateRequestDto {

    private Long teamId;
    private String channelName;
}
