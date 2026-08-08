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

    // 전체 배치 개수. AI가 매 배치마다 같은 값을 보내면 됩니다(진행률 계산용).
    @JsonProperty("total_batches")
    private Integer totalBatches;

    // 이번 배치에서 완료된 팀 목록입니다. /matching/run 최종 응답의 teams와 같은 모양을 그대로 씁니다.
    private List<AiTeamSummaryResponseDto.TeamDto> teams;
}
