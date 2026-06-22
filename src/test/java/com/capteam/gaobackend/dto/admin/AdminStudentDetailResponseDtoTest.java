package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.entity.UserDevelopmentScore;
import com.capteam.gaobackend.entity.UserPersonalityScore;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.ResponseReliability;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.enums.StudentRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminStudentDetailResponseDtoTest {

    @Test
    void hidesLegacySkillLevelStoredAsAnalysisResult() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        UserAnalysis analysis = UserAnalysis.builder()
                .user(user)
                .analysisResult("중")
                .studentLevel(StudentLevel.MIDDLE)
                .responseReliability(ResponseReliability.MEDIUM)
                .inconsistentAnswers(2)
                .build();

        AdminStudentDetailResponseDto response = AdminStudentDetailResponseDto.from(
                user,
                null,
                analysis,
                new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0),
                new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0)
        );

        assertThat(response.getAnalysisResult()).isNull();
        assertThat(response.getStudentLevel()).isEqualTo(StudentLevel.MIDDLE);
        assertThat(response.getResponseReliability()).isEqualTo(ResponseReliability.MEDIUM);
        assertThat(response.getInconsistentAnswers()).isEqualTo(2);
    }

    @Test
    void includesProjectTeamNameWhenProvided() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        Team team = Team.builder()
                .teamName("1팀")
                .build();
        TeamUser teamUser = TeamUser.builder()
                .team(team)
                .user(user)
                .studentRole(StudentRole.BACKEND)
                .leaderRole(LeaderRole.MEMBER)
                .build();

        AdminStudentDetailResponseDto response = AdminStudentDetailResponseDto.from(
                user,
                teamUser,
                null,
                new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0),
                new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0),
                "가오팀"
        );

        assertThat(response.getTeamName()).isEqualTo("1팀");
        assertThat(response.getProjectTeamName()).isEqualTo("가오팀");
    }
}
