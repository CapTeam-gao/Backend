package com.capteam.gaobackend.dto.team;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PreferredTeammateRequestDto {

    // 사용자가 선호 팀원으로 등록하려는 학생 userId 목록을 받는 필드입니다.
    @NotNull
    @Valid
    private List<String> preferredTeammates;
}
