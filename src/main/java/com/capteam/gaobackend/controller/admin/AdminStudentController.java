package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.dto.user.response.StudentListResponseDto;
import com.capteam.gaobackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final UserService userService;


    @GetMapping
    public ResponseEntity<List<StudentListResponseDto>> getAllStudent() {
        return ResponseEntity.ok(userService.getAllStudents());
    }

}
