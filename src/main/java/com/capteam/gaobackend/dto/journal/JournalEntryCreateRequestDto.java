package com.capteam.gaobackend.dto.journal;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JournalEntryCreateRequestDto {

    // 오늘 개인이 진행한 작업 내용을 받는 필드입니다.
    @NotBlank
    private String activityContent;

    // 다음 작업 계획 내용을 받는 필드입니다.
    @NotBlank
    private String nextPlanContent;

    // 회고 내용을 받는 필드입니다.
    @NotBlank
    private String reflectionContent;
}
