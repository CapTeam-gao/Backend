package com.capteam.gaobackend.dto.team;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PreferredTeammateRequestDto {

    // 사용자가 선호 팀원으로 등록하려는 학생 목록을 받는 필드입니다.
    @NotNull
    @Valid
    private List<PreferredTeammateDto> preferredTeammates;

    @Getter
    @NoArgsConstructor
    public static class PreferredTeammateDto {
        // 선호 팀원으로 등록할 학생 userId를 받는 필드입니다.
        @NotBlank
        private String userId;

        // userId와 실제 이름이 일치하는지 검증하기 위해 받는 필드입니다.
        @NotBlank
        private String name;
    }
}
