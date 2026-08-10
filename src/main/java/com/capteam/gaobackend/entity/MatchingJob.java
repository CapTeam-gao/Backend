package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Table(name = "matching_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchingJob extends BaseTimeEntity {

    // 백엔드와 AI 서버가 동일한 작업을 식별할 때 사용하는 UUID입니다.
    @Id
    @Column(length = 36)
    private String id;

    // 작업 진행 상태를 DB에 저장해 요청 연결이 종료되어도 조회할 수 있게 합니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingJobStatus status;

    @Column(length = 1000)
    private String errorMessage;

    @Column(length = 1000)
    private String regenerationPrompt;

    @Column(name = "base_version_id")
    private Long baseVersionId;

    // 배치 단위 진행률 표시(6번)를 위한 카운터입니다. AI가
    // POST /internal/matching/jobs/{jobId}/batch-complete로 배치 완료를 알려줄 때마다
    // MatchingBatchCallbackService가 갱신합니다. AI가 아직 이 콜백을 안 보내는 경우엔 0/0으로 남습니다.
    @Column(name = "total_batches", nullable = false)
    private int totalBatches;

    @Column(name = "completed_batches", nullable = false)
    private int completedBatches;

    // 프론트 로딩 화면의 1~4단계를 나타냅니다. 값은 0부터 시작하며
    // AI가 실제 워크플로우 단계에 진입할 때 내부 콜백으로 단조 증가시킵니다.
    @Column(name = "progress_step", nullable = false)
    private int progressStep;

    // AI가 네트워크 재시도 등으로 같은 batch_index를 다시 보내도 completedBatches가
    // 중복으로 올라가지 않도록(멱등 처리) 이미 받은 batch_index를 기억해둡니다.
    @ElementCollection
    @CollectionTable(name = "matching_job_batch_indices", joinColumns = @JoinColumn(name = "matching_job_id"))
    @Column(name = "batch_index")
    private Set<Integer> receivedBatchIndices = new HashSet<>();

    public MatchingJob(String id, Grade grade) {
        this(id, grade, null, null);
    }

    public MatchingJob(String id, Grade grade, String regenerationPrompt) {
        this(id, grade, regenerationPrompt, null);
    }

    public MatchingJob(String id, Grade grade, String regenerationPrompt, Long baseVersionId) {
        this.id = id;
        this.grade = grade;
        this.regenerationPrompt = regenerationPrompt;
        this.baseVersionId = baseVersionId;
        this.status = MatchingJobStatus.QUEUED;
    }

    public boolean start() {
        if (status != MatchingJobStatus.QUEUED) {
            return false;
        }
        status = MatchingJobStatus.RUNNING;
        return true;
    }

    // 결과 저장이 시작된 이후에는 부분 저장을 막기 위해 취소를 허용하지 않습니다.
    public boolean beginCompletion() {
        if (status != MatchingJobStatus.RUNNING) {
            return false;
        }
        status = MatchingJobStatus.COMPLETING;
        return true;
    }

    public void succeed() {
        if (status == MatchingJobStatus.COMPLETING) {
            status = MatchingJobStatus.SUCCEEDED;
        }
    }

    public void fail(String message) {
        // 늦게 도착한 예외가 이미 확정된 취소/성공 상태를 덮어쓰지 않게 합니다.
        if (status == MatchingJobStatus.CANCELLED || status == MatchingJobStatus.SUCCEEDED) {
            return;
        }
        status = MatchingJobStatus.FAILED;
        errorMessage = message;
    }

    public void updateTotalBatches(int totalBatches) {
        this.totalBatches = totalBatches;
    }

    public void incrementCompletedBatches() {
        this.completedBatches++;
    }

    public void updateProgressStep(int progressStep) {
        if (progressStep < 0 || progressStep > 3) {
            throw new IllegalArgumentException("팀 매칭 진행 단계는 0~3이어야 합니다.");
        }
        this.progressStep = Math.max(this.progressStep, progressStep);
    }

    // 처음 보는 batch_index면 true를 반환하며 기억해두고, 이미 받은 적 있으면 false를
    // 반환합니다. 호출부는 true일 때만 incrementCompletedBatches()를 호출해야 합니다.
    public boolean markBatchReceived(int batchIndex) {
        return receivedBatchIndices.add(batchIndex);
    }

    public boolean cancel() {
        // AI 호출 전 또는 실행 중인 작업만 취소할 수 있습니다.
        if (status != MatchingJobStatus.QUEUED && status != MatchingJobStatus.RUNNING) {
            return false;
        }
        status = MatchingJobStatus.CANCELLED;
        return true;
    }
}
