package com.capteam.gaobackend.dto.auth.response;

import com.capteam.gaobackend.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder
public class SessionUserDto {

    private String userId;
    private String name;

    public static SessionUserDto from(User user) {
        return SessionUserDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .build();
    }
}
