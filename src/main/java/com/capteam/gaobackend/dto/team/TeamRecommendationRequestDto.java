package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.enums.Grade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 어드민이 AI 팀 추천 요청 시 보내는 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamRecommendationRequestDto {

    // AI 팀 추천을 생성할 대상 학년을 받는 필드입니다.
    @NotNull
    private Grade grade;

    // 추천안 재생성 시 AI에게 전달할 추가 요구사항입니다.
    @Size(max = 1000)
    private String regenerationPrompt;

    // 어떤 버전을 기준으로 재생성했는지 diff 연결점을 남기기 위해 선택적으로 받습니다.
    private Long baseVersionId;
}
