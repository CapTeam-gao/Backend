package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.notification.request.FcmTokenRegisterRequestDto;
import com.capteam.gaobackend.dto.notification.response.FcmTokenResponseDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserFcmToken;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.repository.UserFcmTokenRepository;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserFcmTokenRepository userFcmTokenRepository;

    private FcmTokenService fcmTokenService;

    @BeforeEach
    void setUp() {
        fcmTokenService = new FcmTokenService(userRepository, userFcmTokenRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerTokenCreatesNewActiveToken() {
        User user = student("stu2301", "장준민", Grade.GRADE_2);
        authenticate(user.getUserId());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(userFcmTokenRepository.findByToken("token-value")).thenReturn(Optional.empty());
        when(userFcmTokenRepository.save(any(UserFcmToken.class))).thenAnswer(invocation -> {
            UserFcmToken token = invocation.getArgument(0);
            ReflectionTestUtils.setField(token, "id", 1L);
            return token;
        });

        FcmTokenResponseDto response = fcmTokenService.registerToken(request(" token-value "));

        assertThat(response.getTokenId()).isEqualTo(1L);
        assertThat(response.getTokenPreview()).contains("token-");
    }

    @Test
    void registerTokenReassignsExistingTokenToCurrentUser() {
        User user = student("stu2301", "장준민", Grade.GRADE_2);
        User previousUser = student("stu2302", "위재성", Grade.GRADE_2);
        UserFcmToken token = UserFcmToken.builder()
                .user(previousUser)
                .token("token-value")
                .lastRegisteredAt(LocalDateTime.now().minusDays(1))
                .build();

        authenticate(user.getUserId());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(userFcmTokenRepository.findByToken("token-value")).thenReturn(Optional.of(token));

        FcmTokenResponseDto response = fcmTokenService.registerToken(request("token-value"));

        assertThat(response.getTokenId()).isNull();
        assertThat(token.getUser()).isSameAs(user);
    }

    @Test
    void deleteMyTokensRemovesAllTokensForAuthenticatedUser() {
        User user = student("stu2301", "장준민", Grade.GRADE_2);
        authenticate(user.getUserId());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

        fcmTokenService.deleteMyTokens();

        verify(userFcmTokenRepository).deleteAllByUserUserId("stu2301");
    }

    private FcmTokenRegisterRequestDto request(String token) {
        FcmTokenRegisterRequestDto request = new FcmTokenRegisterRequestDto();
        ReflectionTestUtils.setField(request, "fcmToken", token);
        return request;
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
