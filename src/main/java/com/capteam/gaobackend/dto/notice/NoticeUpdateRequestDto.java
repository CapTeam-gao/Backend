package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.enums.Important;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeUpdateRequestDto {

    private String title;

    private String content;

    // 공지 수정 시 중요 공지 여부도 함께 변경할 수 있도록 받음
    private Important important;
}
