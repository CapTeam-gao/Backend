package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 관리자 채팅 목록에서 여러 방의 채널 요약을 한 번에 내려주는 응답입니다.
 */
@Getter
@Builder
public class AdminChatRoomSummaryResponseDto {

    private Long roomId;

    private List<ChatChannelSummaryResponseDto> channelSummaries;
}
