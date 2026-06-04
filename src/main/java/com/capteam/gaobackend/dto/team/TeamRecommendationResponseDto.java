package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.RecommendationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 추천 목록 조회 시 반환하는 DTO (간략 정보)
@Getter
@Builder
public class TeamRecommendationResponseDto {

    // 팀 추천안 고유 id를 내려주는 필드입니다.
    private Long id;

    // 추천 대상 학년을 내려주는 필드입니다.
    private Grade grade;

    // 추천안 상태를 내려주는 필드입니다. 예: PENDING, ACCEPTED
    private RecommendationStatus status;

    // 추천안 생성 시각을 내려주는 필드입니다.
    private LocalDateTime createdAt;

    // TeamRecommendation 엔티티를 추천 목록 응답 DTO로 변환하는 기능입니다.
    public static TeamRecommendationResponseDto from(TeamRecommendation recommendation) {
        return TeamRecommendationResponseDto.builder()
                .id(recommendation.getId())
                .grade(recommendation.getGrade())
                .status(recommendation.getStatus())
                .createdAt(recommendation.getCreatedAt())
                .build();
    }
}
