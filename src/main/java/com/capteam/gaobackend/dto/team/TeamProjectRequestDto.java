package com.capteam.gaobackend.dto.team;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamProjectRequestDto {
    // 기획서에 저장할 팀명을 받는 필드입니다.
    @NotBlank
    private String teamName;

    // 기획서에 저장할 서비스명을 받는 필드입니다.
    @NotBlank
    private String serviceName;

    // 기획서에 저장할 서비스 소개를 받는 필드입니다.
    @NotBlank
    private String serviceIntro;

    // 기획서에 저장할 주요 기능을 받는 필드입니다.
    @NotBlank
    private String mainFeatures;
}
