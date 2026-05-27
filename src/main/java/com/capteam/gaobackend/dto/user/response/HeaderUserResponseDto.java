package com.capteam.gaobackend.dto.user.response;


import com.capteam.gaobackend.enums.AccountRole;
import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class HeaderUserResponseDto {
    private String userId;
    private String name;
    private AccountRole accountRole;
}
