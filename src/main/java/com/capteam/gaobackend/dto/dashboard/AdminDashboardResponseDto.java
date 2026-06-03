package com.capteam.gaobackend.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardResponseDto {

    private boolean teamCreated;
    private long totalTeamCount;
    private long grade2TeamCount;
    private long grade3TeamCount;
    private long activeChatRoomCount;
    private long journalNotSubmittedTeamCount;
    private long totalStudentCount;
    private boolean hasUnreadNotice;
}
