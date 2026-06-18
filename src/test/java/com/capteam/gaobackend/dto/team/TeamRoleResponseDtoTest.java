package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.TeamStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeamRoleResponseDtoTest {

    @Test
    void exposesSpecializedRolesInRoleCountAndMembers() {
        Team team = Team.builder()
                .teamName("1팀")
                .grade(Grade.GRADE_2)
                .status(TeamStatus.APPROVED)
                .build();
        TeamUser fullstackMember = teamUser(team, "stu2301", "김풀스택", StudentRole.FULLSTACK);
        TeamUser securityMember = teamUser(team, "stu2302", "이보안", StudentRole.SECURITY);

        TeamDetailResponseDto response = TeamDetailResponseDto.from(
                team,
                null,
                List.of(fullstackMember, securityMember)
        );

        assertThat(response.getRoleCount()).containsEntry(StudentRole.FULLSTACK, 1L);
        assertThat(response.getRoleCount()).containsEntry(StudentRole.SECURITY, 1L);
        assertThat(response.getMembers())
                .extracting(TeamDetailResponseDto.TeamMemberDto::getStudentRole)
                .containsExactly(StudentRole.FULLSTACK, StudentRole.SECURITY);
    }

    private TeamUser teamUser(Team team, String userId, String name, StudentRole studentRole) {
        return TeamUser.builder()
                .team(team)
                .user(User.builder()
                        .userId(userId)
                        .name(name)
                        .accountRole(AccountRole.STUDENT)
                        .build())
                .studentRole(studentRole)
                .leaderRole(LeaderRole.MEMBER)
                .build();
    }
}
