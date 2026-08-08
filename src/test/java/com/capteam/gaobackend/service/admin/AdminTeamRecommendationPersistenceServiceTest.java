package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.entity.TeamMatchingVersion;
import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.entity.TeamRecommendationMember;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.TeamMatchingVersionRepository;
import com.capteam.gaobackend.repository.TeamRecommendationMemberRepository;
import com.capteam.gaobackend.repository.TeamRecommendationReasonRepository;
import com.capteam.gaobackend.repository.TeamRecommendationRepository;
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

    @Mock private TeamMatchingVersionRepository teamMatchingVersionRepository;
    @Mock private TeamRecommendationRepository recommendationRepository;
    @Mock private TeamRecommendationMemberRepository recommendationMemberRepository;
    @Mock private TeamRecommendationReasonRepository recommendationReasonRepository;
    @Mock private UserRepository userRepository;

    private AdminTeamRecommendationPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new AdminTeamRecommendationPersistenceService(
                teamMatchingVersionRepository,
                recommendationRepository,
                recommendationMemberRepository,
                recommendationReasonRepository,
                userRepository
        );
    }

    @Test
    void storesTeamStrengthsAndWeaknesses() {
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
        when(teamMatchingVersionRepository.findFirstByGradeOrderByVersionNumberDesc(any())).thenReturn(Optional.empty());
        when(teamMatchingVersionRepository.save(any(TeamMatchingVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.save(any(TeamRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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
        assertThat(recommendationCaptor.getValue().getMatchingVersion()).isNotNull();
        assertThat(recommendationCaptor.getValue().getMatchingVersion().getVersionNumber()).isEqualTo(1);
    }

    @Test
    void storesMemberByUserIdWhenAiNameDoesNotMatchBackendName() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        AiTeamSummaryResponseDto.MemberDto member = new AiTeamSummaryResponseDto.MemberDto();
        member.setUserId("stu2301");
        member.setName("잘못된 이름");
        member.setRoleGroup("backend");
        member.setSkillLevel("상");
        member.setStrength("백엔드 구현 강점");
        AiTeamSummaryResponseDto.TeamDto team = new AiTeamSummaryResponseDto.TeamDto();
        team.setMembers(List.of(member));
        team.setLeader("홍길동");

        when(userRepository.findAllById(any())).thenReturn(List.of(user));
        when(teamMatchingVersionRepository.findFirstByGradeOrderByVersionNumberDesc(any())).thenReturn(Optional.empty());
        when(teamMatchingVersionRepository.save(any(TeamMatchingVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.save(any(TeamRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        persistenceService.replacePendingRecommendations(
                Grade.GRADE_2,
                Map.of("홍길동", "stu2301"),
                List.of(team)
        );

        ArgumentCaptor<TeamRecommendationMember> memberCaptor =
                ArgumentCaptor.forClass(TeamRecommendationMember.class);
        verify(recommendationMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getUser().getUserId()).isEqualTo("stu2301");
        assertThat(memberCaptor.getValue().isRecommendedLeader()).isTrue();
    }

    @Test
    void storesSpecializedAiRolesWithoutBackendFallback() {
        User fullstackUser = user("stu2301", "김풀스택");
        User devopsUser = user("stu2302", "박데브옵스");
        User securityUser = user("stu2303", "이보안");
        User gameUser = user("stu2304", "최게임");

        AiTeamSummaryResponseDto.TeamDto team = new AiTeamSummaryResponseDto.TeamDto();
        team.setMembers(List.of(
                member("김풀스택", "fullstack", "백엔드와 프론트엔드를 함께 구현"),
                member("박데브옵스", "backend", "devops 배포 자동화 담당"),
                member("이보안", null, "security 취약점 점검 담당"),
                member("최게임", "game", "게임 클라이언트 구현")
        ));
        team.setLeader("김풀스택");

        when(userRepository.findAllById(any()))
                .thenReturn(List.of(fullstackUser, devopsUser, securityUser, gameUser));
        when(teamMatchingVersionRepository.findFirstByGradeOrderByVersionNumberDesc(any())).thenReturn(Optional.empty());
        when(teamMatchingVersionRepository.save(any(TeamMatchingVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.save(any(TeamRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        persistenceService.replacePendingRecommendations(
                Grade.GRADE_2,
                Map.of(
                        "김풀스택", "stu2301",
                        "박데브옵스", "stu2302",
                        "이보안", "stu2303",
                        "최게임", "stu2304"
                ),
                List.of(team)
        );

        ArgumentCaptor<TeamRecommendationMember> memberCaptor =
                ArgumentCaptor.forClass(TeamRecommendationMember.class);
        verify(recommendationMemberRepository, org.mockito.Mockito.times(4)).save(memberCaptor.capture());

        assertThat(memberCaptor.getAllValues())
                .extracting(TeamRecommendationMember::getStudentRole)
                .containsExactly(
                        StudentRole.FULLSTACK,
                        StudentRole.DEVOPS,
                        StudentRole.SECURITY,
                        StudentRole.GAME
                );
    }

    private User user(String userId, String name) {
        return User.builder()
                .userId(userId)
                .name(name)
                .accountRole(AccountRole.STUDENT)
                .build();
    }

    private AiTeamSummaryResponseDto.MemberDto member(String name, String roleGroup, String role) {
        AiTeamSummaryResponseDto.MemberDto member = new AiTeamSummaryResponseDto.MemberDto();
        member.setName(name);
        member.setRoleGroup(roleGroup);
        member.setRole(role);
        member.setSkillLevel("중");
        member.setStrength(name + " 강점");
        return member;
    }
}
