package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.enums.Important;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeCreateRequestDto {

    // 새 공지 제목을 받는 필드입니다.
    @NotBlank
    private String title;

    // 새 공지 본문 내용을 받는 필드입니다.
    @NotBlank
    private String content;

    // 프론트 체크박스 값으로 IMPORTANT 또는 COMMON 문자열을 받는 필드입니다.
    private Important important;
}
