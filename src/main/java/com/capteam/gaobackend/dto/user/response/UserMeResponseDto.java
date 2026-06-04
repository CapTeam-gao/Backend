package com.capteam.gaobackend.dto.user.response;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class
UserMeResponseDto {

    // 내 프로필의 userId를 내려주는 필드입니다.
    private String userId;

    // 내 프로필의 이름을 내려주는 필드입니다.
    private String name;

    // 내 희망 개발 역할을 내려주는 필드입니다.
    private StudentRole studentRole;

    // 내 기술 스택 목록을 내려주는 필드입니다.
    private List<String> skill;

    // 내 구현 경험 목록을 내려주는 필드입니다.
    private List<String> experience;

    // 내 팀장 희망 여부를 내려주는 필드입니다.
    private boolean wantsLeader;

    // 내 선호 팀원 userId 목록을 내려주는 필드입니다.
    private List<String> preferredTeammates;

    // User 엔티티를 내 프로필 응답 DTO로 변환하는 기능입니다.
    public static UserMeResponseDto from(User user) {
        return UserMeResponseDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .studentRole(user.getStudentRole())
                .skill(user.getSkill())
                .experience(user.getExperience())
                .wantsLeader(user.isWantsLeader())
                .preferredTeammates(user.getPreferredTeammates())
                .build();
    }
}
