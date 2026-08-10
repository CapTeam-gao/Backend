package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.team.TeamMatchingVersionDiffResponseDto;
import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.enums.*;
import com.capteam.gaobackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamMatchingVersionServiceTest {

    @Mock private TeamMatchingVersionRepository teamMatchingVersionRepository;
    @Mock private TeamRecommendationRepository teamRecommendationRepository;
    @Mock private TeamRecommendationMemberRepository teamRecommendationMemberRepository;
    @Mock private TeamRecommendationReasonRepository teamRecommendationReasonRepository;
    @Mock private UserAnalysisRepository userAnalysisRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatChannelRepository chatChannelRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatReadStatusRepository chatReadStatusRepository;
    @Mock private TeamProjectRepository teamProjectRepository;
    @Mock private JournalRepository journalRepository;
    @Mock private TeamAssignmentNoticeService teamAssignmentNoticeService;

    private TeamMatchingVersionService teamMatchingVersionService;

    @BeforeEach
    void setUp() {
        teamMatchingVersionService = new TeamMatchingVersionService(
                teamMatchingVersionRepository,
                teamRecommendationRepository,
                teamRecommendationMemberRepository,
                teamRecommendationReasonRepository,
                userAnalysisRepository,
                teamRepository,
                teamUserRepository,
                chatRoomRepository,
                chatChannelRepository,
                chatMessageRepository,
                chatReadStatusRepository,
                teamProjectRepository,
                journalRepository,
                teamAssignmentNoticeService
        );
    }

    @Test
    void getVersionDiffIncludesMovedStudentAndRoleChange() {
        TeamMatchingVersion fromVersion = version(1L, Grade.GRADE_2, 1, TeamMatchingVersionStatus.APPLIED);
        TeamMatchingVersion toVersion = version(2L, Grade.GRADE_2, 2, TeamMatchingVersionStatus.DRAFT);
        TeamRecommendation fromRecommendation = recommendation(10L, fromVersion, "old-strength");
        TeamRecommendation toRecommendation = recommendation(20L, toVersion, "new-strength");
        TeamRecommendation toRecommendationTwo = recommendation(21L, toVersion, "new-strength-2");

        TeamRecommendationMember fromMember = member(fromRecommendation, student("stu2301", "김진용", true), StudentRole.BACKEND, true);
        TeamRecommendationMember toMovedMember = member(toRecommendationTwo, student("stu2301", "김진용", true), StudentRole.FULLSTACK, false);

        when(teamMatchingVersionRepository.findById(1L)).thenReturn(Optional.of(fromVersion));
        when(teamMatchingVersionRepository.findById(2L)).thenReturn(Optional.of(toVersion));
        when(teamRecommendationRepository.findByMatchingVersionIdOrderByIdAsc(1L)).thenReturn(List.of(fromRecommendation));
        when(teamRecommendationRepository.findByMatchingVersionIdOrderByIdAsc(2L)).thenReturn(List.of(toRecommendation, toRecommendationTwo));
        when(teamRecommendationMemberRepository.findByRecommendationId(10L)).thenReturn(List.of(fromMember));
        when(teamRecommendationMemberRepository.findByRecommendationId(20L)).thenReturn(List.of());
        when(teamRecommendationMemberRepository.findByRecommendationId(21L)).thenReturn(List.of(toMovedMember));

        TeamMatchingVersionDiffResponseDto response = teamMatchingVersionService.getVersionDiff(1L, 2L);

        assertThat(response.getMovedStudents()).hasSize(1);
        assertThat(response.getMovedStudents().get(0).getUserId()).isEqualTo("stu2301");
        assertThat(response.getMovedStudents().get(0).getFromStudentRole()).isEqualTo(StudentRole.BACKEND);
        assertThat(response.getMovedStudents().get(0).getToStudentRole()).isEqualTo(StudentRole.FULLSTACK);
        assertThat(response.getChangedTeamIds()).containsExactlyInAnyOrder("10", "21");
    }

    // 회귀 테스트: recommendationId(추천안 row PK)는 버전마다 새로 발급되므로, 같은 "1팀"에
    // 같은 역할로 남아있어도 row id만 비교하면 항상 "이동"으로 잘못 표시되던 버그가 있었다.
    // fromRecommendation(10L)과 toRecommendation(20L)처럼 id는 다르지만 둘 다 각 버전의
    // 첫 번째 추천안(=1팀)이고 역할도 동일하면 movedStudents에 나타나면 안 된다.
    @Test
    void getVersionDiffTreatsSameTeamPositionAsUnchangedEvenWhenRecommendationIdDiffers() {
        TeamMatchingVersion fromVersion = version(1L, Grade.GRADE_2, 1, TeamMatchingVersionStatus.APPLIED);
        TeamMatchingVersion toVersion = version(2L, Grade.GRADE_2, 2, TeamMatchingVersionStatus.DRAFT);
        TeamRecommendation fromRecommendation = recommendation(10L, fromVersion, "old-strength");
        TeamRecommendation toRecommendation = recommendation(20L, toVersion, "new-strength");

        TeamRecommendationMember fromMember = member(fromRecommendation, student("stu2301", "김진용", true), StudentRole.BACKEND, true);
        TeamRecommendationMember toMember = member(toRecommendation, student("stu2301", "김진용", true), StudentRole.BACKEND, true);

        when(teamMatchingVersionRepository.findById(1L)).thenReturn(Optional.of(fromVersion));
        when(teamMatchingVersionRepository.findById(2L)).thenReturn(Optional.of(toVersion));
        when(teamRecommendationRepository.findByMatchingVersionIdOrderByIdAsc(1L)).thenReturn(List.of(fromRecommendation));
        when(teamRecommendationRepository.findByMatchingVersionIdOrderByIdAsc(2L)).thenReturn(List.of(toRecommendation));
        when(teamRecommendationMemberRepository.findByRecommendationId(10L)).thenReturn(List.of(fromMember));
        when(teamRecommendationMemberRepository.findByRecommendationId(20L)).thenReturn(List.of(toMember));

        TeamMatchingVersionDiffResponseDto response = teamMatchingVersionService.getVersionDiff(1L, 2L);

        assertThat(response.getMovedStudents()).isEmpty();
        assertThat(response.getChangedTeamIds()).isEmpty();
    }

    @Test
    void getVersionDiffRejectsUnknownVersionId() {
        when(teamMatchingVersionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamMatchingVersionService.getVersionDiff(999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("팀 추천 버전을 찾을 수 없습니다.");
    }

    @Test
    void applyVersionCreatesApprovedTeamsAndDiscardsOthers() {
        TeamMatchingVersion oldVersion = version(1L, Grade.GRADE_2, 1, TeamMatchingVersionStatus.APPLIED);
        TeamMatchingVersion targetVersion = version(2L, Grade.GRADE_2, 2, TeamMatchingVersionStatus.DRAFT);
        TeamRecommendation recommendation = recommendation(10L, targetVersion, "강점");
        User user = student("stu2301", "김진용", true);
        TeamRecommendationMember recommendedMember = member(recommendation, user, StudentRole.BACKEND, true);

        when(teamMatchingVersionRepository.findById(2L)).thenReturn(Optional.of(targetVersion));
        when(teamRecommendationRepository.findByMatchingVersionIdOrderByIdAsc(2L)).thenReturn(List.of(recommendation));
        when(teamRepository.findByGrade(Grade.GRADE_2)).thenReturn(List.of());
        when(teamRecommendationMemberRepository.findByRecommendationId(10L)).thenReturn(List.of(recommendedMember));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            ReflectionTestUtils.setField(team, "id", 100L);
            return team;
        });
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom chatRoom = invocation.getArgument(0);
            ReflectionTestUtils.setField(chatRoom, "id", 200L);
            return chatRoom;
        });
        when(teamMatchingVersionRepository.findByGradeOrderByVersionNumberDesc(Grade.GRADE_2))
                .thenReturn(List.of(targetVersion, oldVersion));

        var response = teamMatchingVersionService.applyVersion(2L);

        assertThat(response.getStatus()).isEqualTo(TeamMatchingVersionStatus.APPLIED);
        assertThat(targetVersion.getStatus()).isEqualTo(TeamMatchingVersionStatus.APPLIED);
        assertThat(oldVersion.getStatus()).isEqualTo(TeamMatchingVersionStatus.DISCARDED);
        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.ACCEPTED);
        verify(teamRepository).save(any(Team.class));
        verify(teamUserRepository).save(any(TeamUser.class));
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatChannelRepository).save(any(ChatChannel.class));
        verify(teamAssignmentNoticeService).createNotice(Grade.GRADE_2);
    }

    @Test
    void discardVersionRejectsAppliedVersion() {
        TeamMatchingVersion appliedVersion = version(2L, Grade.GRADE_2, 2, TeamMatchingVersionStatus.APPLIED);
        when(teamMatchingVersionRepository.findById(2L)).thenReturn(Optional.of(appliedVersion));

        assertThatThrownBy(() -> teamMatchingVersionService.discardVersion(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("현재 적용 중인 버전은 폐기할 수 없습니다.");
    }

    private TeamMatchingVersion version(Long id, Grade grade, int versionNumber, TeamMatchingVersionStatus status) {
        TeamMatchingVersion version = TeamMatchingVersion.builder()
                .grade(grade)
                .versionNumber(versionNumber)
                .jobId("job-" + versionNumber)
                .regenerationPrompt("prompt-" + versionNumber)
                .build();
        ReflectionTestUtils.setField(version, "id", id);
        ReflectionTestUtils.setField(version, "status", status);
        return version;
    }

    private TeamRecommendation recommendation(Long id, TeamMatchingVersion version, String strengths) {
        TeamRecommendation recommendation = TeamRecommendation.builder()
                .matchingVersion(version)
                .grade(version.getGrade())
                .strengths(strengths)
                .weaknesses(null)
                .build();
        ReflectionTestUtils.setField(recommendation, "id", id);
        return recommendation;
    }

    private TeamRecommendationMember member(
            TeamRecommendation recommendation,
            User user,
            StudentRole studentRole,
            boolean leader
    ) {
        return TeamRecommendationMember.builder()
                .recommendation(recommendation)
                .user(user)
                .studentRole(studentRole)
                .isRecommendedLeader(leader)
                .build();
    }

    private User student(String userId, String name, boolean surveyCompleted) {
        User user = User.builder()
                .userId(userId)
                .name(name)
                .accountRole(AccountRole.STUDENT)
                .grade(Grade.GRADE_2)
                .build();
        ReflectionTestUtils.setField(user, "surveyCompleted", surveyCompleted);
        return user;
    }
}
