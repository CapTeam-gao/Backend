package com.capteam.gaobackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// AI 서버가 배치 하나를 끝낼 때마다 보내는 내부 콜백 요청입니다.
// POST /internal/matching/jobs/{jobId}/batch-complete
@Getter
@NoArgsConstructor
public class AiMatchingBatchCompleteRequestDto {

    // 지금 보내는 배치가 몇 번째인지(0부터 시작). 로그/디버깅용이고 저장 로직 자체는 순서에 의존하지 않습니다.
    @JsonProperty("batch_index")
    private int batchIndex;

    // 전체 팀 개수. AI가 팀 하나를 보낼 때마다 같은 값을 보내면 됩니다(진행률 계산용).
    @JsonProperty("total_batches")
    private Integer totalBatches;

    // 이번 콜백에서 완료된 팀 하나입니다. 배열 형식은 기존 JSON 호환성을 위해 유지하지만
    // 서버는 여러 팀이 들어오면 거부합니다.
    private List<AiTeamSummaryResponseDto.TeamDto> teams;
}
