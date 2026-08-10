package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.team.ManualTeamRecommendationRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationResponseDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualTeamRecommendationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private AdminTeamRecommendationPersistenceService recommendationPersistenceService;
    @Mock private AdminTeamRecommendationService adminTeamRecommendationService;

    private ManualTeamRecommendationService manualTeamRecommendationService;

    @BeforeEach
    void setUp() {
        manualTeamRecommendationService = new ManualTeamRecommendationService(
                userRepository,
                teamUserRepository,
                recommendationPersistenceService,
                adminTeamRecommendationService
        );
    }

    @Test
    void createsManualRecommendationsAfterValidation() {
        when(teamUserRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, Grade.GRADE_2))
                .thenReturn(List.of(
                        student("stu2301", "장준민", Grade.GRADE_2, StudentRole.FRONTEND),
                        student("stu2302", "위재성", Grade.GRADE_2, StudentRole.BACKEND)
                ));
        ManualTeamRecommendationRequestDto request = request(
                Grade.GRADE_2,
                List.of(team(1, List.of(
                        member("stu2301", StudentRole.FRONTEND, true),
                        member("stu2302", StudentRole.BACKEND, false)
                )))
        );
        when(recommendationPersistenceService.replacePendingManualRecommendations(
                eq(Grade.GRADE_2), any(), any()
        )).thenReturn(List.of(TeamRecommendationResponseDto.builder().id(10L).build()));

        manualTeamRecommendationService.createManualRecommendations(request);

        verify(recommendationPersistenceService).replacePendingManualRecommendations(
                eq(Grade.GRADE_2),
                any(),
                any()
        );
    }

    @Test
    void createsManualRecommendationsFromFrontendPayload() {
        when(teamUserRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, Grade.GRADE_2))
                .thenReturn(List.of(
                        student("stu2301", "장준민", Grade.GRADE_2, StudentRole.FRONTEND),
                        student("stu2302", "위재성", Grade.GRADE_2, StudentRole.BACKEND)
                ));
        ManualTeamRecommendationRequestDto request = request(
                Grade.GRADE_2,
                List.of(frontendTeam("1팀", List.of("stu2301", "stu2302"), "stu2301"))
        );

        manualTeamRecommendationService.createManualRecommendations(request);

        verify(recommendationPersistenceService).replacePendingManualRecommendations(
                eq(Grade.GRADE_2),
                any(),
                any()
        );
    }

    @Test
    void createsAndAcceptsManualTeamsFromFrontendPayload() {
        when(teamUserRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, Grade.GRADE_2))
                .thenReturn(List.of(
                        student("stu2301", "장준민", Grade.GRADE_2, StudentRole.FRONTEND),
                        student("stu2302", "위재성", Grade.GRADE_2, StudentRole.BACKEND)
                ));
        ManualTeamRecommendationRequestDto request = request(
                Grade.GRADE_2,
                List.of(frontendTeam("1팀", List.of("stu2301", "stu2302"), "stu2301"))
        );
        when(recommendationPersistenceService.replacePendingManualRecommendations(
                eq(Grade.GRADE_2), any(), any()
        )).thenReturn(List.of(TeamRecommendationResponseDto.builder().id(10L).build()));

        manualTeamRecommendationService.createAndAcceptManualTeams(request);

        verify(recommendationPersistenceService).replacePendingManualRecommendations(
                eq(Grade.GRADE_2),
                any(),
                any()
        );
        verify(adminTeamRecommendationService).acceptRecommendations(List.of(10L));
    }

    @Test
    void rejectsManualTeamWithMoreThanFiveMembers() {
        when(teamUserRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, Grade.GRADE_2))
                .thenReturn(List.of(
                        student("stu2301", "학생1", Grade.GRADE_2),
                        student("stu2302", "학생2", Grade.GRADE_2),
                        student("stu2303", "학생3", Grade.GRADE_2),
                        student("stu2304", "학생4", Grade.GRADE_2),
                        student("stu2305", "학생5", Grade.GRADE_2),
                        student("stu2306", "학생6", Grade.GRADE_2)
                ));
        ManualTeamRecommendationRequestDto request = request(
                Grade.GRADE_2,
                List.of(team(1, List.of(
                        member("stu2301", StudentRole.FRONTEND, true),
                        member("stu2302", StudentRole.BACKEND, false),
                        member("stu2303", StudentRole.AI, false),
                        member("stu2304", StudentRole.DESIGN, false),
                        member("stu2305", StudentRole.APP, false),
                        member("stu2306", StudentRole.BACKEND, false)
                )))
        );

        assertThatThrownBy(() -> manualTeamRecommendationService.createManualRecommendations(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("한 팀은 최대 5명까지 배정할 수 있습니다: 1팀");
    }

    @Test
    void rejectsDuplicateStudentAssignment() {
        when(teamUserRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, Grade.GRADE_2))
                .thenReturn(List.of(student("stu2301", "장준민", Grade.GRADE_2)));
        ManualTeamRecommendationRequestDto request = request(
                Grade.GRADE_2,
                List.of(
                        team(1, List.of(member("stu2301", StudentRole.FRONTEND, true))),
                        team(2, List.of(member("stu2301", StudentRole.FRONTEND, false)))
                )
        );

        assertThatThrownBy(() -> manualTeamRecommendationService.createManualRecommendations(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("한 학생은 여러 팀에 중복 배정될 수 없습니다: stu2301");
    }

    @Test
    void allowsUnassignedStudentsAfterFrontendConfirmation() {
        when(teamUserRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, Grade.GRADE_2))
                .thenReturn(List.of(
                        student("stu2301", "장준민", Grade.GRADE_2),
                        student("stu2302", "위재성", Grade.GRADE_2)
                ));
        ManualTeamRecommendationRequestDto request = request(
                Grade.GRADE_2,
                List.of(team(1, List.of(member("stu2301", StudentRole.FRONTEND, true))))
        );

        manualTeamRecommendationService.createManualRecommendations(request);

        verify(recommendationPersistenceService).replacePendingManualRecommendations(
                eq(Grade.GRADE_2),
                any(),
                any()
        );
    }

    @Test
    void rejectsMoreThanOneLeaderInTeam() {
        when(teamUserRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, Grade.GRADE_2))
                .thenReturn(List.of(
                        student("stu2301", "장준민", Grade.GRADE_2),
                        student("stu2302", "위재성", Grade.GRADE_2)
                ));
        ManualTeamRecommendationRequestDto request = request(
                Grade.GRADE_2,
                List.of(team(1, List.of(
                        member("stu2301", StudentRole.FRONTEND, true),
                        member("stu2302", StudentRole.BACKEND, true)
                )))
        );

        assertThatThrownBy(() -> manualTeamRecommendationService.createManualRecommendations(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("한 팀에는 팀장을 1명만 지정할 수 있습니다: 1팀");
    }

    private ManualTeamRecommendationRequestDto request(
            Grade grade,
            List<ManualTeamRecommendationRequestDto.ManualTeamDto> teams
    ) {
        ManualTeamRecommendationRequestDto request = new ManualTeamRecommendationRequestDto();
        ReflectionTestUtils.setField(request, "grade", grade);
        ReflectionTestUtils.setField(request, "teams", teams);
        return request;
    }

    private ManualTeamRecommendationRequestDto.ManualTeamDto team(
            Integer teamNumber,
            List<ManualTeamRecommendationRequestDto.ManualTeamMemberDto> members
    ) {
        ManualTeamRecommendationRequestDto.ManualTeamDto team = new ManualTeamRecommendationRequestDto.ManualTeamDto();
        ReflectionTestUtils.setField(team, "teamNumber", teamNumber);
        ReflectionTestUtils.setField(team, "members", members);
        return team;
    }

    private ManualTeamRecommendationRequestDto.ManualTeamDto frontendTeam(
            String teamName,
            List<String> memberUserIds,
            String leaderUserId
    ) {
        ManualTeamRecommendationRequestDto.ManualTeamDto team = new ManualTeamRecommendationRequestDto.ManualTeamDto();
        ReflectionTestUtils.setField(team, "teamName", teamName);
        ReflectionTestUtils.setField(team, "memberUserIds", memberUserIds);
        ReflectionTestUtils.setField(team, "leaderUserId", leaderUserId);
        return team;
    }

    private ManualTeamRecommendationRequestDto.ManualTeamMemberDto member(
            String userId,
            StudentRole role,
            boolean leader
    ) {
        ManualTeamRecommendationRequestDto.ManualTeamMemberDto member = new ManualTeamRecommendationRequestDto.ManualTeamMemberDto();
        ReflectionTestUtils.setField(member, "userId", userId);
        ReflectionTestUtils.setField(member, "role", role);
        ReflectionTestUtils.setField(member, "leader", leader);
        return member;
    }

    private User student(String userId, String name, Grade grade) {
        return student(userId, name, grade, StudentRole.BACKEND);
    }

    private User student(String userId, String name, Grade grade, StudentRole studentRole) {
        User user = User.builder()
                .userId(userId)
                .name(name)
                .accountRole(AccountRole.STUDENT)
                .grade(grade)
                .build();
        user.updateProfile(studentRole, List.of(), List.of(), false, List.of());
        return user;
    }
}
