package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.TeamMatchingVersionStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "team_matching_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_matching_version_grade_number",
                        columnNames = {"grade", "version_number"}
                )
        }
)
public class TeamMatchingVersion extends BaseTimeEntity {

    // 추천 버전 자체를 식별하는 PK입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 학년 추천 묶음인지 버전별 조회와 적용 범위를 구분하기 위해 저장합니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    // 같은 학년 안에서 버전 목록을 시간순으로 비교할 수 있게 증가 번호를 보관합니다.
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    // 비동기 작업과 버전 저장 결과를 연결하기 위해 jobId를 함께 남깁니다.
    @Column(name = "job_id", length = 36)
    private String jobId;

    // 실제 운영 반영 전 임시본과 적용본을 구분하기 위해 버전 상태를 저장합니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamMatchingVersionStatus status;

    // 재생성 지시문을 버전 단위로 남겨두면 diff 검토와 사후 분석이 쉬워집니다.
    @Column(name = "regeneration_prompt", length = 1000)
    private String regenerationPrompt;

    @Builder
    public TeamMatchingVersion(
            Grade grade,
            Integer versionNumber,
            String jobId,
            String regenerationPrompt
    ) {
        this.grade = grade;
        this.versionNumber = versionNumber;
        this.jobId = jobId;
        this.regenerationPrompt = regenerationPrompt;
        this.status = TeamMatchingVersionStatus.DRAFT;
    }

    // 추천 버전을 실제 확정본으로 승격할 때 호출합니다.
    public void apply() {
        this.status = TeamMatchingVersionStatus.APPLIED;
    }

    // 화면에서 더 이상 사용할 버전이 아니면 숨길 수 있도록 폐기 상태로 변경합니다.
    public void discard() {
        this.status = TeamMatchingVersionStatus.DISCARDED;
    }
}
