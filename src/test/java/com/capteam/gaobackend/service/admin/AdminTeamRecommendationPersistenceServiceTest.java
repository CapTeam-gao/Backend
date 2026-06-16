package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.repository.TeamRecommendationMemberRepository;
import com.capteam.gaobackend.repository.TeamRecommendationReasonRepository;
import com.capteam.gaobackend.repository.TeamRecommendationRepository;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTeamRecommendationPersistenceServiceTest {

    @Mock private TeamRecommendationRepository recommendationRepository;
    @Mock private TeamRecommendationMemberRepository recommendationMemberRepository;
    @Mock private TeamRecommendationReasonRepository recommendationReasonRepository;
    @Mock private UserAnalysisRepository userAnalysisRepository;
    @Mock private UserRepository userRepository;

    private AdminTeamRecommendationPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new AdminTeamRecommendationPersistenceService(
                recommendationRepository,
                recommendationMemberRepository,
                recommendationReasonRepository,
                userAnalysisRepository,
                userRepository
        );
    }

    @Test
    void storesMemberStrengthAsAnalysisResultInsteadOfSkillLevel() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        AiTeamSummaryResponseDto.MemberDto member = new AiTeamSummaryResponseDto.MemberDto();
        member.setName("홍길동");
        member.setRoleGroup("backend");
        member.setSkillLevel("중");
        member.setStrength("백엔드 구현 경험이 풍부하고 협업이 안정적입니다.");
        AiTeamSummaryResponseDto.TeamDto team = new AiTeamSummaryResponseDto.TeamDto();
        team.setMembers(List.of(member));
        team.setLeader("홍길동");
        team.setStrengths("프론트엔드와 백엔드 역할이 균형 있게 구성되었습니다.");
        team.setWeaknesses("AI 역할 인원이 적어 분석 로직이 특정 학생에게 집중될 수 있습니다.");

        when(userRepository.findAllById(any())).thenReturn(List.of(user));
        when(recommendationRepository.findByGradeAndStatus(any(), any())).thenReturn(List.of());
        when(recommendationRepository.save(any(TeamRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userAnalysisRepository.findById("stu2301")).thenReturn(Optional.empty());

        persistenceService.replacePendingRecommendations(
                Grade.GRADE_2,
                Map.of("홍길동", "stu2301"),
                List.of(team)
        );

        ArgumentCaptor<TeamRecommendation> recommendationCaptor = ArgumentCaptor.forClass(TeamRecommendation.class);
        verify(recommendationRepository).save(recommendationCaptor.capture());
        assertThat(recommendationCaptor.getValue().getStrengths())
                .isEqualTo("프론트엔드와 백엔드 역할이 균형 있게 구성되었습니다.");
        assertThat(recommendationCaptor.getValue().getWeaknesses())
                .isEqualTo("AI 역할 인원이 적어 분석 로직이 특정 학생에게 집중될 수 있습니다.");

        ArgumentCaptor<UserAnalysis> analysisCaptor = ArgumentCaptor.forClass(UserAnalysis.class);
        verify(userAnalysisRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getAnalysisResult())
                .isEqualTo("백엔드 구현 경험이 풍부하고 협업이 안정적입니다.");
        assertThat(analysisCaptor.getValue().getStudentLevel()).isEqualTo(StudentLevel.MIDDLE);
    }
}
