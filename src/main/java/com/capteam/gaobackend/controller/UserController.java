package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.user.response.HeaderUserResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.user.request.UserProfileUpdateRequestDto;
import com.capteam.gaobackend.dto.user.request.UserSurveyRequestDto;
import com.capteam.gaobackend.dto.user.response.StudentSearchResponseDto;
import com.capteam.gaobackend.dto.user.response.UserMeResponseDto;
import com.capteam.gaobackend.dto.user.response.UserSurveyResponseDto;
import com.capteam.gaobackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    // 로그인한 사용자의 마이페이지 프로필 정보를 조회하는 기능입니다.
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserMeResponseDto>> getMyProfile() {
        UserMeResponseDto dto = userService.getMyProfile();
        return ApiResponse.ok(dto);
    }

    // 로그인한 사용자의 마이페이지 프로필 정보를 수정하는 기능입니다.
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserMeResponseDto>> updateMyProfile(@RequestBody UserProfileUpdateRequestDto dto) {
        UserMeResponseDto result = userService.updateMyProfile(dto);
        return ApiResponse.ok(result);
    }

    // 로그인한 사용자의 설문 저장 결과를 조회하는 기능입니다.
    @GetMapping("/survey")
    public ResponseEntity<ApiResponse<UserSurveyResponseDto>> getMySurvey() {
        return ApiResponse.ok(userService.getMySurvey());
    }

    // 로그인한 사용자의 설문 응답과 성향 점수를 저장하는 기능입니다.
    @PostMapping("/survey")
    public ResponseEntity<ApiResponse<UserSurveyResponseDto>> submitMySurvey(@RequestBody UserSurveyRequestDto dto) {
        return ApiResponse.ok(userService.submitMySurvey(dto));
    }

    // 선호 팀원 선택 UI에서 사용할 같은 학년 학생 검색 기능입니다.
    @GetMapping("/students/search")
    public ResponseEntity<ApiResponse<List<StudentSearchResponseDto>>> searchStudents(@RequestParam String keyword) {
        return ApiResponse.ok(userService.searchStudents(keyword));
    }


    // 헤더 영역에 표시할 로그인 사용자 기본 정보를 조회하는 기능입니다.
    @GetMapping("/header")
    public ResponseEntity<HeaderUserResponseDto> getHeaderUser(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(userService.getHeaderUser(userId));
    }
}
