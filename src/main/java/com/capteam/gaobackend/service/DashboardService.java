package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.dashboard.AdminDashboardResponseDto;
import com.capteam.gaobackend.dto.dashboard.UserDashboardResponseDto;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import com.capteam.gaobackend.repository.JournalEntryRepository;
import com.capteam.gaobackend.repository.JournalRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime CAPSTONE_START = LocalTime.of(15, 40);
    private static final LocalTime CAPSTONE_END = LocalTime.of(18, 10);

    private final TeamRepository teamRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final JournalRepository journalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TeamUserRepository teamUserRepository;
    private final UserRepository userRepository;
    private final NoticeService noticeService;
    private final ChatPresenceService chatPresenceService;

    public AdminDashboardResponseDto getAdminDashboard(String userId) {
        long totalTeamCount = teamRepository.count();
        long submittedTeamCount = journalRepository.countDistinctTeamByDate(today());

        return AdminDashboardResponseDto.builder()
                .teamCreated(totalTeamCount > 0)
                .totalTeamCount(totalTeamCount)
                .grade2TeamCount(teamRepository.countByGrade(Grade.GRADE_2))
                .grade3TeamCount(teamRepository.countByGrade(Grade.GRADE_3))
                .activeChatRoomCount(totalTeamCount > 0 ? chatRoomRepository.count() : 0)
                .journalNotSubmittedTeamCount(totalTeamCount > 0 ? Math.max(totalTeamCount - submittedTeamCount, 0) : 0)
                .totalStudentCount(userRepository.countByAccountRole(AccountRole.STUDENT))
                .hasUnreadNotice(noticeService.hasUnreadNotice(userId))
                .build();
    }

    public UserDashboardResponseDto getUserDashboard(String userId) {
        boolean capstoneTime = isCapstoneTime();
        TeamUser teamUser = teamUserRepository.findByUserUserId(userId)
                .orElse(null);

        if (teamUser == null) {
            return UserDashboardResponseDto.builder()
                    .teamCreated(false)
                    .teamChatActiveStudentCount(0)
                    .capstoneTime(capstoneTime)
                    .todayJournalSubmitted(false)
                    .hasUnreadNotice(noticeService.hasUnreadNotice(userId))
                    .build();
        }

        Long teamId = teamUser.getTeam().getId();

        return UserDashboardResponseDto.builder()
                .teamCreated(true)
                .teamId(teamId)
                .teamName(teamUser.getTeam().getTeamName())
                .teamChatActiveStudentCount(chatPresenceService.countOnlineMembersByTeamId(teamId))
                .capstoneTime(capstoneTime)
                .todayJournalSubmitted(journalEntryRepository.existsByJournalTeamIdAndJournalDateAndWriterUserId(
                        teamId,
                        today(),
                        userId
                ))
                .hasUnreadNotice(noticeService.hasUnreadNotice(userId))
                .build();
    }

    private LocalDate today() {
        return ZonedDateTime.now(SEOUL_ZONE).toLocalDate();
    }

    private boolean isCapstoneTime() {
        ZonedDateTime now = ZonedDateTime.now(SEOUL_ZONE);
        LocalTime time = now.toLocalTime();

        return now.getDayOfWeek() == DayOfWeek.WEDNESDAY
                && !time.isBefore(CAPSTONE_START)
                && !time.isAfter(CAPSTONE_END);
    }
}
