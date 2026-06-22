package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.admin.AdminJournalListResponseDto;
import com.capteam.gaobackend.dto.journal.JournalDetailResponseDto;
import com.capteam.gaobackend.dto.journal.JournalListItemResponseDto;
import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.repository.JournalEntryRepository;
import com.capteam.gaobackend.repository.JournalRepository;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminJournalService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    // 관리자 일지 목록 조회에 사용할 일지 Repository 필드입니다.
    private final JournalRepository journalRepository;

    // 일지 생성 여부와 관계없이 관리자 목록에 모든 팀을 포함하기 위한 Repository 필드입니다.
    private final TeamRepository teamRepository;

    // 일지 목록에 프로젝트 서비스명을 함께 보여주기 위한 Repository 필드입니다.
    private final TeamProjectRepository teamProjectRepository;

    // 일지별 개인 제출 수를 계산하기 위한 Repository 필드입니다.
    private final JournalEntryRepository journalEntryRepository;

    // 팀별 전체 팀원 수를 계산하기 위한 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 일지 상세 조회 로직을 재사용하기 위한 공통 JournalService 필드입니다.
    private final JournalService journalService;

    // 전체 일지 목록과 제출/미제출 통계를 조회하는 기능입니다.
    public AdminJournalListResponseDto getJournalList() {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        Map<Long, TeamProject> teamProjectMap = teamProjectRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        teamProject -> teamProject.getTeam().getId(),
                        teamProject -> teamProject
                ));
        Map<Long, Long> teamMemberCountMap = teamUserRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        teamUser -> teamUser.getTeam().getId(),
                        Collectors.counting()
                ));
        Map<Long, Long> journalEntryCountMap = journalEntryRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        journalEntry -> journalEntry.getJournal().getId(),
                        Collectors.counting()
                ));
        Map<Long, Journal> journalByTeamId = journalRepository.findByDate(today)
                .stream()
                .collect(Collectors.toMap(
                        journal -> journal.getTeam().getId(),
                        Function.identity(),
                        (first, ignored) -> first
                ));

        var journals = teamRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Team::getId))
                .map(team -> {
                    Journal journal = journalByTeamId.get(team.getId());
                    int submittedMemberCount = journal == null
                            ? 0
                            : journalEntryCountMap.getOrDefault(journal.getId(), 0L).intValue();

                    return JournalListItemResponseDto.from(
                        team,
                        journal,
                        teamProjectMap.get(team.getId()),
                        submittedMemberCount,
                        teamMemberCountMap.getOrDefault(team.getId(), 0L).intValue(),
                        today
                    );
                })
                .toList();

        return AdminJournalListResponseDto.from(journals);
    }


    // 관리자가 특정 일지 상세를 조회하는 기능입니다.
    public JournalDetailResponseDto getJournalDetail(Long journalId) {
        return journalService.getAdminJournalDetail(journalId);
    }
}
