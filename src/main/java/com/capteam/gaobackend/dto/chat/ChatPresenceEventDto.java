package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatPresenceEventDto {

    // 상태가 바뀐 사용자입니다.
    private String userId;

    private String name;

    // true면 온라인, false면 오프라인입니다.
    private boolean online;
}
