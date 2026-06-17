package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.enums.Grade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TeamRecommendationDetailResponseDtoTest {

    @Test
    void includesStrengthsAndWeaknessesFromRecommendation() {
        TeamRecommendation recommendation = TeamRecommendation.builder()
                .grade(Grade.GRADE_2)
                .strengths("프론트엔드와 백엔드 역할이 균형 있게 구성되었습니다.")
                .weaknesses("AI 역할 인원이 적어 분석 로직이 특정 학생에게 집중될 수 있습니다.")
                .build();

        TeamRecommendationDetailResponseDto response = TeamRecommendationDetailResponseDto.from(
                recommendation,
                List.of(),
                List.of(),
                Map.of()
        );

        assertThat(response.getStrengths())
                .isEqualTo("프론트엔드와 백엔드 역할이 균형 있게 구성되었습니다.");
        assertThat(response.getWeaknesses())
                .isEqualTo("AI 역할 인원이 적어 분석 로직이 특정 학생에게 집중될 수 있습니다.");
    }
}
