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
    @NotBlank
    private String password;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String checkPassword;
}
