package com.capteam.gaobackend.service;

import com.capteam.gaobackend.config.JwtTokenProvider;
import com.capteam.gaobackend.dto.auth.request.ChangePasswordRequestDto;
import com.capteam.gaobackend.dto.auth.request.LoginRequestDto;
import com.capteam.gaobackend.dto.auth.request.RefreshRequest;
import com.capteam.gaobackend.dto.auth.response.AuthResponse;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse doLogin(LoginRequestDto dto) {
        User user = userRepository.findByUserId(dto.getUserId()).orElseThrow(UserNotFoundException::new);

        if (!user.isPasswordEncoded()) {
            // 최초 로그인 - 평문 비번 비교 후 암호화 저장
            if (!user.getPassword().equals(dto.getPassword()))
                throw new RuntimeException("비밀번호가 틀렸습니다.");

            user.updatePassword(passwordEncoder.encode(dto.getPassword()));
            userRepository.save(user);
        } else {
            // 이후 로그인 - 암호화된 비번 비교
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword()))
                throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }


        var accessToken = jwtTokenProvider.createAccessToken(user);
        var refreshToken = jwtTokenProvider.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getAccountRole().name())
                .build();
    }

    public void changePassword(ChangePasswordRequestDto dto) {
        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword()))
            throw new BadCredentialsException("현재 비밀번호가 일치하지 않습니다.");

        if (!dto.getNewPassword().equals(dto.getCheckPassword()))
            throw new BadCredentialsException("새 비밀번호가 일치하지 않습니다.");

        user.updatePassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    private User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }


    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshRequest dto) {
        var refreshToken = dto.refreshToken();

        var userId = jwtTokenProvider.extractUserId(refreshToken);

        if(!jwtTokenProvider.validateRefreshToken(userId,refreshToken)) {
            throw new BadCredentialsException("유효하지 않은 리프레시 토큰입니다.");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("유효하지 않은 사용자 입니다."));

        var newAccessToken = jwtTokenProvider.createAccessToken(user);
        var newRefreshToken = jwtTokenProvider.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
