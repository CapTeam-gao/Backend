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

    private int totalCount;
    private int submittedCount;
    private int notSubmittedCount;
    private List<JournalListItemResponseDto> journals;

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
