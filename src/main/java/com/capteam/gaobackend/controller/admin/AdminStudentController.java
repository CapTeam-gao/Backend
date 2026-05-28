package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.admin.AdminStudentDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentListResponseDto;
import com.capteam.gaobackend.service.admin.AdminStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentService adminStudentService;


    @GetMapping
    public ResponseEntity<List<AdminStudentListResponseDto>> getAllStudent() {
        return ResponseEntity.ok(adminStudentService.getAllStudents());
    }


    @GetMapping("/{userId}")
    public ResponseEntity<AdminStudentDetailResponseDto> getStudentInfo(@PathVariable String userId) {
        AdminStudentDetailResponseDto response = adminStudentService.getStudentDetail(userId);
        return ResponseEntity.ok(response);
    }

}
