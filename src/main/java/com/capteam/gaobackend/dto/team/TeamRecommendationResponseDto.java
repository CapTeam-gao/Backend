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

    private Long id;
    private Grade grade;              // 대상 학년
    private RecommendationStatus status; // PENDING / ACCEPTED
    private LocalDateTime createdAt;  // 추천 생성 시각

    public static TeamRecommendationResponseDto from(TeamRecommendation recommendation) {
        return TeamRecommendationResponseDto.builder()
                .id(recommendation.getId())
                .grade(recommendation.getGrade())
                .status(recommendation.getStatus())
                .createdAt(recommendation.getCreatedAt())
                .build();
    }
}
