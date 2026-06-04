package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PreferredTeammateResponseDto {

    // 사용자가 현재 등록한 선호 팀원 목록을 내려주는 필드입니다.
    private List<TeammateDto> preferredTeammates;

    @Getter
    @Builder
    public static class TeammateDto {
        // 선호 팀원 userId를 내려주는 필드입니다.
        private String userId;

        // 선호 팀원 이름을 내려주는 필드입니다.
        private String name;

        // User 엔티티를 선호 팀원 응답 DTO로 변환하는 기능입니다.
        public static TeammateDto from(User user) {
            return TeammateDto.builder()
                    .userId(user.getUserId())
                    .name(user.getName())
                    .build();
        }
    }
}
