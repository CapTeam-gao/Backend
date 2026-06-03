package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTeamMatchingService {

    private final AiClient aiClient;
    private final AiTeamAutoCreationService aiTeamAutoCreationService;

    public AiTeamSummaryResponseDto getTeamSummary() {
        if (aiTeamAutoCreationService.hasCreatedTeams()) {
            return aiTeamAutoCreationService.summarizeCurrentTeams();
        }

        return aiClient.getTeamSummary();
    }

    public AiTeamSummaryResponseDto runMatching() {
        return aiTeamAutoCreationService.recreateTeamsByStudentProfiles();
    }
}
