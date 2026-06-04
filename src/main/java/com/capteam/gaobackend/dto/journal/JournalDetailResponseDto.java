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

    // 일지 고유 id를 내려주는 필드입니다.
    private Long journalId;

    // 일지가 속한 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // 일지 제목을 내려주는 필드입니다.
    private String title;

    // 현재 로그인한 작성자 이름을 내려주는 필드입니다. 관리자 조회에서는 null일 수 있습니다.
    private String writerName;

    // 일지가 속한 팀원 이름 목록을 내려주는 필드입니다.
    private List<String> teamMemberNames;

    // 일지 작성 날짜를 내려주는 필드입니다.
    private LocalDate date;

    // 일지 제출 상태를 내려주는 필드입니다.
    private JournalStatus status;

    // 팀 전체 오늘 활동 요약을 내려주는 필드입니다.
    private String todayActivityContent;

    // 팀원별 개인 일지 제출 내용을 내려주는 필드입니다.
    private List<EntryResponse> entries;

    // 기존 호출부 호환을 위해 작성자/팀원 목록 없이 일지 상세 DTO를 만드는 기능입니다.
    public static JournalDetailResponseDto from(Journal journal, List<JournalEntry> entries) {
        return from(journal, null, List.of(), entries);
    }

    // 일지, 현재 작성자, 팀원 목록, 제출 목록을 상세 응답 DTO로 변환하는 기능입니다.
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

        // 개인 일지 제출 고유 id를 내려주는 필드입니다.
        private Long entryId;

        // 제출자 userId를 내려주는 필드입니다.
        private String writerId;

        // 제출자 이름을 내려주는 필드입니다.
        private String writerName;

        // 제출자가 오늘 진행한 작업 내용을 내려주는 필드입니다.
        private String activityContent;

        // 제출자의 다음 작업 계획을 내려주는 필드입니다.
        private String nextPlanContent;

        // 제출자의 회고 내용을 내려주는 필드입니다.
        private String reflectionContent;

        // JournalEntry 엔티티를 개인 일지 제출 응답 DTO로 변환하는 기능입니다.
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
