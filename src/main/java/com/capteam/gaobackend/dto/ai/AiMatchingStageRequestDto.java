package com.capteam.gaobackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiMatchingStageRequestDto {

    @JsonProperty("progress_step")
    private int progressStep;
}
