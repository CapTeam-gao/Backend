package com.capteam.gaobackend.controller;


import com.capteam.gaobackend.dto.auth.request.LoginRequestDto;
import com.capteam.gaobackend.dto.auth.response.MessageResponse;
import com.capteam.gaobackend.dto.auth.response.SessionUserDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;


    @PostMapping("/login")
    @Operation(summary = "로그인")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "로그인 성공"),
            @ApiResponse(responseCode = "404",description = "존재하지 않는유저")
    })
    public ResponseEntity<SessionUserDto> doLogin(@RequestBody LoginRequestDto dto, HttpSession session) {
        SessionUserDto user = authService.doLogin(dto);

        session.setAttribute("LOGIN_USER", user.getId());

        return ResponseEntity.ok(user);
    }
}
