package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.user.response.StudentSearchResponseDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSurveyAnalysisService userSurveyAnalysisService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userSurveyAnalysisService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsSpecializedSurveyRolesWithoutBackendFallback() {
        assertThat(mapRole("fullstack")).isEqualTo(StudentRole.FULLSTACK);
        assertThat(mapRole("devops")).isEqualTo(StudentRole.DEVOPS);
        assertThat(mapRole("security")).isEqualTo(StudentRole.SECURITY);
        assertThat(mapRole("game")).isEqualTo(StudentRole.GAME);
    }

    @Test
    void rejectsMoreThanThreePreferredTeammates() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);

        assertThatThrownBy(() -> resolvePreferredTeammates(user, List.of("stu1001", "stu1002", "stu1003", "stu1004")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선호 팀원은 최대 3명까지 등록할 수 있습니다.");
    }

    @Test
    void rejectsDuplicatePreferredTeammate() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);

        assertThatThrownBy(() -> resolvePreferredTeammates(user, List.of("stu1001", "stu1001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 학생을 선호 팀원으로 중복 등록할 수 없습니다.");
    }

    @Test
    void resolvesValidPreferredTeammates() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);
        User preferredUser = student("stu1001", "김민수", Grade.GRADE_3);
        when(userRepository.findById("stu1001")).thenReturn(Optional.of(preferredUser));

        assertThat(resolvePreferredTeammates(user, List.of("stu1001"))).containsExactly("stu1001");
    }

    @Test
    void rejectsLegacyPreferredTeammateTextInput() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);

        assertThatThrownBy(() -> resolvePreferredTeammates(user, List.of("1001 김민수")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선호 팀원을 찾을 수 없습니다: 1001 김민수");
    }

    @Test
    void rejectsPreferredSelf() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);

        assertThatThrownBy(() -> resolvePreferredTeammates(user, List.of("stu1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인은 선호 팀원으로 등록할 수 없습니다.");
    }

    @Test
    void rejectsPreferredAdminAccount() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);
        User admin = User.builder()
                .userId("admin01")
                .name("관리자")
                .accountRole(AccountRole.ADMIN)
                .build();
        when(userRepository.findById("admin01")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> resolvePreferredTeammates(user, List.of("admin01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학생 계정만 선호 팀원으로 등록할 수 있습니다.");
    }

    @Test
    void rejectsDifferentGradePreferredStudent() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);
        User preferredUser = student("stu2001", "이민수", Grade.GRADE_2);
        when(userRepository.findById("stu2001")).thenReturn(Optional.of(preferredUser));

        assertThatThrownBy(() -> resolvePreferredTeammates(user, List.of("stu2001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 학년 학생만 선호 팀원으로 등록할 수 있습니다.");
    }

    @Test
    void searchesSameGradeStudentsAndExcludesSelf() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);
        authenticate(user.getUserId());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(userRepository.searchStudentsByKeyword(
                eq(AccountRole.STUDENT),
                eq(Grade.GRADE_3),
                eq("stu"),
                any(Pageable.class)
        )).thenReturn(List.of(
                user,
                student("stu1001", "김민수", Grade.GRADE_3),
                student("stu1002", "이민수", Grade.GRADE_3),
                student("stu1003", "박민수", Grade.GRADE_3),
                student("stu1004", "최민수", Grade.GRADE_3),
                student("stu1005", "정민수", Grade.GRADE_3),
                student("stu1006", "강민수", Grade.GRADE_3)
        ));

        List<StudentSearchResponseDto> result = userService.searchStudents(" stu ");

        assertThat(result)
                .extracting(StudentSearchResponseDto::getUserId)
                .containsExactly("stu1001", "stu1002", "stu1003", "stu1004", "stu1005", "stu1006");
    }

    @Test
    void searchesStudentsByNameKeyword() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);
        User preferredUser = student("stu1001", "김민수", Grade.GRADE_3);
        authenticate(user.getUserId());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(userRepository.searchStudentsByKeyword(
                eq(AccountRole.STUDENT),
                eq(Grade.GRADE_3),
                eq("민수"),
                any(Pageable.class)
        )).thenReturn(List.of(preferredUser));

        List<StudentSearchResponseDto> result = userService.searchStudents(" 민수 ");

        assertThat(result)
                .extracting(StudentSearchResponseDto::getUserId, StudentSearchResponseDto::getName)
                .containsExactly(tuple("stu1001", "김민수"));
    }

    @Test
    void returnsEmptySearchResultForBlankKeyword() {
        User user = student("stu1000", "나학생", Grade.GRADE_3);
        authenticate(user.getUserId());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

        assertThat(userService.searchStudents("  ")).isEmpty();
    }

    private StudentRole mapRole(String role) {
        return ReflectionTestUtils.invokeMethod(userService, "mapRole", role);
    }

    @SuppressWarnings("unchecked")
    private List<String> resolvePreferredTeammates(User user, List<String> preferredTeammates) {
        return ReflectionTestUtils.invokeMethod(userService, "resolvePreferredTeammates", user, preferredTeammates);
    }

    private User student(String userId, String name, Grade grade) {
        return User.builder()
                .userId(userId)
                .name(name)
                .accountRole(AccountRole.STUDENT)
                .grade(grade)
                .build();
    }

    private void authenticate(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())
        );
    }
}
