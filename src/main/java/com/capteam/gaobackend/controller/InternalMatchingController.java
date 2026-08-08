package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.ai.AiMatchingBatchCompleteRequestDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.service.MatchingBatchCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

// AI 서버가 팀 매칭 배치를 하나 끝낼 때마다 호출하는 서버-서버 내부 API입니다.
// 사용자 JWT가 아니라 X-Internal-Api-Key 헤더로 인증하고, SecurityConfig에서 /internal/**을
// permitAll로 열어둔 대신 여기서 직접 키를 검사합니다.
@RestController
@RequestMapping("/internal/matching")
@RequiredArgsConstructor
public class InternalMatchingController {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final MatchingBatchCallbackService matchingBatchCallbackService;

    @Value("${internal.matching-api-key}")
    private String internalMatchingApiKey;

    @PostMapping("/jobs/{jobId}/batch-complete")
    public ResponseEntity<ApiResponse<String>> completeBatch(
            @PathVariable String jobId,
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @RequestBody AiMatchingBatchCompleteRequestDto request
    ) {
        if (!internalMatchingApiKey.equals(apiKey)) {
            throw new AccessDeniedException("내부 API 키가 올바르지 않습니다.");
        }

        matchingBatchCallbackService.recordBatch(
                jobId,
                request.getBatchIndex(),
                request.getTotalBatches(),
                request.getTeams()
        );

        return ApiResponse.ok("배치가 저장되었습니다.");
    }
}
