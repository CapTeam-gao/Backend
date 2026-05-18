package com.capteam.gaobackend.dto.user;

import com.capteam.gaobackend.enums.StudentRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequestDto {

    private StudentRole studentRole;           // 희망 역할
    private List<String> skill;               // 기술스택
    private List<String> experience;          // 경험
    private String profileImage;              // 프로필 이미지
    private boolean wantsLeader;              // 팀장 희망 여부
    private List<String> preferredTeammates;  // 선호 팀원 (최대 3명)

}
