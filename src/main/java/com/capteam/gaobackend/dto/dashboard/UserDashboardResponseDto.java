package com.capteam.gaobackend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDashboardResponseDto {

    private boolean teamCreated;
    private Long teamId;
    private String teamName;
    private long teamChatActiveStudentCount;
    private boolean capstoneTime;
    private boolean todayJournalSubmitted;
    private boolean hasUnreadNotice;
}
