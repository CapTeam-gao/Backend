package com.capteam.gaobackend.dto.user.request;

import com.capteam.gaobackend.enums.StudentRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequestDto {

    // 마이페이지에서 수정할 희망 개발 역할을 받는 필드입니다.
    private StudentRole studentRole;

    // 마이페이지에서 수정할 기술 스택 목록을 받는 필드입니다.
    private List<String> skill;

    // 마이페이지에서 수정할 구현 경험 목록을 받는 필드입니다.
    private List<String> experience;

    // 마이페이지에서 수정할 팀장 희망 여부를 받는 필드입니다.
    private boolean wantsLeader;

    // 마이페이지에서 수정할 선호 팀원 userId 목록을 받는 필드입니다.
    private List<String> preferredTeammates;
}
