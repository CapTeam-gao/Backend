package com.capteam.gaobackend.dto.user.response;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserMeResponseDto {

    private String userId;
    private String name;
    private StudentRole studentRole;
    private List<String> skill;
    private List<String> experience;
    private String profileImage;
    private boolean wantsLeader;
    private List<String> preferredTeammates;

    public static UserMeResponseDto from(User user) {
        return UserMeResponseDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .studentRole(user.getStudentRole())
                .skill(user.getSkill())
                .experience(user.getExperience())
                .profileImage(user.getProfileImage())
                .wantsLeader(user.isWantsLeader())
                .preferredTeammates(user.getPreferredTeammates())
                .build();
    }
}
