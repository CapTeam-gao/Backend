package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.dto.journal.JournalListItemResponseDto;
import lombok.Builder;
import lombok.Getter;

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
