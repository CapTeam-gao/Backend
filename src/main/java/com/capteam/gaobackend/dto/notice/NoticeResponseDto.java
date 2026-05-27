package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.entity.Notice;
import com.capteam.gaobackend.enums.Grade;
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
//    private Grade grade;            // 대상 학년 (null이면 전체)
    private LocalDateTime createdAt;

    public static NoticeResponseDto from(Notice notice) {
        return NoticeResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .writer(notice.getWriter().getName())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
