package com.capteam.gaobackend.dto.journal;

import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.entity.JournalEntry;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.enums.JournalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class JournalTodayResponseDto {

    // 오늘 내 팀 일지 id를 내려주는 필드입니다. 아직 생성되지 않았으면 null입니다.
    private Long journalId;

    // 현재 로그인한 사용자가 속한 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 조회 기준 날짜를 내려주는 필드입니다.
    private LocalDate date;

    // 오늘 내 팀 일지 상태를 내려주는 필드입니다. 아직 생성되지 않았으면 null입니다.
    private JournalStatus status;

    // 현재 로그인한 사용자가 오늘 일지를 제출했는지 내려주는 필드입니다.
    private boolean submitted;

    // 현재 로그인한 사용자가 오늘 일지를 작성 또는 수정할 수 있는지 내려주는 필드입니다.
    private boolean editable;

    // 팀원 전체가 오늘 일지를 제출했는지 내려주는 필드입니다.
    private boolean allSubmitted;

    // 현재 로그인한 사용자의 오늘 제출 내용을 내려주는 필드입니다.
    private MyEntryResponse myEntry;

    // 팀 전체 오늘 활동 요약을 내려주는 필드입니다.
    private String todayActivityContent;

    public static JournalTodayResponseDto empty(Team team, LocalDate date) {
        return JournalTodayResponseDto.builder()
                .teamName(team.getTeamName())
                .date(date)
                .submitted(false)
                .editable(true)
                .allSubmitted(false)
                .build();
    }

    public static JournalTodayResponseDto from(
            Journal journal,
            JournalEntry myEntry,
            List<JournalEntry> entries,
            boolean allSubmitted
    ) {
        return JournalTodayResponseDto.builder()
                .journalId(journal.getId())
                .teamName(journal.getTeam().getTeamName())
                .date(journal.getDate())
                .status(journal.getStatus())
                .submitted(myEntry != null)
                .editable(journal.getStatus() != JournalStatus.COMPLETED)
                .allSubmitted(allSubmitted)
                .myEntry(myEntry == null ? null : MyEntryResponse.from(myEntry))
                .todayActivityContent(resolveTodayActivityContent(entries))
                .build();
    }

    private static String resolveTodayActivityContent(List<JournalEntry> entries) {
        return entries.stream()
                .map(JournalEntry::getTodayActivityContent)
                .filter(content -> content != null && !content.isBlank())
                .findFirst()
                .orElse(null);
    }

    @Getter
    @Builder
    public static class MyEntryResponse {

        // 현재 로그인한 사용자가 오늘 진행한 작업 내용을 내려주는 필드입니다.
        private String activityContent;

        // 현재 로그인한 사용자의 다음 작업 계획을 내려주는 필드입니다.
        private String nextPlanContent;

        // 현재 로그인한 사용자의 회고 내용을 내려주는 필드입니다.
        private String reflectionContent;

        public static MyEntryResponse from(JournalEntry entry) {
            return MyEntryResponse.builder()
                    .activityContent(entry.getActivityContent())
                    .nextPlanContent(entry.getNextPlanContent())
                    .reflectionContent(entry.getReflectionContent())
                    .build();
        }
    }
}
