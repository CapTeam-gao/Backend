package com.capteam.gaobackend.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 추천안 내 두 학생의 팀을 교환할 때 사용하는 요청 DTO
@Getter
@NoArgsConstructor
public class SwapRecommendationMembersRequestDto {

    @NotNull
    private Long fromRecommendationId;  // 교환할 학생 A가 속한 추천안 id

    @NotBlank
    private String fromUserId;          // 교환할 학생 A의 userId

    @NotNull
    private Long toRecommendationId;    // 교환할 학생 B가 속한 추천안 id

    @NotBlank
    private String toUserId;            // 교환할 학생 B의 userId
}
