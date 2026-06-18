package com.capteam.gaobackend.service;

import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSurveyAnalysisService userSurveyAnalysisService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userSurveyAnalysisService);
    }

    @Test
    void mapsSpecializedSurveyRolesWithoutBackendFallback() {
        assertThat(mapRole("fullstack")).isEqualTo(StudentRole.FULLSTACK);
        assertThat(mapRole("devops")).isEqualTo(StudentRole.DEVOPS);
        assertThat(mapRole("security")).isEqualTo(StudentRole.SECURITY);
        assertThat(mapRole("game")).isEqualTo(StudentRole.GAME);
    }

    private StudentRole mapRole(String role) {
        return ReflectionTestUtils.invokeMethod(userService, "mapRole", role);
    }
}
