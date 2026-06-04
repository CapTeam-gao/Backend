package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.journal.JournalDetailResponseDto;
import com.capteam.gaobackend.dto.journal.JournalCreateRequestDto;
import com.capteam.gaobackend.dto.journal.JournalResponseDto;
import com.capteam.gaobackend.dto.journal.JournalUpdateRequestDto;
import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.entity.JournalEntry;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.JournalEntryRepository;
import com.capteam.gaobackend.repository.JournalRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalService {

    // 일지 작성 날짜를 한국 시간 기준으로 맞추기 위한 필드입니다.
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    // 팀별 일지 헤더를 조회하거나 생성하는 Repository 필드입니다.
    private final JournalRepository journalRepository;

    // 학생 개인의 일지 제출 내용을 조회/저장/수정하는 Repository 필드입니다.
    private final JournalEntryRepository journalEntryRepository;

    // 로그인 사용자의 팀 소속과 팀원 목록을 조회하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 현재 로그인한 사용자를 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 오늘 날짜의 내 팀 일지를 만들고 내 제출 내용을 저장하는 기능입니다.
    @Transactional
    public JournalDetailResponseDto createMyJournalEntry(JournalCreateRequestDto dto) {
        User user = getAuthenticatedUser();
        TeamUser myTeamUser = getMyTeamUser(user);
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        Journal journal = journalRepository.findByTeamIdAndDate(myTeamUser.getTeam().getId(), today)
                .orElseGet(() -> journalRepository.save(Journal.builder()
                        .team(myTeamUser.getTeam())
                        .title(today + " 캡스톤 일지")
                        .date(today)
                        .build()));

        if (journalEntryRepository.findByJournalIdAndWriterUserId(journal.getId(), user.getUserId()).isPresent()) {
            throw new IllegalArgumentException("오늘 일지를 이미 제출했습니다. 수정 API를 사용해주세요.");
        }

        journalEntryRepository.save(JournalEntry.builder()
                .journal(journal)
                .writer(user)
                .todayActivityContent(dto.getTodayActivityContent())
                .activityContent(dto.getActivityContent())
                .nextPlanContent(dto.getNextPlanContent())
                .reflectionContent(dto.getReflectionContent())
                .build());

        completeIfAllMembersSubmitted(journal);
        return getJournalDetail(journal, user);
    }

    // 내가 속한 팀의 일지 목록을 최신 날짜순으로 조회하는 기능입니다.
    public List<JournalResponseDto> getMyTeamJournalList() {
        User user = getAuthenticatedUser();
        TeamUser myTeamUser = getMyTeamUser(user);

        return journalRepository.findByTeamIdOrderByDateDesc(myTeamUser.getTeam().getId())
                .stream()
                .map(JournalResponseDto::from)
                .toList();
    }

    // 내가 제출한 특정 일지 내용을 수정하는 기능입니다.
    @Transactional
    public JournalDetailResponseDto updateMyJournalEntry(Long journalId, JournalUpdateRequestDto dto) {
        User user = getAuthenticatedUser();
        TeamUser myTeamUser = getMyTeamUser(user);
        Journal journal = getJournal(journalId);

        if (!journal.getTeam().getId().equals(myTeamUser.getTeam().getId())) {
            throw new RuntimeException("다른 팀의 일지는 수정할 수 없습니다.");
        }

        JournalEntry entry = journalEntryRepository.findByJournalIdAndWriterUserId(journalId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("수정할 내 일지 제출 내역이 없습니다."));
        entry.update(
                dto.getTodayActivityContent(),
                dto.getActivityContent(),
                dto.getNextPlanContent(),
                dto.getReflectionContent()
        );

        completeIfAllMembersSubmitted(journal);
        return getJournalDetail(journal, user);
    }

    // 내가 속한 팀의 특정 일지 상세를 조회하는 기능입니다.
    public JournalDetailResponseDto getMyTeamJournalDetail(Long journalId) {
        User user = getAuthenticatedUser();

        TeamUser myTeamUser = getMyTeamUser(user);

        Journal journal = getJournal(journalId);

        if (!journal.getTeam().getId().equals(myTeamUser.getTeam().getId())) {
            throw new RuntimeException("다른 팀의 일지는 조회할 수 없습니다.");
        }

        return getJournalDetail(journal, user);
    }

    // 관리자가 팀 제한 없이 특정 일지 상세를 조회하는 기능입니다.
    public JournalDetailResponseDto getAdminJournalDetail(Long journalId) {
        Journal journal = getJournal(journalId);
        return getJournalDetail(journal, null);
    }

    // journalId로 일지를 조회하고 없으면 예외를 발생시키는 기능입니다.
    private Journal getJournal(Long journalId) {
        return journalRepository.findById(journalId)
                .orElseThrow(() -> new RuntimeException("일지를 찾을 수 없습니다."));
    }

    // 일지 기본 정보, 팀원 목록, 제출 목록을 묶어 상세 응답 DTO로 만드는 기능입니다.
    private JournalDetailResponseDto getJournalDetail(Journal journal, User writer) {
        Long teamId = journal.getTeam().getId();

        List<JournalEntry> entries = journalEntryRepository.findByJournalId(journal.getId())
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getWriter().getName()))
                .toList();

        List<User> teamMembers = teamUserRepository.findByTeamId(teamId)
                .stream()
                .map(TeamUser::getUser)
                .sorted(Comparator.comparing(User::getName))
                .toList();

        return JournalDetailResponseDto.from(journal, writer, teamMembers, entries);
    }

    // SecurityContext에서 현재 로그인한 사용자를 조회하는 기능입니다.
    private User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    // 사용자가 속한 팀원 정보를 조회하고 팀이 없으면 예외를 발생시키는 기능입니다.
    private TeamUser getMyTeamUser(User user) {
        return teamUserRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("소속된 팀을 찾을 수 없습니다."));
    }

    // 팀원 전원이 일지를 제출했으면 일지 상태를 완료로 변경하는 기능입니다.
    private void completeIfAllMembersSubmitted(Journal journal) {
        long memberCount = teamUserRepository.findByTeamId(journal.getTeam().getId()).size();
        long submittedCount = journalEntryRepository.countByJournalId(journal.getId());

        if (memberCount > 0 && submittedCount >= memberCount) {
            journal.complete();
        }
    }
}
