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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    // 회귀 테스트: AI가 같은 팀을 team_update → team_ready처럼 두 번 보내도(배치 스트리밍),
    // TeamRecommendation row가 두 개로 늘어나지 않고 기존 row가 최신 내용으로 갱신되어야 한다.
    @Test
    void appendBatchTeamsUpsertsSameTeamInsteadOfDuplicating() {
        TeamMatchingVersion version = TeamMatchingVersion.builder()
                .grade(Grade.GRADE_2)
                .versionNumber(5)
                .jobId("job-1")
                .build();
        ReflectionTestUtils.setField(version, "id", 1L);
        User user = user("stu2301", "홍길동");

        when(teamMatchingVersionRepository.findByJobId("job-1")).thenReturn(Optional.of(version));
        when(userRepository.findAllById(any())).thenReturn(List.of(user));
        when(recommendationRepository.findByMatchingVersionIdAndAiTeamName(1L, "1팀"))
                .thenReturn(Optional.empty());
        when(recommendationRepository.save(any(TeamRecommendation.class)))
                .thenAnswer(invocation -> {
                    TeamRecommendation recommendation = invocation.getArgument(0);
                    ReflectionTestUtils.setField(recommendation, "id", 100L);
                    return recommendation;
                });

        AiTeamSummaryResponseDto.TeamDto firstBatch = new AiTeamSummaryResponseDto.TeamDto();
        firstBatch.setTeamName("1팀");
        firstBatch.setStrengths("초기 강점");
        firstBatch.setMembers(List.of(member("홍길동", "backend", "구현 강점")));
        firstBatch.setLeader("홍길동");

        persistenceService.appendBatchTeams(
                Grade.GRADE_2, "job-1", null, Map.of("홍길동", "stu2301"), List.of(firstBatch));

        ArgumentCaptor<TeamRecommendation> savedCaptor = ArgumentCaptor.forClass(TeamRecommendation.class);
        verify(recommendationRepository, times(1)).save(savedCaptor.capture());
        TeamRecommendation created = savedCaptor.getValue();

        // team_ready로 같은 팀이 더 완성된 내용으로 다시 도착
        when(recommendationRepository.findByMatchingVersionIdAndAiTeamName(1L, "1팀"))
                .thenReturn(Optional.of(created));

        AiTeamSummaryResponseDto.TeamDto secondBatch = new AiTeamSummaryResponseDto.TeamDto();
        secondBatch.setTeamName("1팀");
        secondBatch.setStrengths("최종 강점");
        secondBatch.setMembers(List.of(member("홍길동", "backend", "구현 강점")));
        secondBatch.setLeader("홍길동");

        persistenceService.appendBatchTeams(
                Grade.GRADE_2, "job-1", null, Map.of("홍길동", "stu2301"), List.of(secondBatch));

        // 새 row가 또 생기지 않고(save 여전히 1번), 기존 row 내용만 갱신됨
        verify(recommendationRepository, times(1)).save(any(TeamRecommendation.class));
        assertThat(created.getStrengths()).isEqualTo("최종 강점");
        verify(recommendationMemberRepository).deleteByRecommendationId(100L);
        verify(recommendationReasonRepository).deleteByRecommendationId(100L);
    }

    // 회귀 테스트: 배치 스트리밍이 만들어둔 버전을 최종 저장 시점에 재사용하고, 그 버전에
    // 남아있던 임시 결과(batch leftover)를 지운 뒤 최종 결과로 다시 채워야 한다.
    // (재사용하지 않고 새 버전을 또 만들면 같은 job에 버전이 두 개 남는다.)
    @Test
    void replacePendingRecommendationsReusesJobVersionAndClearsBatchLeftovers() {
        TeamMatchingVersion existingVersion = TeamMatchingVersion.builder()
                .grade(Grade.GRADE_2)
                .versionNumber(5)
                .jobId("job-1")
                .build();
        ReflectionTestUtils.setField(existingVersion, "id", 1L);

        TeamRecommendation leftoverFromStreaming = TeamRecommendation.builder()
                .matchingVersion(existingVersion)
                .grade(Grade.GRADE_2)
                .aiTeamName("1팀")
                .build();
        ReflectionTestUtils.setField(leftoverFromStreaming, "id", 100L);

        User user = user("stu2301", "홍길동");

        when(teamMatchingVersionRepository.findByJobId("job-1")).thenReturn(Optional.of(existingVersion));
        when(recommendationRepository.findByMatchingVersionId(1L)).thenReturn(List.of(leftoverFromStreaming));
        when(userRepository.findAllById(any())).thenReturn(List.of(user));
        when(recommendationRepository.save(any(TeamRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiTeamSummaryResponseDto.TeamDto finalTeam = new AiTeamSummaryResponseDto.TeamDto();
        finalTeam.setTeamName("1팀");
        finalTeam.setStrengths("최종 결과");
        finalTeam.setMembers(List.of(member("홍길동", "backend", "구현 강점")));
        finalTeam.setLeader("홍길동");

        persistenceService.replacePendingRecommendations(
                Grade.GRADE_2, Map.of("홍길동", "stu2301"), List.of(finalTeam), "job-1", null);

        // 새 버전을 만들지 않고 배치가 쓰던 버전을 그대로 재사용해야 한다.
        verify(teamMatchingVersionRepository, never()).save(any(TeamMatchingVersion.class));
        // 스트리밍 중 남아있던 임시 결과는 지워져야 한다.
        verify(recommendationMemberRepository).deleteByRecommendationId(100L);
        verify(recommendationReasonRepository).deleteByRecommendationId(100L);
        verify(recommendationRepository).deleteAll(List.of(leftoverFromStreaming));
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
