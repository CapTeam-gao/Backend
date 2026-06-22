package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatUnreadSummaryResponseDto {

    // 관리자가 아직 읽지 않은 전체 학생 채팅 메시지 수를 내려주는 필드입니다.
    private long totalUnreadCount;
}
