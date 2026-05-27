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

    private Long id;
    private String title;
    private String content;
    private String writer;
    private Important important;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
