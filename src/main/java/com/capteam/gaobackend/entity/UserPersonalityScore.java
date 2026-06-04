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
    private Integer communication = 0;

    // 책임감 성향 점수를 저장하는 필드입니다.
    private Integer responsibility = 0;

    // 협업 성향 점수를 저장하는 필드입니다.
    private Integer collaboration = 0;

    // 유연성 성향 점수를 저장하는 필드입니다.
    private Integer flexibility = 0;

    // 감정 안정성 점수를 저장하는 필드입니다.
    private Integer emotionalStability = 0;

    // 성격 성향 항목별 점수 객체를 생성하는 기능입니다.
    public UserPersonalityScore(Integer communication, Integer responsibility, Integer collaboration,
                                Integer flexibility, Integer emotionalStability) {
        this.communication = communication;
        this.responsibility = responsibility;
        this.collaboration = collaboration;
        this.flexibility = flexibility;
        this.emotionalStability = emotionalStability;
    }
}
