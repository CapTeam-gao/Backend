package com.capteam.gaobackend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardResponseDto {

    // 2학년과 3학년 팀이 모두 생성되었는지 알려주는 하위 호환 필드입니다.
    private boolean teamCreated;

    // 2학년 팀이 1개 이상 생성되었는지 알려주는 필드입니다.
    private boolean grade2TeamCreated;

    // 3학년 팀이 1개 이상 생성되었는지 알려주는 필드입니다.
    private boolean grade3TeamCreated;

    // 전체 팀 개수를 내려주는 필드입니다.
    private long totalTeamCount;

    // 2학년 팀 개수를 내려주는 필드입니다.
    private long grade2TeamCount;

    // 3학년 팀 개수를 내려주는 필드입니다.
    private long grade3TeamCount;

    // 현재 생성되어 있는 전체 팀 채팅방 수를 내려주는 필드입니다.
    private long totalChatRoomCount;

    // 기존 프론트 호환을 위해 전체 팀 채팅방 수를 내려주는 필드입니다.
    private long activeChatRoomCount;

    // 오늘 일지를 아직 완료 제출하지 않은 팀 수를 내려주는 필드입니다.
    private long journalNotSubmittedTeamCount;

    // 학생 계정 전체 수를 내려주는 필드입니다.
    private long totalStudentCount;

    // 관리자에게 읽지 않은 공지가 있는지 내려주는 필드입니다.
    private boolean hasUnreadNotice;
}
