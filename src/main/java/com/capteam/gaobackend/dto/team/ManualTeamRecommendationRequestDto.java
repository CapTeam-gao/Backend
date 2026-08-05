package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ManualTeamRecommendationRequestDto {

    // 직접 구성할 대상 학년입니다.
    private Grade grade;

    // 관리자가 화면에서 직접 구성한 팀 목록입니다.
    private List<ManualTeamDto> teams;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualTeamDto {

        // 화면에 표시되는 팀 번호입니다.
        private Integer teamNumber;

        // 프론트 직접 구성 화면에서 보내는 팀 이름입니다. 예: "1팀"
        private String teamName;

        // 해당 팀에 배정된 학생 목록입니다.
        private List<ManualTeamMemberDto> members;

        // 프론트 직접 구성 화면에서 보내는 팀원 userId 목록입니다.
        private List<String> memberUserIds;

        // 프론트 직접 구성 화면에서 보내는 팀장 userId입니다.
        private String leaderUserId;

        public ManualTeamDto(Integer teamNumber, List<ManualTeamMemberDto> members) {
            this.teamNumber = teamNumber;
            this.members = members;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualTeamMemberDto {

        // 팀에 배정할 학생 userId입니다.
        private String userId;

        // 관리자가 지정한 팀 내 역할입니다.
        private StudentRole role;

        // 관리자가 지정한 팀장 여부입니다.
        private boolean leader;
    }
}
