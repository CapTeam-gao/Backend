package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.enums.Important;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeUpdateRequestDto {

    // 수정할 공지 제목을 받는 필드입니다.
    private String title;

    // 수정할 공지 본문 내용을 받는 필드입니다.
    private String content;

    // 공지 수정 시 중요 공지 여부도 함께 변경하기 위해 받는 필드입니다.
    private Important important;
}
