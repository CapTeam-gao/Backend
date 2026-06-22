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

    // 대시보드 날짜/시간 계산을 한국 시간 기준으로 맞추기 위한 필드입니다.
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    // 캡스톤 수업 시작 시간을 저장하는 필드입니다.
    private static final LocalTime CAPSTONE_START = LocalTime.of(15, 40);

    // 캡스톤 수업 종료 시간을 저장하는 필드입니다.
    private static final LocalTime CAPSTONE_END = LocalTime.of(18, 10);

    // 팀 전체 개수와 학년별 팀 개수를 조회하는 Repository 필드입니다.
    private final TeamRepository teamRepository;

    // 팀별 채팅방 목록을 조회하는 Repository 필드입니다.
    private final ChatRoomRepository chatRoomRepository;

    // 오늘 팀별 일지 제출 여부를 조회하는 Repository 필드입니다.
    private final JournalRepository journalRepository;

    // 학생 개인의 오늘 일지 작성 여부를 조회하는 Repository 필드입니다.
    private final JournalEntryRepository journalEntryRepository;

    // 로그인 사용자가 어느 팀에 속했는지 조회하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 전체 학생 수를 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 읽지 않은 공지 여부를 확인하는 Service 필드입니다.
    private final NoticeService noticeService;

    // 팀 채팅에 접속 중인 온라인 멤버 수를 계산하는 Service 필드입니다.
    private final ChatPresenceService chatPresenceService;

    // 관리자 대시보드에 필요한 팀/채팅/일지/학생/공지 통계를 조회하는 기능입니다.
    public AdminDashboardResponseDto getAdminDashboard(String userId) {
        long totalTeamCount = teamRepository.count();
        long grade2TeamCount = teamRepository.countByGrade(Grade.GRADE_2);
        long grade3TeamCount = teamRepository.countByGrade(Grade.GRADE_3);
        boolean grade2TeamCreated = grade2TeamCount > 0;
        boolean grade3TeamCreated = grade3TeamCount > 0;
        long submittedTeamCount = journalRepository.countDistinctTeamByDate(today());
        long totalChatRoomCount = totalTeamCount > 0 ? chatRoomRepository.count() : 0;

        return AdminDashboardResponseDto.builder()
                .teamCreated(grade2TeamCreated && grade3TeamCreated)
                .grade2TeamCreated(grade2TeamCreated)
                .grade3TeamCreated(grade3TeamCreated)
                .totalTeamCount(totalTeamCount)
                .grade2TeamCount(grade2TeamCount)
                .grade3TeamCount(grade3TeamCount)
                .totalChatRoomCount(totalChatRoomCount)
                .activeChatRoomCount(totalChatRoomCount)
                .journalNotSubmittedTeamCount(totalTeamCount > 0 ? Math.max(totalTeamCount - submittedTeamCount, 0) : 0)
                .totalStudentCount(userRepository.countByAccountRole(AccountRole.STUDENT))
                .hasUnreadNotice(noticeService.hasUnreadNotice(userId))
                .build();
    }

    // 학생 대시보드에 필요한 내 팀/채팅 접속자/수업 시간/일지/공지 상태를 조회하는 기능입니다.
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

    // 대시보드 기준 날짜를 한국 시간의 오늘 날짜로 계산하는 기능입니다.
    private LocalDate today() {
        return ZonedDateTime.now(SEOUL_ZONE).toLocalDate();
    }

    // 현재 시간이 수요일 15:40~18:10 캡스톤 수업 시간인지 판단하는 기능입니다.
    private boolean isCapstoneTime() {
        ZonedDateTime now = ZonedDateTime.now(SEOUL_ZONE);
        LocalTime time = now.toLocalTime();

        return now.getDayOfWeek() == DayOfWeek.WEDNESDAY
                && !time.isBefore(CAPSTONE_START)
                && !time.isAfter(CAPSTONE_END);
    }
}