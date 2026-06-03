package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PreferredTeammateResponseDto {

    private List<TeammateDto> preferredTeammates;

    @Getter
    @Builder
    public static class TeammateDto {
        private String userId;
        private String name;

        public static TeammateDto from(User user) {
            return TeammateDto.builder()
                    .userId(user.getUserId())
                    .name(user.getName())
                    .build();
        }
    }
}
