package com.capteam.gaobackend.dto.journal;

import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.JournalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class JournalListItemResponseDto {

    private Long journalId;
    private Long teamId;
    private String teamName;
    private Grade grade;
    private String serviceName;
    private LocalDate date;
    private JournalStatus status;
    private boolean submitted;

    public static JournalListItemResponseDto from(Journal journal, TeamProject teamProject) {
        return JournalListItemResponseDto.builder()
                .journalId(journal.getId())
                .teamId(journal.getTeam().getId())
                .teamName(journal.getTeam().getTeamName())
                .grade(journal.getTeam().getGrade())
                .serviceName(teamProject == null ? null : teamProject.getServiceName())
                .date(journal.getDate())
                .status(journal.getStatus())
                .submitted(journal.getStatus() == JournalStatus.COMPLETED)
                .build();
    }
}
