package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTeamMatchingService {

    // 외부 AI 서버에서 팀 요약을 조회할 때 사용하는 Client 필드입니다.
    private final AiClient aiClient;

    // 백엔드 내부 로직으로 팀을 자동 생성하고 요약할 때 사용하는 Service 필드입니다.
    private final AiTeamAutoCreationService aiTeamAutoCreationService;

    // 이미 생성된 팀이 있으면 현재 DB 기준 요약을 반환하고, 없으면 AI 서버 요약을 조회하는 기능입니다.
    public AiTeamSummaryResponseDto getTeamSummary() {
        if (aiTeamAutoCreationService.hasCreatedTeams()) {
            return aiTeamAutoCreationService.summarizeCurrentTeams();
        }

        return aiClient.getTeamSummary();
    }

    // 학생 프로필을 기준으로 기존 팀 데이터를 재생성하고 새 팀 매칭 결과를 반환하는 기능입니다.
    public AiTeamSummaryResponseDto runMatching() {
        return aiTeamAutoCreationService.recreateTeamsByStudentProfiles();
    }
}
