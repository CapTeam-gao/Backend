package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTeamMatchingService {

    // 외부 AI 서버에서 팀 요약 및 매칭을 요청할 때 사용하는 Client 필드입니다.
    private final AiClient aiClient;

    // 외부 AI 서버에서 현재 팀 요약을 조회하는 기능입니다.
    public AiTeamSummaryResponseDto getTeamSummary() {
        return aiClient.getTeamSummary();
    }

    // 외부 AI 서버에 팀 매칭 실행을 요청하는 기능입니다.
    public AiTeamSummaryResponseDto runMatching() {
        return aiClient.runMatching();
    }
}
