package com.capteam.gaobackend.dto.journal;

import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.entity.JournalEntry;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.JournalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class JournalDetailResponseDto { //상세 조회

    private Long journalId;
    private String teamName;
    private String title;
    private String writerName;
    private List<String> teamMemberNames;
    private LocalDate date;
    private JournalStatus status;
    private String todayActivityContent;
    private List<EntryResponse> entries;

    public static JournalDetailResponseDto from(Journal journal, List<JournalEntry> entries) {
        return from(journal, null, List.of(), entries);
    }

    public static JournalDetailResponseDto from(Journal journal, User writer,
                                                List<User> teamMembers, List<JournalEntry> entries) {

        //가독성 안좋아서 리스트 별개 변수는 분리

        List<String> teamMemberNames = teamMembers.stream()
                .map(User::getName)
                .toList();

        String todayActivityContent = entries.stream()
                .map(JournalEntry::getTodayActivityContent)
                .filter(content -> content != null && !content.isBlank())
                .findFirst()
                .orElse(null);

        List<EntryResponse> entryResponses = entries.stream()
                .map(EntryResponse::from)
                .toList();

        return JournalDetailResponseDto.builder()
                .journalId(journal.getId())
                .teamName(journal.getTeam().getTeamName())
                .title(journal.getTitle())
                .writerName(writer == null ? null : writer.getName())
                .teamMemberNames(teamMemberNames)
                .date(journal.getDate())
                .status(journal.getStatus())
                .todayActivityContent(todayActivityContent)
                .entries(entryResponses)
                .build();
    }

    @Getter
    @Builder
    public static class EntryResponse {

        private Long entryId;
        private String writerId;
        private String writerName;
        private String activityContent;
        private String nextPlanContent;
        private String reflectionContent;

        public static EntryResponse from(JournalEntry entry) {
            return EntryResponse.builder()
                    .entryId(entry.getId())
                    .writerId(entry.getWriter().getUserId())
                    .writerName(entry.getWriter().getName())
                    .activityContent(entry.getActivityContent())
                    .nextPlanContent(entry.getNextPlanContent())
                    .reflectionContent(entry.getReflectionContent())
                    .build();
        }
    }
}
