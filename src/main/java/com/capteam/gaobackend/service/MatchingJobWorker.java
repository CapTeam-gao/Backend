package com.capteam.gaobackend.service;

import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.exception.MatchingJobCancelledException;
import com.capteam.gaobackend.service.admin.AdminTeamRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingJobWorker {

    private final MatchingJobStateService matchingJobStateService;
    private final AdminTeamRecommendationService adminTeamRecommendationService;

    // HTTP 요청 스레드와 분리된 실행기에서 AI 호출부터 추천안 저장까지 처리합니다.
    public void run(String jobId, Grade grade) {
        if (!matchingJobStateService.start(jobId)) {
            return;
        }

        try {
            adminTeamRecommendationService.createRecommendation(
                    grade,
                    jobId,
                    // 저장 직전 상태 전이를 원자적으로 확인해 취소된 결과의 반영을 막습니다.
                    () -> matchingJobStateService.beginCompletion(jobId)
            );
            matchingJobStateService.succeed(jobId);
        } catch (MatchingJobCancelledException e) {
            // 취소는 정상적인 작업 종료이므로 FAILED 상태로 변경하지 않습니다.
            log.info("팀 매칭 작업이 취소되었습니다. jobId={}", jobId);
        } catch (RuntimeException e) {
            if (!matchingJobStateService.isCancelled(jobId)) {
                matchingJobStateService.fail(jobId, getFailureMessage(e));
                log.error("팀 매칭 작업에 실패했습니다. jobId={}", jobId, e);
            }
        }
    }

    private String getFailureMessage(RuntimeException e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? "팀 매칭 작업에 실패했습니다." : message;
    }
}
