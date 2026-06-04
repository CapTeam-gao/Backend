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

    // 일지 고유 id를 내려주는 필드입니다.
    private Long journalId;

    // 일지가 속한 팀 id를 내려주는 필드입니다.
    private Long teamId;

    // 일지가 속한 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 팀 학년을 내려주는 필드입니다.
    private Grade grade;

    // 팀 프로젝트 서비스 이름을 내려주는 필드입니다.
    private String serviceName;

    // 일지 날짜를 내려주는 필드입니다.
    private LocalDate date;

    // 일지 제출 상태를 내려주는 필드입니다.
    private JournalStatus status;

    // 팀원 전원이 제출 완료했는지 내려주는 필드입니다.
    private boolean submitted;

    // Journal과 TeamProject 엔티티를 관리자 일지 목록 항목 DTO로 변환하는 기능입니다.
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
