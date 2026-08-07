package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.entity.MatchingJob;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MatchingJobResponseDto {

    // 프론트가 상태 조회와 취소 요청에 사용하는 작업 식별자입니다.
    private String jobId;
    private Grade grade;
    private MatchingJobStatus status;
    private String errorMessage;
    private String regenerationPrompt;
    private Long versionId;
    private Long baseVersionId;
    private String origin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MatchingJobResponseDto from(MatchingJob job) {
        return from(job, null);
    }

    public static MatchingJobResponseDto from(MatchingJob job, Long versionId) {
        return MatchingJobResponseDto.builder()
                .jobId(job.getId())
                .grade(job.getGrade())
                .status(job.getStatus())
                .errorMessage(job.getErrorMessage())
                .regenerationPrompt(job.getRegenerationPrompt())
                .versionId(versionId)
                .baseVersionId(job.getBaseVersionId())
                .origin(resolveOrigin(job))
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private static String resolveOrigin(MatchingJob job) {
        if (job.getBaseVersionId() != null) {
            return "REGENERATION";
        }
        if (job.getRegenerationPrompt() != null && !job.getRegenerationPrompt().isBlank()) {
            return "REGENERATION";
        }
        return "INITIAL";
    }
}
