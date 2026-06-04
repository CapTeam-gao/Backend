package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.admin.AdminStudentDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentListPageResponseDto;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.service.admin.AdminStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentService adminStudentService;


    // 관리자가 학생 통계와 학생 목록을 조회하고 이름/학번/희망 직군으로 검색하는 기능입니다.
    @GetMapping
    public ResponseEntity<AdminStudentListPageResponseDto> getAllStudent(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) StudentRole studentRole,
            @RequestParam(required = false) Grade grade,
            @RequestParam(required = false) Boolean surveyCompleted
    ) {
        return ResponseEntity.ok(adminStudentService.getAllStudents(name, userId, studentRole, grade, surveyCompleted));
    }


    // 관리자가 특정 학생의 상세 정보와 AI 분석 결과를 조회하는 기능입니다.
    @GetMapping("/{userId}")
    public ResponseEntity<AdminStudentDetailResponseDto> getStudentInfo(@PathVariable String userId) {
        AdminStudentDetailResponseDto response = adminStudentService.getStudentDetail(userId);
        return ResponseEntity.ok(response);
    }

}
