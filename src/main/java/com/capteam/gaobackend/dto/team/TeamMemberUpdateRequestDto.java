package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamMemberUpdateRequestDto {

    // 이동하거나 수정할 학생 userId를 받는 필드입니다.
    @NotBlank
    private String userId;

    // 학생을 이동시킬 대상 팀 id를 받는 필드입니다.
    @NotNull
    private Long targetTeamId;

    // 이동 후 학생의 팀 내 개발 역할을 받는 필드입니다.
    @NotNull
    private StudentRole studentRole;

    // 이동 후 학생의 팀장/팀원 역할을 받는 필드입니다.
    @NotNull
    private LeaderRole leaderRole;
}
