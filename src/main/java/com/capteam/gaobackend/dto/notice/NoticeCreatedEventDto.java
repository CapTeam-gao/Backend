package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.entity.Notice;
import com.capteam.gaobackend.enums.Important;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeCreatedEventDto {

    // 프론트가 새 공지 생성 이벤트인지 구분할 수 있게 내려주는 이벤트 타입 필드입니다.
    private String eventType;

    // 새로 생성된 공지 id를 내려주는 필드입니다.
    private Long noticeId;

    // 새로 생성된 공지 제목을 내려주는 필드입니다.
    private String title;

    // 새로 생성된 공지가 중요 공지인지 내려주는 필드입니다.
    private boolean important;

    // 새 공지가 생성된 시각을 내려주는 필드입니다.
    private LocalDateTime createdAt;

    // Notice 엔티티를 WebSocket 새 공지 이벤트 DTO로 변환하는 기능입니다.
    public static NoticeCreatedEventDto from(Notice notice) {
        return NoticeCreatedEventDto.builder()
                .eventType("NOTICE_CREATED")
                .noticeId(notice.getId())
                .title(notice.getTitle())
                .important(notice.getImportant() == Important.IMPORTANT)
                .createdAt(notice.getCreatedAt())
                .build();
    }

    // 공지 상세 응답 DTO를 WebSocket 새 공지 이벤트 DTO로 변환하는 기능입니다.
    public static NoticeCreatedEventDto from(NoticeDetailResponseDto notice) {
        return NoticeCreatedEventDto.builder()
                .eventType("NOTICE_CREATED")
                .noticeId(notice.getId())
                .title(notice.getTitle())
                .important(notice.getImportant() == Important.IMPORTANT)
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
