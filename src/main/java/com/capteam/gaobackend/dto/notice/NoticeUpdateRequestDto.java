package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.enums.Grade;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeUpdateRequestDto {

    private String title;

    private String content;

    private Grade grade;
}
