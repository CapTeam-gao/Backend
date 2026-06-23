package com.capteam.gaobackend.dto.notice;

import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamResultResponseDto {

    private Grade grade;

    private List<TeamResultTeamDto> teams;

    @Getter
    @Builder
    public static class TeamResultTeamDto {

        private Long teamId;

        private String teamName;

        private String leaderName;

        private String roleSummary;

        private List<TeamResultMemberDto> members;
    }

    @Getter
    @Builder
    public static class TeamResultMemberDto {

        private String userId;

        private String name;

        private StudentRole studentRole;

        private boolean leader;
    }
}
