package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingJobTest {

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
}
