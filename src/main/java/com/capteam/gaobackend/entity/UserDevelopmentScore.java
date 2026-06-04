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

    // 리더십 성향 점수를 저장하는 필드입니다.
    private Double leadership = 0.0;

    // 문제 해결력 점수를 저장하는 필드입니다.
    private Double problemSolving = 0.0;

    // 구현 실행력 점수를 저장하는 필드입니다.
    private Double implementation = 0.0;

    // 학습 성장성 점수를 저장하는 필드입니다.
    private Double learningAbility = 0.0;

    // 기획 정리력 점수를 저장하는 필드입니다.
    private Double planning = 0.0;

    // 개발 성향 항목별 점수 객체를 생성하는 기능입니다.
    public UserDevelopmentScore(Double leadership, Double problemSolving, Double implementation,
                                Double learningAbility, Double planning) {
        this.leadership = leadership;
        this.problemSolving = problemSolving;
        this.implementation = implementation;
        this.learningAbility = learningAbility;
        this.planning = planning;
    }
}
