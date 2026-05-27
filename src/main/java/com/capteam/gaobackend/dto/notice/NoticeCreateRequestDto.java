package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.enums.Important;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeCreateRequestDto {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    // 프론트 체크박스 값이 IMPORTANT 또는 COMMON 문자열로 들어옴
    private Important important;
}
