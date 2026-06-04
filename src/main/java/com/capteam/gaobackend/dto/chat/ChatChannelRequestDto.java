package com.capteam.gaobackend.dto.chat;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatChannelRequestDto {
    // 생성하거나 수정할 채널 이름을 받는 필드입니다.
    private String channelName;
}
