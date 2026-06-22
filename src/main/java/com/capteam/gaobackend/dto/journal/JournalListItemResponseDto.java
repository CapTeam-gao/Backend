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

    // 프로젝트 기획서에 작성한 팀명을 내려주는 필드입니다.
    private String projectTeamName;

    // 팀 학년을 내려주는 필드입니다.
    private Grade grade;

    // 팀 프로젝트 서비스 이름을 내려주는 필드입니다.
    private String serviceName;

    // 일지 날짜를 내려주는 필드입니다.
    private LocalDate date;

    // 일지 제출 상태를 내려주는 필드입니다.
    private JournalStatus status;

    // 해당 일지에 개인 일지를 제출한 팀원 수를 내려주는 필드입니다.
    private int submittedMemberCount;

    // 해당 일지가 속한 팀의 전체 팀원 수를 내려주는 필드입니다.
    private int totalMemberCount;

    // 아직 개인 일지를 제출하지 않은 팀원 수를 내려주는 필드입니다.
    private int notSubmittedMemberCount;

    // 팀원 전원이 제출 완료했는지 내려주는 필드입니다.
    private boolean submitted;

    // Journal, TeamProject, 제출 인원 정보를 관리자 일지 목록 항목 DTO로 변환하는 기능입니다.
    public static JournalListItemResponseDto from(
            Journal journal,    //일지 정보 조회하려고
            TeamProject teamProject,    //팀프로젝트에서 서비스명 가져오려고
            int submittedMemberCount,   //몇명 제출했는지 가져오려고 서비스 로직에서
            int totalMemberCount    //  팀원 총 수
    ) {
        int notSubmittedMemberCount = Math.max(totalMemberCount - submittedMemberCount, 0); //몇 명 제출 안했는지 총 인원 - 제출한 팀원으로 계산
        boolean submitted = totalMemberCount > 0 && submittedMemberCount >= totalMemberCount;

        return JournalListItemResponseDto.builder()
                .journalId(journal.getId())
                .teamId(journal.getTeam().getId())
                .teamName(journal.getTeam().getTeamName())
                .projectTeamName(teamProject == null ? null : teamProject.getTeamName())
                .grade(journal.getTeam().getGrade())
                .serviceName(teamProject == null ? null : teamProject.getServiceName())
                .date(journal.getDate())
                .status(journal.getStatus())
                .submittedMemberCount(submittedMemberCount)
                .totalMemberCount(totalMemberCount)
                .notSubmittedMemberCount(notSubmittedMemberCount)
                .submitted(submitted)
                .build();
    }
}
