package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationRequestDto;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.entity.TeamRecommendationMember;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserDevelopmentScore;
import com.capteam.gaobackend.entity.UserPersonalityScore;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock private TeamAssignmentNoticeService teamAssignmentNoticeService;

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
                matchingPreparationService,
                teamAssignmentNoticeService
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
        when(aiClient.runMatchingForGrade(studentPayloads, Grade.GRADE_2, prompt)).thenReturn(aiResponse);

        adminTeamRecommendationService.createRecommendation(
                new TeamRecommendationRequestDto(Grade.GRADE_2, "  " + prompt + "  ", null)
        );

        verify(aiClient).runMatchingForGrade(studentPayloads, Grade.GRADE_2, prompt);
        verify(recommendationPersistenceService)
                .replacePendingRecommendations(Grade.GRADE_2, Map.of("홍길동", "stu2301"), List.of(team));
    }

    @Test
    void matchesAiMemberByUserIdWhenNameIsDifferent() {
        AiStudentPayloadDto studentPayload = AiStudentPayloadDto.builder()
                .userId("stu2301")
                .name("홍길동")
                .build();
        List<AiStudentPayloadDto> studentPayloads = List.of(studentPayload);
        AiTeamSummaryResponseDto.MemberDto member = new AiTeamSummaryResponseDto.MemberDto();
        member.setUserId("stu2301");
        member.setName("AI가 바꾼 이름");
        AiTeamSummaryResponseDto.TeamDto team = new AiTeamSummaryResponseDto.TeamDto();
        team.setMembers(List.of(member));
        AiTeamSummaryResponseDto aiResponse = new AiTeamSummaryResponseDto();
        aiResponse.setTeams(List.of(team));

        when(matchingPreparationService.prepare(Grade.GRADE_2))
                .thenReturn(new AdminTeamMatchingPreparationService.PreparedMatching(
                        Map.of("홍길동", "stu2301"),
                        studentPayloads
                ));
        when(aiClient.runMatchingForGrade(studentPayloads, Grade.GRADE_2, null)).thenReturn(aiResponse);

        adminTeamRecommendationService.createRecommendation(
                new TeamRecommendationRequestDto(Grade.GRADE_2, null, null)
        );

        verify(recommendationPersistenceService)
                .replacePendingRecommendations(Grade.GRADE_2, Map.of("홍길동", "stu2301"), List.of(team));
    }

    @Test
    void copiesRecommendationStrengthsAndWeaknessesToAcceptedTeam() {
        TeamRecommendation recommendation = TeamRecommendation.builder()
                .grade(Grade.GRADE_2)
                .strengths("프론트엔드와 백엔드 역할이 균형 있게 구성되었습니다.")
                .weaknesses("AI 역할 인원이 적어 분석 로직이 특정 학생에게 집중될 수 있습니다.")
                .build();
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        user.completeSurvey(
                StudentRole.DEVOPS,
                List.of("Java"),
                List.of("Spring 프로젝트"),
                false,
                List.of(),
                new UserPersonalityScore(3.0, 3.0, 3.0, 3.0, 3.0),
                new UserDevelopmentScore(3.0, 3.0, 3.0, 3.0, 3.0)
        );
        TeamRecommendationMember member = TeamRecommendationMember.builder()
                .recommendation(recommendation)
                .user(user)
                .studentRole(StudentRole.DEVOPS)
                .isRecommendedLeader(true)
                .build();

        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));
        when(recommendationMemberRepository.findByRecommendationId(1L)).thenReturn(List.of(member));
        when(teamRepository.countByGrade(Grade.GRADE_2)).thenReturn(1L);
        when(chatRoomRepository.findByTeamId(any())).thenReturn(Optional.empty());

        adminTeamRecommendationService.acceptRecommendation(1L);

        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(teamCaptor.capture());
        assertThat(teamCaptor.getValue().getTeamName()).isEqualTo("2팀");
        assertThat(teamCaptor.getValue().getStrengths())
                .isEqualTo("프론트엔드와 백엔드 역할이 균형 있게 구성되었습니다.");
        assertThat(teamCaptor.getValue().getWeaknesses())
                .isEqualTo("AI 역할 인원이 적어 분석 로직이 특정 학생에게 집중될 수 있습니다.");

        ArgumentCaptor<TeamUser> teamUserCaptor = ArgumentCaptor.forClass(TeamUser.class);
        verify(teamUserRepository).save(teamUserCaptor.capture());
        assertThat(teamUserCaptor.getValue().getStudentRole()).isEqualTo(StudentRole.DEVOPS);
        verify(teamAssignmentNoticeService).createNotice(Grade.GRADE_2);
    }

    @Test
    void createsOneNoticeAfterAcceptingAllRecommendationsByGrade() {
        TeamRecommendation recommendation = TeamRecommendation.builder()
                .grade(Grade.GRADE_3)
                .build();
        ReflectionTestUtils.setField(recommendation, "id", 10L);
        User user = User.builder()
                .userId("stu3301")
                .name("김학생")
                .accountRole(AccountRole.STUDENT)
                .build();
        user.completeSurvey(
                StudentRole.APP,
                List.of("Flutter"),
                List.of("앱 프로젝트"),
                false,
                List.of(),
                new UserPersonalityScore(3.0, 3.0, 3.0, 3.0, 3.0),
                new UserDevelopmentScore(3.0, 3.0, 3.0, 3.0, 3.0)
        );
        TeamRecommendationMember member = TeamRecommendationMember.builder()
                .recommendation(recommendation)
                .user(user)
                .studentRole(StudentRole.APP)
                .isRecommendedLeader(true)
                .build();

        when(recommendationRepository.findByGrade(Grade.GRADE_3)).thenReturn(List.of(recommendation));
        when(recommendationRepository.findById(10L)).thenReturn(Optional.of(recommendation));
        when(recommendationMemberRepository.findByRecommendationId(10L)).thenReturn(List.of(member));
        when(teamRepository.countByGrade(Grade.GRADE_3)).thenReturn(0L);
        when(chatRoomRepository.findByTeamId(any())).thenReturn(Optional.empty());

        adminTeamRecommendationService.acceptAllByGrade(Grade.GRADE_3);

        verify(teamAssignmentNoticeService).createNotice(Grade.GRADE_3);
    }
}
