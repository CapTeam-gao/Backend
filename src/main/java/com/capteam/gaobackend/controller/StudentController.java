package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.user.response.StudentSearchResponseDto;
import com.capteam.gaobackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final UserService userService;

    // 설문 선호 팀원 선택 UI에서 사용할 같은 학년 학생 검색 기능입니다.
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StudentSearchResponseDto>>> searchStudents(@RequestParam String keyword) {
        return ApiResponse.ok(userService.searchStudents(keyword));
    }
}
