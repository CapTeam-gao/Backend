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

    @NotNull
    @Valid
    private List<PreferredTeammateDto> preferredTeammates;

    @Getter
    @NoArgsConstructor
    public static class PreferredTeammateDto {
        @NotBlank
        private String userId;

        @NotBlank
        private String name;
    }
}
