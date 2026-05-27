package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.entity.Notice;
import com.capteam.gaobackend.enums.Important;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeResponseDto {

    private Long id;
    private String title;
    private String writer;      // 작성자 이름 수정
    private String content;         // content 추가
    private Important important;    // 목록에서도 중요 공지 태그 표시를 위해 내려줌
    private LocalDateTime createdAt;

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
