package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.config.JwtTokenProvider;
import com.capteam.gaobackend.dto.auth.request.LoginRequestDto;
import com.capteam.gaobackend.dto.auth.response.AuthResponse;
import com.capteam.gaobackend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService, jwtTokenProvider);
        ReflectionTestUtils.setField(authController, "refreshCookieSecure", false);
        ReflectionTestUtils.setField(authController, "refreshCookieSameSite", "Lax");
        lenient().when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
    }

    @Test
    void loginSetsRefreshTokenAsHttpOnlyCookie() {
        LoginRequestDto request = new LoginRequestDto();
        AuthResponse authResponse = authResponse();
        when(authService.doLogin(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.doLogin(request);

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .contains("refreshToken=refresh-token")
                .contains("HttpOnly")
                .contains("Path=/api/auth")
                .contains("SameSite=Lax");
    }

    @Test
    void reissueReadsCookieAndRotatesRefreshTokenCookie() {
        AuthResponse authResponse = authResponse();
        when(authService.refreshToken("old-refresh-token")).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.reissue("old-refresh-token");

        verify(authService).refreshToken("old-refresh-token");
        assertThat(response.getBody().accessToken()).isEqualTo("new-access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refreshToken=refresh-token")
                .contains("Max-Age=604800")
                .contains("Path=/api/auth")
                .contains("SameSite=Lax")
                .contains("HttpOnly");
    }

    @Test
    void logoutDeletesRefreshTokenCookieAndStoredToken() {
        ResponseEntity<?> response = authController.logout("refresh-token");

        verify(authService).logout("refresh-token");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refreshToken=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("Path=/api/auth")
                .contains("SameSite=Lax");
    }

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("refresh-token")
                .role("STUDENT")
                .surveyCompleted(true)
                .build();
    }
}
