package com.capteam.gaobackend.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPersonalityScore {

    // 아이디어 기획 성향 점수를 저장하는 필드입니다.
    private Double ideaPlanning = 0.0;

    // 소통 성향 점수를 저장하는 필드입니다.
    private Double communication = 0.0;

    // 역할 유연성 점수를 저장하는 필드입니다.
    private Double roleFlexibility = 0.0;

    // 시간 압박 대응 점수를 저장하는 필드입니다.
    private Double timePressure = 0.0;

    // 체력/집중 유지 점수를 저장하는 필드입니다.
    private Double staminaFocus = 0.0;

    // 성격 성향 항목별 점수 객체를 생성하는 기능입니다.
    public UserPersonalityScore(Double ideaPlanning, Double communication, Double roleFlexibility,
                                Double timePressure, Double staminaFocus) {
        this.ideaPlanning = ideaPlanning;
        this.communication = communication;
        this.roleFlexibility = roleFlexibility;
        this.timePressure = timePressure;
        this.staminaFocus = staminaFocus;
    }

    // 기존 DB/코드 호환을 위한 과거 성향 이름 getter입니다.
    public Double getResponsibility() {
        return ideaPlanning;
    }

    public Double getCollaboration() {
        return roleFlexibility;
    }

    public Double getFlexibility() {
        return timePressure;
    }

    public Double getEmotionalStability() {
        return staminaFocus;
    }
}
