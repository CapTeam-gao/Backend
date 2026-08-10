package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingJobTest {

    @Test
    void 진행단계는_뒤로가지_않는다() {
        MatchingJob job = new MatchingJob("job-id", Grade.GRADE_2);

        job.updateProgressStep(2);
        job.updateProgressStep(1);

        assertThat(job.getProgressStep()).isEqualTo(2);
    }

    @Test
    void runningJobCanBeCancelledBeforePersistenceStarts() {
        MatchingJob job = new MatchingJob("job-id", Grade.GRADE_2);

        assertThat(job.start()).isTrue();
        assertThat(job.cancel()).isTrue();
        assertThat(job.getStatus()).isEqualTo(MatchingJobStatus.CANCELLED);
        assertThat(job.beginCompletion()).isFalse();
    }

    @Test
    void completingJobCannotBeCancelled() {
        MatchingJob job = new MatchingJob("job-id", Grade.GRADE_2);

        job.start();
        assertThat(job.beginCompletion()).isTrue();
        assertThat(job.cancel()).isFalse();
        job.succeed();

        assertThat(job.getStatus()).isEqualTo(MatchingJobStatus.SUCCEEDED);
    }

    @Test
    void cancelledJobIsNotChangedToFailed() {
        MatchingJob job = new MatchingJob("job-id", Grade.GRADE_3);

        job.start();
        job.cancel();
        job.fail("failure");

        assertThat(job.getStatus()).isEqualTo(MatchingJobStatus.CANCELLED);
        assertThat(job.getErrorMessage()).isNull();
    }

    @Test
    void storesRegenerationPromptForJobHistory() {
        MatchingJob job = new MatchingJob("job-id", Grade.GRADE_2, "백엔드 역할을 강화해줘");

        assertThat(job.getRegenerationPrompt()).isEqualTo("백엔드 역할을 강화해줘");
    }

    // 회귀 테스트: AI가 네트워크 재시도 등으로 같은 batch_index를 다시 보내도
    // completedBatches가 중복으로 올라가지 않아야 한다(멱등 처리).
    @Test
    void markBatchReceivedIsIdempotentPerBatchIndex() {
        MatchingJob job = new MatchingJob("job-id", Grade.GRADE_2);

        assertThat(job.markBatchReceived(0)).isTrue();
        assertThat(job.markBatchReceived(0)).isFalse();
        assertThat(job.markBatchReceived(1)).isTrue();
    }
}
