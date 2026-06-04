package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.entity.Notice;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.Important;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeDetailResponseDto {

    // 공지 고유 id를 내려주는 필드입니다.
    private Long id;

    // 공지 제목을 내려주는 필드입니다.
    private String title;

    // 공지 본문 내용을 내려주는 필드입니다.
    private String content;

    // 공지 작성자 이름을 내려주는 필드입니다.
    private String writer;

    // 중요 공지 여부를 내려주는 필드입니다.
    private Important important;

    // 공지 작성 시각을 내려주는 필드입니다.
    private LocalDateTime createdAt;

    // 공지 마지막 수정 시각을 내려주는 필드입니다.
    private LocalDateTime updatedAt;

    // Notice 엔티티를 공지 상세 응답 DTO로 변환하는 기능입니다.
    public static NoticeDetailResponseDto from(Notice notice) {
        return NoticeDetailResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .writer(notice.getWriter().getName())
                .important(notice.getImportant())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
