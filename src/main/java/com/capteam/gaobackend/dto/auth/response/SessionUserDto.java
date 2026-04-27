package com.capteam.gaobackend.dto.auth.response;

import com.capteam.gaobackend.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder
public class SessionUserDto {

    private Long id;
    private String stuId;
    private String name;

    public static SessionUserDto from(User user) {
        return SessionUserDto.builder()
                .id(user.getId())
                .stuId(user.getStuId())
                .name(user.getName())
                .build();
    }


}
