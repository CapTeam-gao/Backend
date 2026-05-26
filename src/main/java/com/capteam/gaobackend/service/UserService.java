package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.auth.response.HeaderUserResponseDto;
import com.capteam.gaobackend.dto.user.request.UserProfileUpdateRequestDto;
import com.capteam.gaobackend.dto.user.response.UserMeResponseDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.UserRepository;
//import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    // 마이페이지 조회
    @Transactional(readOnly = true)
    public UserMeResponseDto getMyProfile() {
        User user = getAuthenticatedUser();
        return UserMeResponseDto.from(user);
    }

    // 마이페이지 수정
    @Transactional
    public UserMeResponseDto updateMyProfile(UserProfileUpdateRequestDto dto) {
        User user = getAuthenticatedUser();

        user.updateProfile(
                dto.getStudentRole(),
                dto.getSkill(),
                dto.getExperience(),
                dto.getProfileImage(),
                dto.isWantsLeader(),
                dto.getPreferredTeammates()
        );

        return UserMeResponseDto.from(user);
    }

    private User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }


    //헤더바에 학번 이름을 보내주는 코드
    public HeaderUserResponseDto getHeaderUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(UserNotFoundException::new);

        return new HeaderUserResponseDto(
                user.getUserId(),
                user.getName()
        );
    }
}
