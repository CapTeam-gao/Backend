package com.capteam.gaobackend.dto.journal;

import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.enums.JournalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class JournalResponseDto {

    private Long journalId;
    private String teamName;
    private String title;
    private LocalDate date;
    private JournalStatus status;

    public static JournalResponseDto from(Journal journal) {
        return JournalResponseDto.builder()
                .journalId(journal.getId())
                .teamName(journal.getTeam().getTeamName())
                .title(journal.getTitle())
                .date(journal.getDate())
                .status(journal.getStatus())
                .build();
    }
}
