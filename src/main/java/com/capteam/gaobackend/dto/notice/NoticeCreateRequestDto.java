package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.enums.Grade;
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

    private Important Important;    // null이면 전체 학년 대상
}
