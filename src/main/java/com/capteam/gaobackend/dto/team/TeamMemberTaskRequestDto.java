package com.capteam.gaobackend.dto.team;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamMemberTaskRequestDto {

    // 팀원이 직접 적는 담당 업무입니다. 빈 문자열을 보내면 담당 업무를 지웁니다.
    private String assignedTask;
}
