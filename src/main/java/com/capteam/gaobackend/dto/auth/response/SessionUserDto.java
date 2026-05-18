package com.capteam.gaobackend.dto.auth.response;

import com.capteam.gaobackend.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder
public class SessionUserDto {

    private Long id;
    private String studentId;
    private String name;

    public static SessionUserDto from(User user) {
        return SessionUserDto.builder()
                .id(user.getId())
                .studentId(user.getStudentId())
                .name(user.getName())
                .build();
    }


}
