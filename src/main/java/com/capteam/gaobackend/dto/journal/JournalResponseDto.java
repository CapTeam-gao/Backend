package com.capteam.gaobackend.dto.journal;

import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.enums.JournalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class JournalResponseDto {

    // 일지 고유 id를 내려주는 필드입니다.
    private Long journalId;

    // 일지가 속한 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 프로젝트 기획서에 작성한 팀명을 내려주는 필드입니다.
    private String projectTeamName;

    // 일지 제목을 내려주는 필드입니다.
    private String title;

    // 일지 날짜를 내려주는 필드입니다.
    private LocalDate date;

    // 일지 제출 상태를 내려주는 필드입니다.
    private JournalStatus status;

    // Journal 엔티티를 학생 일지 목록 응답 DTO로 변환하는 기능입니다.
    public static JournalResponseDto from(Journal journal) {
        return from(journal, null);
    }

    // 프로젝트 기획서 팀명을 포함해 학생 일지 목록 응답 DTO로 변환하는 기능입니다.
    public static JournalResponseDto from(Journal journal, String projectTeamName) {
        return JournalResponseDto.builder()
                .journalId(journal.getId())
                .teamName(journal.getTeam().getTeamName())
                .projectTeamName(projectTeamName)
                .title(journal.getTitle())
                .date(journal.getDate())
                .status(journal.getStatus())
                .build();
    }
}
