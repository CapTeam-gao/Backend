package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationRequestDto;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.repository.ChatChannelRepository;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import com.capteam.gaobackend.repository.TeamRecommendationMemberRepository;
import com.capteam.gaobackend.repository.TeamRecommendationReasonRepository;
import com.capteam.gaobackend.repository.TeamRecommendationRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTeamRecommendationServiceTest {

    @Mock private AiClient aiClient;
    @Mock private TeamRecommendationRepository recommendationRepository;
    @Mock private TeamRecommendationMemberRepository recommendationMemberRepository;
    @Mock private TeamRecommendationReasonRepository recommendationReasonRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private UserAnalysisRepository userAnalysisRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatChannelRepository chatChannelRepository;
    @Mock private AdminTeamRecommendationPersistenceService recommendationPersistenceService;
    @Mock private AdminTeamMatchingPreparationService matchingPreparationService;

    private AdminTeamRecommendationService adminTeamRecommendationService;

    @BeforeEach
    void setUp() {
        adminTeamRecommendationService = new AdminTeamRecommendationService(
                aiClient,
                recommendationRepository,
                recommendationMemberRepository,
                recommendationReasonRepository,
                teamRepository,
                teamUserRepository,
                userAnalysisRepository,
                chatRoomRepository,
                chatChannelRepository,
                recommendationPersistenceService,
                matchingPreparationService
        );
    }

    @Test
    void passesRegenerationPromptToAiClient() {
        AiStudentPayloadDto studentPayload = AiStudentPayloadDto.builder()
                .name("홍길동")
                .build();
        List<AiStudentPayloadDto> studentPayloads = List.of(studentPayload);
        AiTeamSummaryResponseDto.MemberDto member = new AiTeamSummaryResponseDto.MemberDto();
        member.setName("홍길동");
        AiTeamSummaryResponseDto.TeamDto team = new AiTeamSummaryResponseDto.TeamDto();
        team.setMembers(List.of(member));
        AiTeamSummaryResponseDto aiResponse = new AiTeamSummaryResponseDto();
        aiResponse.setTeams(List.of(team));
        String prompt = "백엔드 역할을 강화해줘";

        when(matchingPreparationService.prepare(Grade.GRADE_2))
                .thenReturn(new AdminTeamMatchingPreparationService.PreparedMatching(
                        Map.of("홍길동", "stu2301"),
                        studentPayloads
                ));
        when(aiClient.runMatchingWithPrompt(studentPayloads, prompt)).thenReturn(aiResponse);

        adminTeamRecommendationService.createRecommendation(
                new TeamRecommendationRequestDto(Grade.GRADE_2, "  " + prompt + "  ")
        );

        verify(aiClient).runMatchingWithPrompt(studentPayloads, prompt);
        verify(recommendationPersistenceService)
                .replacePendingRecommendations(Grade.GRADE_2, Map.of("홍길동", "stu2301"), List.of(team));
    }
}
