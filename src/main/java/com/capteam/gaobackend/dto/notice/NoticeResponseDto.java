package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.entity.Notice;
import com.capteam.gaobackend.enums.Important;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeResponseDto {

    // 공지 고유 id를 내려주는 필드입니다.
    private Long id;

    // 공지 제목을 내려주는 필드입니다.
    private String title;

    // 공지 작성자 이름을 내려주는 필드입니다.
    private String writer;

    // 목록에서 미리보기나 상세 이동 전 표시할 공지 내용을 내려주는 필드입니다.
    private String content;

    // 목록에서도 중요 공지 태그를 표시하기 위해 내려주는 필드입니다.
    private Important important;

    // 공지 작성 시각을 내려주는 필드입니다.
    private LocalDateTime createdAt;

    // Notice 엔티티를 공지 목록 응답 DTO로 변환하는 기능입니다.
    public static NoticeResponseDto from(Notice notice) {
        return NoticeResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .writer(notice.getWriter().getName())
                // 프론트에서 IMPORTANT일 때만 중요 태그를 보여줌
                .important(notice.getImportant())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
