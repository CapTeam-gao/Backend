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

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final JournalRepository journalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TeamUserRepository teamUserRepository;
    private final UserRepository userRepository;

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

    public List<JournalResponseDto> getMyTeamJournalList() {
        User user = getAuthenticatedUser();
        TeamUser myTeamUser = getMyTeamUser(user);

        return journalRepository.findByTeamIdOrderByDateDesc(myTeamUser.getTeam().getId())
                .stream()
                .map(JournalResponseDto::from)
                .toList();
    }

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

    public JournalDetailResponseDto getMyTeamJournalDetail(Long journalId) {
        User user = getAuthenticatedUser();

        TeamUser myTeamUser = getMyTeamUser(user);

        Journal journal = getJournal(journalId);

        if (!journal.getTeam().getId().equals(myTeamUser.getTeam().getId())) {
            throw new RuntimeException("다른 팀의 일지는 조회할 수 없습니다.");
        }

        return getJournalDetail(journal, user);
    }

    public JournalDetailResponseDto getAdminJournalDetail(Long journalId) {
        Journal journal = getJournal(journalId);
        return getJournalDetail(journal, null);
    }

    private Journal getJournal(Long journalId) {
        return journalRepository.findById(journalId)
                .orElseThrow(() -> new RuntimeException("일지를 찾을 수 없습니다."));
    }

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

    private User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private TeamUser getMyTeamUser(User user) {
        return teamUserRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("소속된 팀을 찾을 수 없습니다."));
    }

    private void completeIfAllMembersSubmitted(Journal journal) {
        long memberCount = teamUserRepository.findByTeamId(journal.getTeam().getId()).size();
        long submittedCount = journalEntryRepository.countByJournalId(journal.getId());

        if (memberCount > 0 && submittedCount >= memberCount) {
            journal.complete();
        }
    }
}
