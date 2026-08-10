package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.entity.MatchingJob;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import com.capteam.gaobackend.repository.MatchingJobRepository;
import com.capteam.gaobackend.service.admin.AdminTeamMatchingPreparationService;
import com.capteam.gaobackend.service.admin.AdminTeamRecommendationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// AI 서버가 배치 하나를 끝낼 때마다 보내는 내부 콜백(POST /internal/matching/jobs/{jobId}/batch-complete)을
// 처리하는 서비스입니다. AdminTeamRecommendationService(동기 전체 매칭)와는 별개 경로라서 분리했습니다.
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingBatchCallbackService {

    private static final List<MatchingJobStatus> ACCEPTING_BATCHES_STATUSES = List.of(
            MatchingJobStatus.QUEUED,
            MatchingJobStatus.RUNNING
    );

    private final MatchingJobRepository matchingJobRepository;
    private final AdminTeamMatchingPreparationService matchingPreparationService;
    private final AdminTeamRecommendationPersistenceService recommendationPersistenceService;

    @Transactional
    public void recordBatch(String jobId, int batchIndex, Integer totalBatches, List<AiTeamSummaryResponseDto.TeamDto> teams) {
        MatchingJob job = matchingJobRepository.findWithLockById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("팀 매칭 작업을 찾을 수 없습니다: " + jobId));

        if (!ACCEPTING_BATCHES_STATUSES.contains(job.getStatus())) {
            // 취소되었거나 이미 끝난 작업으로 뒤늦게 도착한 배치는 저장하지 않습니다.
            log.warn("배치 콜백 무시. jobId={}, batchIndex={}, status={}", jobId, batchIndex, job.getStatus());
            return;
        }

        // 같은 배치 재전송은 저장 작업 자체를 다시 실행하지 않습니다.
        // 작업 row를 비관적 잠금으로 읽었기 때문에 동시에 들어온 콜백도 순서대로 검사됩니다.
        if (job.getReceivedBatchIndices().contains(batchIndex)) {
            log.info("이미 저장된 배치 콜백 무시. jobId={}, batchIndex={}", jobId, batchIndex);
            return;
        }

        if (teams != null && !teams.isEmpty()) {
            AdminTeamMatchingPreparationService.PreparedMatching prepared =
                    matchingPreparationService.prepare(job.getGrade());

            // appendBatchTeams는 팀 이름 기준으로 upsert하므로, 같은 batch_index가
            // 재전송되어 여기까지 다시 들어와도 팀 row가 중복 저장되지는 않습니다.
            recommendationPersistenceService.appendBatchTeams(
                    job.getGrade(),
                    jobId,
                    job.getRegenerationPrompt(),
                    prepared.nameToUserId(),
                    teams
            );
        }

        if (totalBatches != null && totalBatches > 0) {
            job.updateTotalBatches(totalBatches);
        }

        // 같은 batch_index를 재전송받은 경우엔 completedBatches를 다시 올리지 않습니다.
        boolean isFirstTimeForThisBatch = job.markBatchReceived(batchIndex);
        if (isFirstTimeForThisBatch) {
            job.incrementCompletedBatches();
        }
    }
}
