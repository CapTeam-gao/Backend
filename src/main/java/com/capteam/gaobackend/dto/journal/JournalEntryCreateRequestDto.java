package com.capteam.gaobackend.dto.journal;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JournalEntryCreateRequestDto {

    @NotBlank
    private String activityContent;

    @NotBlank
    private String nextPlanContent;

    @NotBlank
    private String reflectionContent;
}
