package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStudentServiceTest {

    @Mock private TeamUserRepository teamUserRepository;
    @Mock private UserAnalysisRepository userAnalysisRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamProjectRepository teamProjectRepository;

    private AdminStudentService adminStudentService;

    @BeforeEach
    void setUp() {
        adminStudentService = new AdminStudentService(
                teamUserRepository,
                userAnalysisRepository,
                userRepository,
                teamProjectRepository
        );
    }

    @Test
    void searchesManualTeamStudentsByNameOrUserIdOrRoleKeyword() {
        User student = student("stu2301", "장준민", Grade.GRADE_2, StudentRole.FRONTEND);
        when(userRepository.searchStudentsForManualTeam(
                eq(AccountRole.STUDENT),
                eq(Grade.GRADE_2),
                eq("프론트"),
                eq(StudentRole.FRONTEND),
                any(Pageable.class)
        )).thenReturn(List.of(student));

        var response = adminStudentService.searchStudentsForManualTeam(Grade.GRADE_2, " 프론트 ");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getUserId()).isEqualTo("stu2301");
        assertThat(response.get(0).getName()).isEqualTo("장준민");
        assertThat(response.get(0).getStudentRole()).isEqualTo(StudentRole.FRONTEND);
        assertThat(response.get(0).getStudentRoleLabel()).isEqualTo("프론트엔드");
    }

    @Test
    void limitsManualTeamStudentSearchToTenRows() {
        when(userRepository.searchStudentsForManualTeam(
                eq(AccountRole.STUDENT),
                eq(Grade.GRADE_2),
                eq("23"),
                eq(null),
                any(Pageable.class)
        )).thenReturn(List.of());

        adminStudentService.searchStudentsForManualTeam(Grade.GRADE_2, "23");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchStudentsForManualTeam(
                eq(AccountRole.STUDENT),
                eq(Grade.GRADE_2),
                eq("23"),
                eq(null),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void rejectsManualTeamSearchWithoutGrade() {
        assertThatThrownBy(() -> adminStudentService.searchStudentsForManualTeam(null, "김"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학년을 선택해주세요.");
    }

    @Test
    void returnsEmptyManualTeamSearchForBlankKeyword() {
        assertThat(adminStudentService.searchStudentsForManualTeam(Grade.GRADE_2, "  ")).isEmpty();
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
