package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.admin.AdminJournalListResponseDto;
import com.capteam.gaobackend.dto.journal.JournalDetailResponseDto;
import com.capteam.gaobackend.dto.journal.JournalListItemResponseDto;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.repository.JournalRepository;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminJournalService {

    private final JournalRepository journalRepository;
    private final TeamProjectRepository teamProjectRepository;
    private final JournalService journalService;


    // 일지 리스트 조회
    public AdminJournalListResponseDto getJournalList() {
        Map<Long, TeamProject> teamProjectMap = teamProjectRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        teamProject -> teamProject.getTeam().getId(),
                        teamProject -> teamProject
                ));

        var journals = journalRepository.findAllByOrderByDateDesc()
                .stream()
                .map(journal -> JournalListItemResponseDto.from(
                        journal,
                        teamProjectMap.get(journal.getTeam().getId())
                ))
                .toList();

        return AdminJournalListResponseDto.from(journals);
    }


    // 일지 상세 조회
    public JournalDetailResponseDto getJournalDetail(Long journalId) {
        return journalService.getAdminJournalDetail(journalId);
    }
}
