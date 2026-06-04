package com.capteam.gaobackend.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPersonalityScore {

    // 소통 성향 점수를 저장하는 필드입니다.
    private Double communication = 0.0;

    // 책임감 성향 점수를 저장하는 필드입니다.
    private Double responsibility = 0.0;

    // 협업 성향 점수를 저장하는 필드입니다.
    private Double collaboration = 0.0;

    // 유연성 성향 점수를 저장하는 필드입니다.
    private Double flexibility = 0.0;

    // 감정 안정성 점수를 저장하는 필드입니다.
    private Double emotionalStability = 0.0;

    // 성격 성향 항목별 점수 객체를 생성하는 기능입니다.
    public UserPersonalityScore(Double communication, Double responsibility, Double collaboration,
                                Double flexibility, Double emotionalStability) {
        this.communication = communication;
        this.responsibility = responsibility;
        this.collaboration = collaboration;
        this.flexibility = flexibility;
        this.emotionalStability = emotionalStability;
    }
}
