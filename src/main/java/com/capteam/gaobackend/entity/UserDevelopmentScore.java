package com.capteam.gaobackend.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevelopmentScore {

    // 설문 문항이 바뀌어도 개발 성향 결과는 아래 5개 항목 기준으로 저장합니다.

    // 구현 실행력 점수를 저장하는 필드입니다.
    private Double implementation = 0.0;

    // 문제 해결력 점수를 저장하는 필드입니다.
    private Double problemSolving = 0.0;

    // 완성도 점수를 저장하는 필드입니다.
    private Double completionQuality = 0.0;

    // 발표/전달력 점수를 저장하는 필드입니다.
    private Double presentation = 0.0;

    // 리더십 성향 점수를 저장하는 필드입니다.
    private Double leadership = 0.0;

    // 개발 성향 항목별 점수 객체를 생성하는 기능입니다.
    public UserDevelopmentScore(Double implementation, Double problemSolving, Double completionQuality,
                                Double presentation, Double leadership) {
        this.implementation = implementation;
        this.problemSolving = problemSolving;
        this.completionQuality = completionQuality;
        this.presentation = presentation;
        this.leadership = leadership;
    }

    // 기존 DB/코드 호환을 위한 과거 개발 성향 이름 getter입니다.
    public Double getLearningAbility() {
        return completionQuality;
    }

    public Double getPlanning() {
        return presentation;
    }
}
