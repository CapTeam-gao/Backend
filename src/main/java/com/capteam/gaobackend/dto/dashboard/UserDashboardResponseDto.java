package com.capteam.gaobackend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDashboardResponseDto {

    // 로그인한 학생에게 팀이 배정되었는지 알려주는 필드입니다.
    private boolean teamCreated;

    // 로그인한 학생의 팀 id를 내려주는 필드입니다.
    private Long teamId;

    // 로그인한 학생의 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 내 팀 채팅에 현재 온라인인 학생 수를 내려주는 필드입니다.
    private long teamChatActiveStudentCount;

    // 현재 시간이 캡스톤 수업 시간인지 알려주는 필드입니다.
    private boolean capstoneTime;

    // 로그인한 학생이 오늘 일지를 제출했는지 알려주는 필드입니다.
    private boolean todayJournalSubmitted;

    // 로그인한 학생에게 읽지 않은 공지가 있는지 알려주는 필드입니다.
    private boolean hasUnreadNotice;
}
