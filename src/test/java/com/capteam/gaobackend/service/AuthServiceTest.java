package com.capteam.gaobackend.service;

import com.capteam.gaobackend.config.JwtTokenProvider;
import com.capteam.gaobackend.dto.auth.response.AuthResponse;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void reissuesTokensFromValidRefreshToken() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        when(jwtTokenProvider.extractUserId("valid-refresh-token")).thenReturn("stu2301");
        when(jwtTokenProvider.validateRefreshToken("stu2301", "valid-refresh-token")).thenReturn(true);
        when(userRepository.findById("stu2301")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refreshToken("valid-refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void rejectsMissingRefreshToken() {
        assertThatThrownBy(() -> authService.refreshToken((String) null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("리프레시 토큰이 필요합니다.");
    }
}
