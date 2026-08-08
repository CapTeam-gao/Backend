package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.team.PreferredTeammateRequestDto;
import com.capteam.gaobackend.dto.team.PreferredTeammateResponseDto;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private TeamProjectRepository teamProjectRepository;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(
                userRepository,
                teamRepository,
                teamUserRepository,
                teamProjectRepository
        );
    }

    @Test
    void getMyTeamProjectReturnsNullWhenUserHasNoTeam() {
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.empty());

        var response = teamService.getMyTeamProject("stu2301");

        assertThat(response).isNull();
    }

    @Test
    void getMyTeamProjectReturnsProjectWhenUserHasTeamProject() {
        Team team = Team.builder()
                .teamName("1팀")
                .build();
        ReflectionTestUtils.setField(team, "id", 1L);

        User user = User.builder()
                .userId("stu2301")
                .name("장준민")
                .build();

        TeamUser teamUser = TeamUser.builder()
                .team(team)
                .user(user)
                .studentRole(StudentRole.BACKEND)
                .leaderRole(LeaderRole.MEMBER)
                .build();

        TeamProject teamProject = TeamProject.builder()
                .team(team)
                .teamName("가오팀")
                .serviceName("팀 빌딩")
                .serviceIntro("학생 팀 매칭")
                .mainFeatures("AI 팀 추천")
                .build();
        ReflectionTestUtils.setField(teamProject, "id", 10L);

        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));
        when(teamProjectRepository.findByTeamId(1L)).thenReturn(Optional.of(teamProject));

        var response = teamService.getMyTeamProject("stu2301");

        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getTeamName()).isEqualTo("가오팀");
        assertThat(response.getServiceName()).isEqualTo("팀 빌딩");
    }

    @Test
    void updatePreferencesStoresPreferredStudentIdsOnly() {
        User user = student("stu2301", "장준민", Grade.GRADE_2);
        User preferredUser = student("stu2302", "위재성", Grade.GRADE_2);
        PreferredTeammateRequestDto request = request(List.of(" stu2302 "));

        when(userRepository.findByUserId("stu2301")).thenReturn(Optional.of(user));
        when(userRepository.findByUserId("stu2302")).thenReturn(Optional.of(preferredUser));

        var response = teamService.updatePreferences("stu2301", request);

        assertThat(user.getPreferredTeammates()).containsExactly("stu2302");
        assertThat(response.getPreferredTeammates())
                .extracting(PreferredTeammateResponseDto.TeammateDto::getUserId)
                .containsExactly("stu2302");
    }

    @Test
    void updatePreferencesRejectsDuplicateStudentIds() {
        User user = student("stu2301", "장준민", Grade.GRADE_2);
        User preferredUser = student("stu2302", "위재성", Grade.GRADE_2);
        PreferredTeammateRequestDto request = request(List.of("stu2302", "stu2302"));

        when(userRepository.findByUserId("stu2301")).thenReturn(Optional.of(user));
        when(userRepository.findByUserId("stu2302")).thenReturn(Optional.of(preferredUser));

        assertThatThrownBy(() -> teamService.updatePreferences("stu2301", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 학생을 선호 팀원으로 중복 등록할 수 없습니다.");
    }

    @Test
    void updatePreferencesRejectsDifferentGradeStudent() {
        User user = student("stu2301", "장준민", Grade.GRADE_2);
        User preferredUser = student("stu3301", "김민수", Grade.GRADE_3);
        PreferredTeammateRequestDto request = request(List.of("stu3301"));

        when(userRepository.findByUserId("stu2301")).thenReturn(Optional.of(user));
        when(userRepository.findByUserId("stu3301")).thenReturn(Optional.of(preferredUser));

        assertThatThrownBy(() -> teamService.updatePreferences("stu2301", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 학년 학생만 선호 팀원으로 등록할 수 있습니다.");
    }

    private User student(String userId, String name, Grade grade) {
        return User.builder()
                .userId(userId)
                .name(name)
                .accountRole(AccountRole.STUDENT)
                .grade(grade)
                .build();
    }

    private PreferredTeammateRequestDto request(List<String> preferredTeammates) {
        PreferredTeammateRequestDto request = new PreferredTeammateRequestDto();
        ReflectionTestUtils.setField(request, "preferredTeammates", preferredTeammates);
        return request;
    }
}
