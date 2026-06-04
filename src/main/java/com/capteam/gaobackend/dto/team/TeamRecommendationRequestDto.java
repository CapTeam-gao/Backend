package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.enums.Grade;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 어드민이 AI 팀 추천 요청 시 보내는 DTO
@Getter
@NoArgsConstructor
public class TeamRecommendationRequestDto {

    // AI 팀 추천을 생성할 대상 학년을 받는 필드입니다.
    @NotNull
    private Grade grade;
}
