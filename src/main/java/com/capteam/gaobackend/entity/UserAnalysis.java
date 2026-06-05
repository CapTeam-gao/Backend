package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.StudentLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_analysis")
public class UserAnalysis extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private String userId;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private User user;

    @Column(columnDefinition = "LONGTEXT")
    private String analysisResult;

    @Enumerated(EnumType.STRING)
    private StudentLevel studentLevel; // AI가 분석한 학생 실력 (상/중/하), 어드민만 조회 가능

    @Builder
    public UserAnalysis(User user, String analysisResult, StudentLevel studentLevel) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("분석 대상 사용자가 필요합니다.");
        }
        this.userId = user.getUserId();
        this.user = user;
        this.analysisResult = analysisResult;
        this.studentLevel = studentLevel;
    }

    public void updateAnalysisResult(String analysisResult, StudentLevel studentLevel) {
        this.analysisResult = analysisResult;
        this.studentLevel = studentLevel;
    }
}
