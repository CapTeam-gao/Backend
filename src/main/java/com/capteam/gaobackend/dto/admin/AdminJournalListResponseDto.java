package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.dto.journal.JournalListItemResponseDto;
import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.JournalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AdminJournalListResponseDto {

    // 관리자 일지 목록의 전체 항목 수를 내려주는 필드입니다.
    private int totalCount;

    // 제출 완료 상태인 일지 수를 내려주는 필드입니다.
    private int submittedCount;

    // 아직 제출 완료되지 않은 일지 수를 내려주는 필드입니다.
    private int notSubmittedCount;

    // 관리자 화면에 표시할 일지 목록을 내려주는 필드입니다.
    private List<JournalListItemResponseDto> journals;

    // 일지 목록 항목을 받아 전체/제출/미제출 수를 계산한 관리자 일지 목록 DTO로 변환하는 기능입니다.
    public static AdminJournalListResponseDto from(List<JournalListItemResponseDto> journals) {
        int submittedCount = (int) journals.stream()
                .filter(JournalListItemResponseDto::isSubmitted)
                .count();

        return AdminJournalListResponseDto.builder()
                .totalCount(journals.size())
                .submittedCount(submittedCount)
                .notSubmittedCount(journals.size() - submittedCount)
                .journals(journals)
                .build();
    }
}
