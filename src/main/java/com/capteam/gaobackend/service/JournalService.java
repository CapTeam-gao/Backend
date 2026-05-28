package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.journal.JournalDetailResponseDto;
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

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalService {

    private final JournalRepository journalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TeamUserRepository teamUserRepository;
    private final UserRepository userRepository;

    public JournalDetailResponseDto getMyTeamJournalDetail(Long journalId) {
        User user = getAuthenticatedUser();

        TeamUser myTeamUser = teamUserRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("소속된 팀을 찾을 수 없습니다."));

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
}
