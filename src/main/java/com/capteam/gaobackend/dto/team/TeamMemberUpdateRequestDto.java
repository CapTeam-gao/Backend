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

    @NotBlank
    private String userId;

    @NotNull
    private Long targetTeamId;

    @NotNull
    private StudentRole studentRole;

    @NotNull
    private LeaderRole leaderRole;
}
