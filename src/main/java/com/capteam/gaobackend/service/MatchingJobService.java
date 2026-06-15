package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.team.MatchingJobResponseDto;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class MatchingJobService {

    private final MatchingJobStateService matchingJobStateService;
    private final MatchingJobWorker matchingJobWorker;
    private final AiClient aiClient;

    private final AsyncTaskExecutor matchingJobExecutor;

    public MatchingJobService(
            MatchingJobStateService matchingJobStateService,
            MatchingJobWorker matchingJobWorker,
            AiClient aiClient,
            @Qualifier("matchingJobExecutor") AsyncTaskExecutor matchingJobExecutor
    ) {
        this.matchingJobStateService = matchingJobStateService;
        this.matchingJobWorker = matchingJobWorker;
        this.aiClient = aiClient;
        this.matchingJobExecutor = matchingJobExecutor;
    }

    public MatchingJobResponseDto start(Grade grade) {
        // 작업을 먼저 DB에 등록한 뒤 즉시 jobId를 반환할 수 있도록 비동기로 실행합니다.
        MatchingJobResponseDto job = matchingJobStateService.create(grade);
        try {
            matchingJobExecutor.execute(() -> matchingJobWorker.run(job.getJobId(), grade));
        } catch (RuntimeException e) {
            matchingJobStateService.fail(job.getJobId(), "팀 매칭 작업 실행 대기열이 가득 찼습니다.");
            throw e;
        }
        return job;
    }

    public MatchingJobResponseDto get(String jobId) {
        return matchingJobStateService.get(jobId);
    }

    public MatchingJobResponseDto cancel(String jobId) {
        MatchingJobResponseDto job = matchingJobStateService.cancel(jobId);
        if (job.getStatus() == MatchingJobStatus.CANCELLED) {
            // 백엔드 HTTP 요청과 AI 서버 내부 작업에 동일한 jobId로 취소를 전달합니다.
            aiClient.cancelMatching(jobId);
        }
        return job;
    }
}
