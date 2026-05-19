package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.user.request.UserProfileUpdateRequestDto;
import com.capteam.gaobackend.dto.user.response.UserMeResponseDto;
import com.capteam.gaobackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    // GET /api/user/me/profile - 내 프로필 조회
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserMeResponseDto>> getMyProfile() {
        UserMeResponseDto dto = userService.getMyProfile();
        return ApiResponse.ok(dto);
    }

    // PUT /api/user/me/profile - 내 프로필 수정
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserMeResponseDto>> updateMyProfile(@RequestBody UserProfileUpdateRequestDto dto) {
        UserMeResponseDto result = userService.updateMyProfile(dto);
        return ApiResponse.ok(result);
    }
}
