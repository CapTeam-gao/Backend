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

    // 관리자 일지 목록 조회에 사용할 일지 Repository 필드입니다.
    private final JournalRepository journalRepository;

    // 일지 목록에 프로젝트 서비스명을 함께 보여주기 위한 Repository 필드입니다.
    private final TeamProjectRepository teamProjectRepository;

    // 일지 상세 조회 로직을 재사용하기 위한 공통 JournalService 필드입니다.
    private final JournalService journalService;


    // 전체 일지 목록과 제출/미제출 통계를 조회하는 기능입니다.
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


    // 관리자가 특정 일지 상세를 조회하는 기능입니다.
    public JournalDetailResponseDto getJournalDetail(Long journalId) {
        return journalService.getAdminJournalDetail(journalId);
    }
}
