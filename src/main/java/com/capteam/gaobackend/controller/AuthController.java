package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.config.ApiResponse;
import com.capteam.gaobackend.dto.auth.request.ChangePasswordRequestDto;
import com.capteam.gaobackend.dto.auth.request.LoginRequestDto;
import com.capteam.gaobackend.dto.auth.response.SessionUserDto;
import com.capteam.gaobackend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<SessionUserDto> doLogin(@RequestBody LoginRequestDto dto, HttpSession session) {
        SessionUserDto user = authService.doLogin(dto);

        session.setAttribute("LOGIN_USER", user.getId());

        return ResponseEntity.ok(user);
    }


    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequestDto dto) {
        authService.changePassword(dto);
        return ApiResponse.ok("비밀번호가 성공적으로 변경되었습니다.");
    }



//    // 유저가 로그인했는지 안했는지
//    @GetMapping("/status")
//    public ResponseEntity<?> getAuthStatus(@AuthenticationPrincipal UserDetails userDetails) {
//        //@AuthenticationPrincipal 이미 로그인한 사용자의 정보를 꺼내서 볼 수 있음
//        if (userDetails != null) {
//            // 로그인 상태
//            return ResponseEntity.ok(Map.of(
//                    "isLoggedIn", true, //로그인 상태가 true -> 로그인 한거임
//                    "username", userDetails.getUsername()       //UserDetails 사용자 정보 가져오기 마음대로
//            ));
//        }
//        // 로그인 아닐 때
//        return ResponseEntity.ok(Map.of("isLoggedIn", false));
//        //응답을 원래는 디티오로 해야하는데 귀찮아서 만든게 이 함수 키,밸류 형식으로 보냄 null 금지, 개수 10개, 많으면 디티오 쓰는게 좋음
//    }



    @GetMapping("/me")
    public ResponseEntity<?> getAuthStatus(@AuthenticationPrincipal UserDetails userDetails) {
        if(userDetails != null) {
            return ResponseEntity.ok(Map.of(
                    "isLoggedIn", true,
                    "username", userDetails.getUsername()
            ));
        }

        return ResponseEntity.ok(Map.of("isLoggedIn",false));
    }
}
