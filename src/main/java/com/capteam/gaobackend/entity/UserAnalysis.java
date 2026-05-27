package com.capteam.gaobackend.entity;

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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

//    @Column(columnDefinition = "LONGTEXT")
    private String analysisResult;

    @Builder
    public UserAnalysis(User user, String analysisResult) {
        this.user = user;
        this.analysisResult = analysisResult;
    }

    public void updateAnalysisResult(String analysisResult) {
        this.analysisResult = analysisResult;
    }
}
