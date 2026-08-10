package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.entity.MatchingJob;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.repository.MatchingJobRepository;
import com.capteam.gaobackend.service.admin.AdminTeamMatchingPreparationService;
import com.capteam.gaobackend.service.admin.AdminTeamRecommendationPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingBatchCallbackServiceTest {

    @Mock private MatchingJobRepository matchingJobRepository;
    @Mock private AdminTeamMatchingPreparationService matchingPreparationService;
    @Mock private AdminTeamRecommendationPersistenceService recommendationPersistenceService;

    private MatchingBatchCallbackService callbackService;

    @BeforeEach
    void setUp() {
        callbackService = new MatchingBatchCallbackService(
                matchingJobRepository,
                matchingPreparationService,
                recommendationPersistenceService
        );
    }

    // 회귀 테스트: 같은 batch_index가 재전송(네트워크 재시도 등)되어도
    // completedBatches는 한 번만 올라가야 한다.
    @Test
    void sameBatchIndexArrivingTwiceOnlyIncrementsCompletedBatchesOnce() {
        MatchingJob job = new MatchingJob("job-1", Grade.GRADE_2);
        job.start();
        when(matchingJobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(matchingPreparationService.prepare(Grade.GRADE_2))
                .thenReturn(new AdminTeamMatchingPreparationService.PreparedMatching(Map.of(), List.of()));

        AiTeamSummaryResponseDto.TeamDto team = new AiTeamSummaryResponseDto.TeamDto();
        team.setTeamName("1팀");
        team.setMembers(List.of());

        callbackService.recordBatch("job-1", 0, 3, List.of(team));
        callbackService.recordBatch("job-1", 0, 3, List.of(team));

        assertThat(job.getCompletedBatches()).isEqualTo(1);
        assertThat(job.getTotalBatches()).isEqualTo(3);
        verify(recommendationPersistenceService, times(2))
                .appendBatchTeams(any(), any(), any(), any(), any());
    }

    @Test
    void distinctBatchIndexesEachIncrementCompletedBatches() {
        MatchingJob job = new MatchingJob("job-1", Grade.GRADE_2);
        job.start();
        when(matchingJobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(matchingPreparationService.prepare(Grade.GRADE_2))
                .thenReturn(new AdminTeamMatchingPreparationService.PreparedMatching(Map.of(), List.of()));

        AiTeamSummaryResponseDto.TeamDto team = new AiTeamSummaryResponseDto.TeamDto();
        team.setTeamName("1팀");
        team.setMembers(List.of());

        callbackService.recordBatch("job-1", 0, 3, List.of(team));
        callbackService.recordBatch("job-1", 1, 3, List.of(team));

        assertThat(job.getCompletedBatches()).isEqualTo(2);
    }

    // 취소되었거나 이미 끝난 작업으로 뒤늦게 도착한 배치는 무시해야 한다.
    @Test
    void ignoresBatchForJobThatIsNoLongerAcceptingBatches() {
        MatchingJob job = new MatchingJob("job-1", Grade.GRADE_2);
        job.start();
        job.cancel();
        when(matchingJobRepository.findById("job-1")).thenReturn(Optional.of(job));

        AiTeamSummaryResponseDto.TeamDto team = new AiTeamSummaryResponseDto.TeamDto();
        team.setTeamName("1팀");
        team.setMembers(List.of());

        callbackService.recordBatch("job-1", 0, 3, List.of(team));

        assertThat(job.getCompletedBatches()).isEqualTo(0);
        verify(recommendationPersistenceService, never())
                .appendBatchTeams(any(), any(), any(), any(), any());
    }

    @Test
    void unknownJobIdThrowsClearException() {
        when(matchingJobRepository.findById("missing")).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> callbackService.recordBatch("missing", 0, 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }
}
