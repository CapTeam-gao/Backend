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
import com.capteam.gaobackend.enums.UserAnalysisStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    // 회귀 테스트: 예전엔 analysisResult 텍스트 유무로만 완료를 판단해서, AI 분석이
    // 실패한 학생도 "분석 실패"가 아니라 계속 "분석 중"(PENDING)으로만 보였다.
    @Test
    void reportsFailedStatusWhenAnalysisMarkedFailed() {
        User user = surveyCompletedUser();
        UserAnalysis analysis = UserAnalysis.builder()
                .user(user)
                .status(UserAnalysisStatus.FAILED)
                .build();

        AdminStudentDetailResponseDto response = AdminStudentDetailResponseDto.from(
                user, null, analysis,
                new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0),
                new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0)
        );

        assertThat(response.getAnalysisStatus()).isEqualTo("FAILED");
    }

    @Test
    void reportsSuccessStatusWhenAnalysisSucceeded() {
        User user = surveyCompletedUser();
        UserAnalysis analysis = UserAnalysis.builder()
                .user(user)
                .analysisResult("실행력이 뛰어난 프론트엔드 성향입니다.")
                .studentLevel(StudentLevel.UPPER)
                .status(UserAnalysisStatus.SUCCEEDED)
                .build();

        AdminStudentDetailResponseDto response = AdminStudentDetailResponseDto.from(
                user, null, analysis,
                new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0),
                new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0)
        );

        assertThat(response.getAnalysisStatus()).isEqualTo("SUCCESS");
    }

    // status 필드가 생기기 전에 분석이 끝난 과거 row(레거시 데이터)도 여전히
    // "분석 완료"로 보여야 한다.
    @Test
    void reportsSuccessStatusForLegacyAnalysisWithoutStatusField() {
        User user = surveyCompletedUser();
        UserAnalysis analysis = UserAnalysis.builder()
                .user(user)
                .analysisResult("문제 해결력이 강한 백엔드 성향입니다.")
                .studentLevel(StudentLevel.UPPER)
                .build();

        AdminStudentDetailResponseDto response = AdminStudentDetailResponseDto.from(
                user, null, analysis,
                new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0),
                new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0)
        );

        assertThat(response.getAnalysisStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void reportsPendingStatusWhenAnalysisNotStartedYet() {
        User user = surveyCompletedUser();

        AdminStudentDetailResponseDto response = AdminStudentDetailResponseDto.from(
                user, null, null,
                new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0),
                new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0)
        );

        assertThat(response.getAnalysisStatus()).isEqualTo("PENDING");
    }

    private User surveyCompletedUser() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        ReflectionTestUtils.setField(user, "surveyCompleted", true);
        return user;
    }
}
