package com.capteam.gaobackend.dto.user.request;

import com.capteam.gaobackend.enums.StudentRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequestDto {

    private StudentRole studentRole;
    private List<String> skill;
    private List<String> experience;
    private String profileImage;
    private boolean wantsLeader;
    private List<String> preferredTeammates;
}
