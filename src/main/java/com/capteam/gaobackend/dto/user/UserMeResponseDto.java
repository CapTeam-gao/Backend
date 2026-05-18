package com.capteam.gaobackend.dto.user;

import com.capteam.gaobackend.enums.StudentRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Getter
@NoArgsConstructor
public class UserMeResponseDto {
    private long userId;
    private String name;
    private StudentRole studentRole;

}
