package com.capteam.gaobackend.dto.auth.request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    // 로그인할 사용자 id를 받는 필드입니다. 예: stu2107
    @NotBlank
    private String userId;

    // 로그인 비밀번호를 받는 필드입니다. 최초 기본값은 1234입니다.
    @NotBlank
    private String password;
}
