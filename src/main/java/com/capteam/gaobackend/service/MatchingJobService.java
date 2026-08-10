package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.team.MatchingJobResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationDetailResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationRequestDto;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import com.capteam.gaobackend.service.admin.TeamMatchingVersionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchingJobService {

    // 작업 중간과 완료 직후 모두 같은 versionId의 팀 목록을 내려줍니다.
    // 프론트가 짧은 폴링 타이밍을 놓쳐도 해당 작업의 partial/final 결과를 잃지 않게 합니다.
    private final MatchingJobStateService matchingJobStateService;
    private final MatchingJobWorker matchingJobWorker;
    private final AiClient aiClient;
    private final TeamMatchingVersionService teamMatchingVersionService;

    private final AsyncTaskExecutor matchingJobExecutor;

    public MatchingJobService(
            MatchingJobStateService matchingJobStateService,
            MatchingJobWorker matchingJobWorker,
            AiClient aiClient,
            TeamMatchingVersionService teamMatchingVersionService,
            @Qualifier("matchingJobExecutor") AsyncTaskExecutor matchingJobExecutor
    ) {
        this.matchingJobStateService = matchingJobStateService;
        this.matchingJobWorker = matchingJobWorker;
        this.aiClient = aiClient;
        this.teamMatchingVersionService = teamMatchingVersionService;
        this.matchingJobExecutor = matchingJobExecutor;
    }

    public MatchingJobResponseDto start(Grade grade) {
        return start(grade, null);
    }

    public MatchingJobResponseDto start(TeamRecommendationRequestDto request) {
        return start(request.getGrade(), normalizePrompt(request.getRegenerationPrompt()), request.getBaseVersionId());
    }

    public MatchingJobResponseDto start(Grade grade, String regenerationPrompt) {
        return start(grade, regenerationPrompt, null);
    }

    public MatchingJobResponseDto start(Grade grade, String regenerationPrompt, Long baseVersionId) {
        // 작업을 먼저 DB에 등록한 뒤 즉시 jobId를 반환할 수 있도록 비동기로 실행합니다.
        MatchingJobResponseDto job = matchingJobStateService.create(grade, regenerationPrompt, baseVersionId);
        try {
            matchingJobExecutor.execute(() -> matchingJobWorker.run(job.getJobId(), grade, regenerationPrompt));
        } catch (RuntimeException e) {
            matchingJobStateService.fail(job.getJobId(), "팀 매칭 작업 실행 대기열이 가득 찼습니다.");
            throw e;
        }
        return job;
    }

    private String normalizePrompt(String regenerationPrompt) {
        if (regenerationPrompt == null || regenerationPrompt.isBlank()) {
            return null;
        }
        return regenerationPrompt.trim();
    }

    public MatchingJobResponseDto get(String jobId) {
        MatchingJobResponseDto job = matchingJobStateService.get(jobId);

        if (job.getVersionId() == null) {
            return job;
        }

        List<TeamRecommendationDetailResponseDto> partialTeams =
                teamMatchingVersionService.getVersionDetails(job.getVersionId());
        return job.toBuilder().partialTeams(partialTeams).build();
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
