package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.team.MatchingJobResponseDto;
import com.capteam.gaobackend.entity.MatchingJob;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import com.capteam.gaobackend.repository.MatchingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingJobStateService {

    private final MatchingJobRepository matchingJobRepository;

    // 같은 학년의 중복 매칭 실행으로 추천안이 서로 덮어쓰이는 것을 방지합니다.
    @Transactional
    public synchronized MatchingJobResponseDto create(Grade grade) {
        boolean activeJobExists = matchingJobRepository.existsByGradeAndStatusIn(
                grade,
                List.of(MatchingJobStatus.QUEUED, MatchingJobStatus.RUNNING, MatchingJobStatus.COMPLETING)
        );
        if (activeJobExists) {
            throw new IllegalStateException("해당 학년의 팀 매칭 작업이 이미 진행 중입니다.");
        }
        MatchingJob job = new MatchingJob(UUID.randomUUID().toString(), grade);
        return MatchingJobResponseDto.from(matchingJobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public MatchingJobResponseDto get(String jobId) {
        return MatchingJobResponseDto.from(findJob(jobId));
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
        return MatchingJobResponseDto.from(job);
    }

    @Transactional(readOnly = true)
    public boolean isCancelled(String jobId) {
        return findJob(jobId).getStatus() == MatchingJobStatus.CANCELLED;
    }

    private MatchingJob findJob(String jobId) {
        return matchingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("팀 매칭 작업을 찾을 수 없습니다: " + jobId));
    }
}
