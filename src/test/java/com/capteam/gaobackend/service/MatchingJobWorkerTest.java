package com.capteam.gaobackend.service;

import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.exception.MatchingJobCancelledException;
import com.capteam.gaobackend.service.admin.AdminTeamRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.BooleanSupplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingJobWorkerTest {

    @Mock
    private MatchingJobStateService matchingJobStateService;

    @Mock
    private AdminTeamRecommendationService adminTeamRecommendationService;

    private MatchingJobWorker matchingJobWorker;

    @BeforeEach
    void setUp() {
        matchingJobWorker = new MatchingJobWorker(matchingJobStateService, adminTeamRecommendationService);
    }

    @Test
    void succeedsOnlyAfterCompletionBoundaryIsAcquired() {
        String jobId = "job-id";
        when(matchingJobStateService.start(jobId)).thenReturn(true);
        when(matchingJobStateService.beginCompletion(jobId)).thenReturn(true);
        doAnswer(invocation -> {
            BooleanSupplier beginCompletion = invocation.getArgument(2);
            beginCompletion.getAsBoolean();
            return null;
        }).when(adminTeamRecommendationService)
                .createRecommendation(eq(Grade.GRADE_2), eq(jobId), any(BooleanSupplier.class));

        matchingJobWorker.run(jobId, Grade.GRADE_2);

        verify(matchingJobStateService).beginCompletion(jobId);
        verify(matchingJobStateService).succeed(jobId);
        verify(matchingJobStateService, never()).fail(eq(jobId), any());
    }

    @Test
    void cancelledJobIsNotMarkedAsFailed() {
        String jobId = "job-id";
        when(matchingJobStateService.start(jobId)).thenReturn(true);
        doThrow(new MatchingJobCancelledException(jobId))
                .when(adminTeamRecommendationService)
                .createRecommendation(eq(Grade.GRADE_2), eq(jobId), any(BooleanSupplier.class));

        matchingJobWorker.run(jobId, Grade.GRADE_2);

        verify(matchingJobStateService, never()).succeed(jobId);
        verify(matchingJobStateService, never()).fail(eq(jobId), any());
    }
}
