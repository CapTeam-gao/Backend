package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.enums.ResponseReliability;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_analysis")
public class UserAnalysis extends BaseTimeEntity implements Persistable<String> {

    @Id
    @Column(name = "user_id")
    // 분석 대상 사용자의 userId를 UserAnalysis의 기본키로 저장하는 필드입니다.
    private String userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    // UserAnalysis와 User를 같은 user_id 기본키로 연결하는 필드입니다.
    private User user;

    @Column(columnDefinition = "LONGTEXT")
    // AI 또는 추천 로직이 생성한 학생 분석 설명을 저장하는 필드입니다.
    private String analysisResult;

    @Enumerated(EnumType.STRING)
    // AI가 분석한 학생 실력 등급을 저장하는 필드입니다.
    private StudentLevel studentLevel; // AI가 분석한 학생 실력 (상/중/하), 어드민만 조회 가능

    @Enumerated(EnumType.STRING)
    // 설문 응답 일관성 기반 신뢰도를 저장하는 필드입니다.
    private ResponseReliability responseReliability;

    // 설문 전체 불일치 응답 수를 저장하는 필드입니다.
    private Integer inconsistentAnswers;

    // 성격 성향 문항 불일치 수를 저장하는 필드입니다.
    private Integer personalityInconsistentCount;

    // 개발 성향 문항 불일치 수를 저장하는 필드입니다.
    private Integer developmentInconsistentCount;

    @Transient
    // 직접 할당하는 userId 기본키 엔티티를 새 엔티티로 persist할지 판단하는 필드입니다.
    private boolean isNew = true;

    // 분석 대상 사용자와 분석 결과를 받아 UserAnalysis 엔티티를 생성하는 기능입니다.
    @Builder
    public UserAnalysis(
            User user,
            String analysisResult,
            StudentLevel studentLevel,
            ResponseReliability responseReliability,
            Integer inconsistentAnswers,
            Integer personalityInconsistentCount,
            Integer developmentInconsistentCount
    ) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("분석 대상 사용자가 필요합니다.");
        }
        this.userId = user.getUserId();
        this.user = user;
        this.analysisResult = analysisResult;
        this.studentLevel = studentLevel;
        this.responseReliability = responseReliability;
        this.inconsistentAnswers = inconsistentAnswers;
        this.personalityInconsistentCount = personalityInconsistentCount;
        this.developmentInconsistentCount = developmentInconsistentCount;
    }

    // 기존 분석 결과와 실력 등급을 최신 값으로 갱신하는 기능입니다.
    public void updateAnalysisResult(String analysisResult, StudentLevel studentLevel) {
        this.analysisResult = analysisResult;
        this.studentLevel = studentLevel;
    }

    // 설문 응답 신뢰도와 불일치 수를 최신 값으로 갱신하는 기능입니다.
    public void updateSurveyReliability(
            ResponseReliability responseReliability,
            Integer inconsistentAnswers,
            Integer personalityInconsistentCount,
            Integer developmentInconsistentCount
    ) {
        this.responseReliability = responseReliability;
        this.inconsistentAnswers = inconsistentAnswers;
        this.personalityInconsistentCount = personalityInconsistentCount;
        this.developmentInconsistentCount = developmentInconsistentCount;
    }

    // Spring Data JPA가 UserAnalysis의 기본키 값을 읽는 기능입니다.
    @Override
    public String getId() {
        return userId;
    }

    // userId가 이미 있어도 새 UserAnalysis는 merge가 아니라 persist로 저장되게 알려주는 기능입니다.
    @Override
    public boolean isNew() {
        return isNew;
    }

    // DB에서 조회되었거나 저장된 이후에는 기존 엔티티로 판단하게 변경하는 기능입니다.
    @PostLoad
    @PostPersist
    private void markNotNew() {
        this.isNew = false;
    }
}
