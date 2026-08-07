package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.team.MatchingJobResponseDto;
import com.capteam.gaobackend.entity.MatchingJob;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import com.capteam.gaobackend.repository.MatchingJobRepository;
import com.capteam.gaobackend.repository.TeamMatchingVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingJobStateService {

    private static final List<MatchingJobStatus> ACTIVE_STATUSES = List.of(
            MatchingJobStatus.QUEUED,
            MatchingJobStatus.RUNNING,
            MatchingJobStatus.COMPLETING
    );

    private final MatchingJobRepository matchingJobRepository;
    private final TeamMatchingVersionRepository teamMatchingVersionRepository;

    // 중복 매칭 실행으로 추천안이 서로 덮어쓰이고 AI 서버가 동시에 과부하되는 것을 방지합니다.
    @Transactional
    public synchronized MatchingJobResponseDto create(Grade grade) {
        return create(grade, null);
    }

    @Transactional
    public synchronized MatchingJobResponseDto create(Grade grade, String regenerationPrompt) {
        return create(grade, regenerationPrompt, null);
    }

    @Transactional
    public synchronized MatchingJobResponseDto create(Grade grade, String regenerationPrompt, Long baseVersionId) {
        MatchingJobResponseDto activeJob = matchingJobRepository
                .findFirstByStatusInOrderByCreatedAtAsc(ACTIVE_STATUSES)
                .map(job -> resolveActiveJob(job, grade, regenerationPrompt, baseVersionId))
                .orElse(null);

        if (activeJob != null) {
            return activeJob;
        }

        MatchingJob job = new MatchingJob(UUID.randomUUID().toString(), grade, regenerationPrompt, baseVersionId);
        return toResponse(matchingJobRepository.save(job));
    }

    private MatchingJobResponseDto resolveActiveJob(
            MatchingJob activeJob,
            Grade requestedGrade,
            String requestedPrompt,
            Long requestedBaseVersionId
    ) {
        boolean sameRequest =
                activeJob.getGrade() == requestedGrade &&
                        Objects.equals(activeJob.getRegenerationPrompt(), requestedPrompt) &&
                        Objects.equals(activeJob.getBaseVersionId(), requestedBaseVersionId);

        if (sameRequest) {
            return toResponse(activeJob);
        }

        throw new IllegalStateException("다른 팀 매칭 작업이 이미 진행 중입니다. 완료 후 다시 시도해주세요.");
    }

    @Transactional(readOnly = true)
    public MatchingJobResponseDto get(String jobId) {
        return toResponse(findJob(jobId));
    }

    @Transactional
    public boolean start(String jobId) {
        return findJob(jobId).start();
    }

    @Transactional
    public boolean beginCompletion(String jobId) {
        // RUNNING -> COMPLETING 전이에 성공한 작업만 추천안을 저장할 수 있습니다.
        return findJob(jobId).beginCompletion();
    }

    @Transactional
    public void succeed(String jobId) {
        findJob(jobId).succeed();
    }

    @Transactional
    public void fail(String jobId, String message) {
        findJob(jobId).fail(message);
    }

    @Transactional
    public MatchingJobResponseDto cancel(String jobId) {
        MatchingJob job = findJob(jobId);
        // 저장 트랜잭션이 시작된 뒤 취소하면 일부 데이터만 반영될 수 있어 차단합니다.
        if (job.getStatus() == MatchingJobStatus.COMPLETING) {
            throw new IllegalStateException("추천안 저장이 시작된 작업은 취소할 수 없습니다.");
        }
        job.cancel();
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public boolean isCancelled(String jobId) {
        return findJob(jobId).getStatus() == MatchingJobStatus.CANCELLED;
    }

    private MatchingJob findJob(String jobId) {
        return matchingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("팀 매칭 작업을 찾을 수 없습니다: " + jobId));
    }

    private MatchingJobResponseDto toResponse(MatchingJob job) {
        Long versionId = teamMatchingVersionRepository.findByJobId(job.getId())
                .map(version -> version.getId())
                .orElse(null);
        return MatchingJobResponseDto.from(job, versionId);
    }
}
