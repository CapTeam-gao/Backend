package com.capteam.gaobackend.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequestDto {
    // 현재 비밀번호 검증을 위해 프론트에서 보내는 필드입니다.
    @NotBlank
    private String password;

    // 변경할 새 비밀번호를 받는 필드입니다.
    @NotBlank
    private String newPassword;

    // 새 비밀번호 확인값을 받아 오타를 검증하는 필드입니다.
    @NotBlank
    private String checkPassword;
}
